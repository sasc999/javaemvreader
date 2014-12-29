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
package sasc.smartcard.pcsc;

import java.util.Arrays;
import java.util.List;
import sasc.emv.EMVUtil;
import sasc.emv.SW;
import sasc.smartcard.common.AtrHandler;
import sasc.smartcard.common.SmartCard;
import sasc.terminal.CardConnection;
import sasc.terminal.CardResponse;
import sasc.terminal.TerminalException;
import sasc.util.Log;
import sasc.util.Util;

/**
 * Handler for reading storage cards inserted into a PC/SC compatible reader
 * (That is, a supported card AND a supported reader is required)
 *
 * Commands are sent to the IFD (reader), and then translated to appropriate commands for the respective cards.
 * Some commands only return data from the readers memory (like Get UID/HB)
 *
 * Some readers (e.g. ACR) have their own API for communication with storage cards.
 * This handler only supports readers that comply with the PC/SC spec, using
 * so-called Pseudo APDUs interpreted by the card reader.
 * 
 * Class is 0xff
 *
 * Part 3. Requirements for PC-Connected Interface Devices pcsc3_v2.01.09.pdf
 * 3.2.2.1 Storage Card Functionality Support
 *
 * This handler is invoked by locating the special access data in ATR
 * Example of how a PC/SC compliant IFD announces that it translates communication to a memory card
 * Samsung Galaxy S3 Secure Element:
 * 4F = AID (or possibly just RID of authority for the data??)
 * 0C = Length of AID
 * A0 00 00 03 06 = RID of PC/SC
 * 00 = No information given
 * 00 00 = Name (registered ID in PC/SC doc)
 * 00 00 00 00 = RFU
 * http://smartcard-atr.appspot.com/parse?ATR=3b8f8001804f0ca0000003060000000000000068
 * See isotype.py
 * http://www.pcscworkgroup.com/specifications/files/pcsc3_v2.01.08_sup.pdf
 *
 * @author sasc
 */
public class StorageCardHandler implements AtrHandler {

    //Match ATRs that contain A0 00 00 03 06 SS NN NN RR RR RR RR
    //TODO this also matches storage cards that requires authentication.
    //Need to implement support for such cards
    private static final List<String> ATR_PATTERNS = Arrays.asList("3B 8F 80 01 80 4F 0C A0 00 00 03 06 .. .. .. 00 00 00 00 ..");

    @Override
    public boolean process(SmartCard card, CardConnection terminal) throws TerminalException {
        Log.debug("Attempting to read storage card");

        byte SW1;
        byte SW2;
        byte[] command;
        CardResponse response;
        byte[] data;

        //6a 81 = Function not supported
        //90 00 = Success
        Log.commandHeader("PS/SC GET DATA: UID (Command handled by terminal when card is contactless)");

        command = Util.fromHexString("ff ca 00 00 00"); //PC/SC 2.01 part 3 GetData: UID
        response = EMVUtil.sendCmdNoParse(terminal, command);
        SW1 = (byte) response.getSW1();
        SW2 = (byte) response.getSW2();
        if (response.getSW() == SW.SUCCESS.getSW()) {

        }

        Log.commandHeader("PC/SC GET DATA: Historical bytes (Command handled by terminal when card is contactless)");

        command = Util.fromHexString("ff ca 01 00 00"); //PC/SC 2.01 part 3 GetData: historical bytes from the ATS of a ISO 14443 A card without CRC
        response = EMVUtil.sendCmdNoParse(terminal, command);
        SW1 = (byte) response.getSW1();
        SW2 = (byte) response.getSW2();
        if (response.getSW() == SW.SUCCESS.getSW()) {

        }

        //Read Binary

        //    Warning
        //      6281 Part of returned data may be corrupted.
        //        82 End of file reached before reading expected number of bytes.
        //      6981 Command incompatible.
        //        82 Security status not satisfied.
        //        86 Command not allowed.
        //      6A81 Function not supported.
        //        82 File not found / Addressed block or byte does not exist.
        //
        //    Error
        //      6CXX Wrong length (wrong number Le; 'XX' is the exact number).

        int addressMSB = 0;
        int addressLSB = 0;
        while(addressMSB < 256) {
            Log.commandHeader("PC/SC Read Binary (Storage Card)");
            command = Util.fromHexString("FF B0 00 00 00"); //with Le
            command[2] = (byte)addressMSB;
            command[3] = (byte)addressLSB;
            response = EMVUtil.sendCmdNoParse(terminal, command);
            SW1 = response.getSW1();
            SW2 = response.getSW2();
            data = response.getData();
            if(data.length > 0 && response.getSW() == SW.SUCCESS.getSW()){
                addressLSB++;
                if(addressLSB > 255) {
                    addressLSB = 0;
                    addressMSB++;
                }
                continue;
            }
            break;
        }
        return false; //Don't handle exclusively. The card may have more applications or other functionality
    }

    @Override
    public List<String> getAtrPatterns() {
        return ATR_PATTERNS;
    }

    public static void main(String[] args) throws Exception {

    }
}
