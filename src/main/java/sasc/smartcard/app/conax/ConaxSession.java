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
package sasc.smartcard.app.conax;

import java.util.Arrays;
import java.util.List;
import sasc.emv.EMVUtil;
import sasc.smartcard.common.AtrHandler;
import sasc.smartcard.common.SmartCard;
import sasc.terminal.CardConnection;
import sasc.terminal.CardResponse;
import sasc.terminal.TerminalException;
import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class ConaxSession implements AtrHandler {
    
    //3B 24 00 80 72 A4 45
    private static final List<String> ATR_PATTERNS = Arrays.asList("3B 24 00 .. .. .. 45", "3B 24 00 30 42 30 30", "3B 34 00 00 30 42 30 30", "3B 34 94 00 30 42 30 30");
    
    public static AtrHandler getAtrHandler() {
        return new ConaxSession();
    }
    
    @Override
    public List<String> getAtrPatterns() {
        return ATR_PATTERNS;
    }
    
    @Override
    public boolean process(SmartCard card, CardConnection terminal) throws TerminalException {
        Log.debug("Found Conax Pay TV Card ATR");
        
        byte SW1;
        byte SW2;
        String command;
        CardResponse response;
        
        //Init card
        Log.commandHeader("Send Conax command 'Init card'");
        command = "DD 26 00 00 03 10 01 01 00"; //with Le
        response = EMVUtil.sendCmdNoParse(terminal, command);
        SW1 = response.getSW1();
        SW2 = response.getSW2();
        if (SW1 == (byte) 0x98) {

        }
        
        //Get init card response
        Log.commandHeader("Send Conax command 'Get response'");
        command = "DD CA 00 00 "+Util.byte2Hex(response.getSW2()); //with Le
        response = EMVUtil.sendCmdNoParse(terminal, command);
        SW1 = response.getSW1();
        SW2 = response.getSW2();
                
        
        //Init EMM
        Log.commandHeader("Send Conax command 'Init EMM'");
        command = "DD 82 00 00 14 11 12 01 B0 0F FF FF DD 00 00 09 04 0B 00 E0 30 1B 64 3D FE 00"; //with Le
        response = EMVUtil.sendCmdNoParse(terminal, command);
        SW1 = response.getSW1();
        SW2 = response.getSW2();
        
        //Get init EMM response
        Log.commandHeader("Send Conax command 'Get response'");
        command = "DD CA 00 00 "+Util.byte2Hex(response.getSW2()); //with Le
        response = EMVUtil.sendCmdNoParse(terminal, command);
        SW1 = response.getSW1();
        SW2 = response.getSW2();
        
        byte[] data = response.getData();
        if(data.length == 0x1a){
            long cardNumber = Util.byteArrayToLong(data, 13, 4);

            String cardNumberStr = String.valueOf(cardNumber);
            
            while(cardNumberStr.length() < 11){
                cardNumberStr = "0"+cardNumberStr;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(cardNumberStr.substring(0, 3));
            sb.append(" ");
            sb.append(cardNumberStr.substring(3, 7));
            sb.append(" ");
            sb.append(cardNumberStr.substring(7, 11));
            sb.append("-X");
            
            Log.info("Card Number: " + sb.toString());
        }
        
        
        //Menu Request
        Log.commandHeader("Send Conax command 'Get Menu'");
        command = "DD B2 00 00 03 15 01 AA 00"; //with Le
        response = EMVUtil.sendCmdNoParse(terminal, command);
        SW1 = response.getSW1();
        SW2 = response.getSW2();
        
        while(Util.isBitSet(SW1, 4)){
            //Get Menu response
            Log.commandHeader("Send Conax command 'Get response'");
            command = "DD CA 00 00 "+Util.byte2Hex(response.getSW2()); //with Le
            response = EMVUtil.sendCmdNoParse(terminal, command);
            SW1 = response.getSW1();
            SW2 = response.getSW2();
        }
        
        //Crypt (Get Unique Address)
        Log.commandHeader("Send Conax command 'Get Unique Address'");
        command = "DD C2 00 00 02 66 00 00"; //with Le
        response = EMVUtil.sendCmdNoParse(terminal, command);
        SW1 = response.getSW1();
        SW2 = response.getSW2();
        
        //Get Crypt response
        Log.commandHeader("Send Conax command 'Get response'");
        command = "DD CA 00 00 "+Util.byte2Hex(response.getSW2()); //with Le
        response = EMVUtil.sendCmdNoParse(terminal, command);
        SW1 = response.getSW1();
        SW2 = response.getSW2();
        
        //Get Return Channel Details
        Log.commandHeader("Send Conax command 'Get Return Channel Details'");
        command = "DD C4 00 00 02 1B 00 00"; //with Le
        response = EMVUtil.sendCmdNoParse(terminal, command);
        SW1 = response.getSW1();
        SW2 = response.getSW2();
        while(Util.isBitSet(SW1, 4)){ //0x9c || 0x98
            //Get Subscription info response
            Log.commandHeader("Send Conax command 'Get response'");
            command = "DD CA 00 00 "+Util.byte2Hex(response.getSW2()); //with Le
            response = EMVUtil.sendCmdNoParse(terminal, command);
            SW1 = response.getSW1();
            SW2 = response.getSW2();
        }
        
        //Get Subscription info
        Log.commandHeader("Send Conax command 'Get Subscription Info'");
        command = "DD C6 00 00 03 1C 01 00 00"; //with Le
        response = EMVUtil.sendCmdNoParse(terminal, command);
        SW1 = response.getSW1();
        SW2 = response.getSW2();
        
        while(Util.isBitSet(SW1, 4)){ //0x9c || 0x98
            //Get Subscription info response
            Log.commandHeader("Send Conax command 'Get response'");
            command = "DD CA 00 00 "+Util.byte2Hex(response.getSW2()); //with Le
            response = EMVUtil.sendCmdNoParse(terminal, command);
            SW1 = response.getSW1();
            SW2 = response.getSW2();
        }
        
        return true; //Handle exclusively
    }
 
    public static void main(String[] args) throws Exception {
//        CardConnection cardConnection = TerminalUtil.connect(TerminalUtil.State.CARD_INSERTED);
//        
//        ConaxSession.start(cardConnection);
        
        System.out.println(parseDate((byte)0x21, (byte)0x15));
        System.out.println(parseDateToString((byte)0x21, (byte)0x15));
    }
    
    /**
     * returns the date formatted as 'yyyymmdd'
     */
    private static int parseDate(byte data0, byte data1){
        int y;
        int m;
        int d;
        
        int l;
        
        y= 1990+ ((data1>>>4) + ((data0>>>5)&0x7)*10);
        m= data1&0xf;
        d= data0&0x1f;
        l= (y*100+m)*100+d;

        return l;
    }
    
    private static String parseDateToString(byte data0, byte data1){
        int y;
        int m;
        int d;
        
        String s;
        
        y= 1990+ ((data1>>>4) + ((data0>>>5)&0x7)*10);
        m= data1&0xf;
        d= data0&0x1f;
//        l= (y*100+m)*100+d;

        return y+"-"+(m<10?"0"+m:m)+"-"+(d<10?"0"+d:d);
    }
}
