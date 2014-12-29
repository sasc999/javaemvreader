/*
 * Copyright 2010 sasc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sasc.terminal.smartcardio;

import javax.smartcardio.*;
import sasc.terminal.CardConnection;
import sasc.terminal.CardResponse;
import sasc.terminal.Terminal;
import sasc.terminal.TerminalException;
import sasc.util.Log;
import sasc.util.Util;

/**
 * Any handling of procedure bytes and GET REPONSE/GET DATA in javax.smartcardio
 * should be disabled, because of improper handling of the CLS byte in some
 * cases.
 *
 * Thus, the "procedure byte handling" of the the TAL (Terminal Abstraction
 * Layer), is moved to a higher layer.
 *
 * @author sasc
 */
public class SmartcardioCardConnection implements CardConnection, Terminal {

    private Card card;
    private CardTerminal smartCardIOTerminal;
    private CardChannel channel;

    public SmartcardioCardConnection(Card card, CardTerminal smartCardIOTerminal) {
        this.card = card;
        this.smartCardIOTerminal = smartCardIOTerminal;
        channel = card.getBasicChannel();
    }

    @Override
    public CardResponse transmit(byte[] cmd) throws TerminalException {
        if (cmd == null) {
            throw new IllegalArgumentException("Argument 'cmd' cannot be null");
        }

        if (cmd.length < 4) {
            throw new IllegalArgumentException("APDU must be at least 4 bytes long: " + cmd.length);
        }

        Log.debug("cmd bytes: " + Util.prettyPrintHexNoWrap(cmd));

        /*
         * case 1 : |CLA|INS|P1 |P2 |                    len = 4 
         * case 2s: |CLA|INS|P1 |P2 |LE |                len = 5 
         * case 3s: |CLA|INS|P1 |P2 |LC |...BODY...|     len = 6..260 
         * case 4s: |CLA|INS|P1 |P2 |LC |...BODY...|LE | len = 7..261
         *
         * (Extended length is not currently supported) 
         * case 2e: |CLA|INS|P1 |P2 |00|LE1|LE2|                    len = 7 
         * case 3e: |CLA|INS|P1 |P2 |00|LC1|LC2|...BODY...|         len = 8..65542 
         * case 4e: |CLA|INS|P1 |P2 |00|LC1|LC2|...BODY...|LE1|LE2| len =10..65544
         *
         * EMV uses case 2, case 3 or case 4 
         * Procedure byte 61 is case 2
         * Procedure byte 6c is case 2
         *
         * EMV: When required in a command message, Le shall always be set to
         * '00' Note that for SmartcardIO: CommandAPDU(byte[]) transforms Le=0
         * into 256 CommandAPDU(int, int.. etc) uses Ne instead of Le. So to
         * send Le=0x00 to ICC, then send Ne=256
         *
         * Use CommandAPDU(byte[]) for case 1 & 3 (those without Le) Use
         * CommandAPDU(int, int, int, int, int) for case 2 (but transform
         * Le=0x00 into Ne=256. SmartcardIO changes this back into Le=0x00) Use
         * CommandAPDU(int, int, int, int, byte, int, int, int) for case 4 (but
         * transform Le=0x00 into Ne=256. SmartcardIO changes this back into
         * Le=0x00)
         */
        CardResponseImpl response = null;
        CommandAPDU commandAPDU = null;

        //Find the 'case' and print to Log 
        if (cmd.length == 4) { //Case 1 (EMV doesn't use this)
            commandAPDU = new CommandAPDU(cmd);
            Log.debug("APDU case 1");
        } else if (cmd.length == 5) { //Case 2s
            commandAPDU = new CommandAPDU(
                    Util.byteToInt(cmd[0]),
                    Util.byteToInt(cmd[1]),
                    Util.byteToInt(cmd[2]),
                    Util.byteToInt(cmd[3]),
                    (cmd[4] == 0x00 ? 256 : Util.byteToInt(cmd[4])));
            Log.debug("APDU case 2");
        } else if (cmd.length == (5 + Util.byteToInt(cmd[4]))) { //Case 3s
//            if("T=1".equalsIgnoreCase(card.getProtocol())){
//                //Add Le to end of command
//                byte[] tmp = new byte[cmd.length+1];
//                System.arraycopy(cmd, 0, tmp, 0, cmd.length);
//                tmp[tmp.length-1] = (byte)0x00; //EMV: When required in a command message, Le shall always be set to '00'
//                cmd = tmp;
//                commandAPDU = new CommandAPDU(cmd);
//                Log.debug("APDU was case 3 but changed to case 4: "+commandAPDU);
//            }else{
            commandAPDU = new CommandAPDU(cmd);
            Log.debug("APDU case 3");
//            }
        } else if (cmd.length == (5 + Util.byteToInt(cmd[4]) + 1)) { //Case 4s
            byte[] data = new byte[Util.byteToInt(cmd[4])];
            System.arraycopy(cmd, 5, data, 0, data.length);
            int le = Util.byteToInt(cmd[cmd.length - 1]);
            commandAPDU = new CommandAPDU(
                    Util.byteToInt(cmd[0]),
                    Util.byteToInt(cmd[1]),
                    Util.byteToInt(cmd[2]),
                    Util.byteToInt(cmd[3]),
                    data,
                    0, //dataOffset
                    data.length,
                    (le == 0 ? 256 : le));
            Log.debug("APDU case 4");
        } else {
            //Might be extended length
            throw new IllegalArgumentException("Unsupported APDU format: " + Util.prettyPrintHexNoWrap(cmd));
        }
        Log.debug(commandAPDU + " (" + Util.prettyPrintHexNoWrap(commandAPDU.getBytes()) + ")");
        try {
            ResponseAPDU apdu = channel.transmit(commandAPDU);
            byte sw1 = (byte) apdu.getSW1();
            byte sw2 = (byte) apdu.getSW2();
            byte[] data = apdu.getData(); //Copy
            response = new CardResponseImpl(data, sw1, sw2, (short) apdu.getSW());
        } catch (CardException ce) {
            //if PCSCException: reflect to get error code
            //http://www.java2s.com/Open-Source/Java/6.0-JDK-Modules-sun/security/sun/security/smartcardio/PCSC.java.htm
            String desc = SmartcardioUtils.getPCSCErrorDescription(ce);
            throw new TerminalException("Error occured while transmitting command: " 
                    + Util.byteArrayToHexString(cmd) 
                    + (desc.isEmpty()?"":" ("+desc+")"), ce);
        }
        return response;
    }

    @Override
    public byte[] getATR() {
        return this.card.getATR().getBytes();
    }

    @Override
    public String toString() {
        return smartCardIOTerminal.getName() + " " + getConnectionInfo();
    }

    @Override
    public Terminal getTerminal() {
        return this;
    }

    @Override
    public String getConnectionInfo() {
        return channel.toString();
    }

    @Override
    public String getProtocol() {
        return this.card.getProtocol();
    }

    @Override
    public boolean disconnect(boolean attemptReset) throws TerminalException {
        try {
            card.disconnect(!attemptReset);
            return true;
        } catch (CardException ex) {
            throw new TerminalException(ex);
        }
    }

    /**
     * Perform warm reset
     *
     */
    @Override
    public void resetCard() throws TerminalException {
        try {
            //From scuba:
            // WARNING: Woj: the meaning of the reset flag is actually
            // reversed w.r.t. to the official documentation, false means
            // that the card is going to be reset, true means do not reset
            // This is a bug in the smartcardio implementation from SUN
            // Moreover, Linux PCSC implementation goes numb if you try to
            // disconnect a card that is not there anymore.

            // From sun/security/smartcardio/CardImpl.java:
            // SCardDisconnect(cardId, (reset ? SCARD_LEAVE_CARD : SCARD_RESET_CARD));
            // (http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/6-b14/sun/security/smartcardio/CardImpl.java?av=f)
            // The BUG: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7047033
            if (smartCardIOTerminal.isCardPresent()) {
                card.disconnect(false);
            }
            card = smartCardIOTerminal.connect("*");
            channel = card.getBasicChannel();
        } catch (CardException ex) {
            throw new TerminalException(ex);
        }
    }

    @Override
    public byte[] transmitControlCommand(int code, byte[] data) throws TerminalException {
        try {
            byte[] response = card.transmitControlCommand(code, data);
            return response;
        } catch (CardException ex) {
            Throwable cause = ex.getCause();
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            throw new TerminalException(cause.getMessage());
        }
    }
    
    private class CardResponseImpl implements CardResponse {

        private byte[] data;
        private byte sw1;
        private byte sw2;
        private short sw;

        CardResponseImpl(byte[] data, byte sw1, byte sw2, short sw) {
            this.data = data;
            this.sw1 = sw1;
            this.sw2 = sw2;
            this.sw = sw;
        }

        @Override
        public byte[] getData() {
            return data;
        }

        @Override
        public byte getSW1() {
            return sw1;
        }

        @Override
        public byte getSW2() {
            return sw2;
        }

        @Override
        public short getSW() {
            return sw;
        }
        
        @Override
        public String toString() {
            return Util.prettyPrintHex(data) + "\n" + Util.short2Hex(sw);
        }
    }
    

    //Terminal interface    
        
    @Override
    public CardConnection connect() throws TerminalException {
        throw new IllegalStateException("Already connected.");
    }

    @Override
    public String getName() {
        return smartCardIOTerminal.getName();
    }

    @Override
    public String getTerminalInfo() {
        return smartCardIOTerminal.toString();
    }

    @Override
    public boolean isCardPresent() throws TerminalException {
        try{
            return smartCardIOTerminal.isCardPresent();
        }catch(CardException ex){
            throw new TerminalException(ex);
        }
    }
}
