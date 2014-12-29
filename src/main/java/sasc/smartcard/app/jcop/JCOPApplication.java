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
package sasc.smartcard.app.jcop;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import sasc.iso7816.AID;
import sasc.iso7816.Application;
import sasc.lookup.ATR_DB;
import sasc.smartcard.common.SmartCard;
import sasc.terminal.CardConnection;
import sasc.terminal.CardResponse;
import sasc.terminal.TerminalUtil;
import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class JCOPApplication implements Application {
    private static final Map<String, Integer> FIELD_NAMES_LENGTHS = new LinkedHashMap<String, Integer>();
    private Map<String, String> fields = new LinkedHashMap<String, String>();
    private byte[] data;
    private AID aid;
    private SmartCard card;
    
    static {
        FIELD_NAMES_LENGTHS.put("FABKEY ID", 1);
        FIELD_NAMES_LENGTHS.put("PATCH ID", 1);
        FIELD_NAMES_LENGTHS.put("TARGET ID", 1);
        FIELD_NAMES_LENGTHS.put("MASK ID", 1);
        FIELD_NAMES_LENGTHS.put("CUSTOM MASK", 4);
        FIELD_NAMES_LENGTHS.put("MASK NAME", 6);
        FIELD_NAMES_LENGTHS.put("FUSE STATE", 1);
        FIELD_NAMES_LENGTHS.put("ROM INFO", 3);
        FIELD_NAMES_LENGTHS.put("COMBO NAME", 1);
    }
    
    public JCOPApplication(AID aid, byte[] data, SmartCard card) {
        if(aid == null) {
            throw new IllegalArgumentException("AID cannot be null");
        }
        if(data == null) {
            throw new IllegalArgumentException("Param data cannot be null");
        }
        if(data.length != 19) {
            throw new IllegalArgumentException("data.length must be 19, but was "+data.length);
        }
//        if(card == null) {
//            throw new IllegalArgumentException("Param card cannot be null");
//        }
        this.aid = aid;
        this.data = data;
        this.card = card;
        
        int idx = 0;

        for (String fieldName : FIELD_NAMES_LENGTHS.keySet()) {
            int length = FIELD_NAMES_LENGTHS.get(fieldName);
            byte[] value = Arrays.copyOfRange(data, idx, idx + length);
            idx += length;
            fields.put(fieldName, Util.byteArrayToHexString(value));
        }
    }
    
    @Override
    public AID getAID() {
        return aid;
    }

    @Override
    public SmartCard getCard() {
        return card;
    }
    
    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    @Override
    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "JCOP Application");

        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);
        
        if (aid != null) {
            aid.dump(pw, indent + Log.INDENT_SIZE);
        }
        
        for (String key : fields.keySet()) {
            pw.println(indentStr+String.format("%s: %s", key, fields.get(key)) 
                    + ("MASK NAME".equals(key)?" ("+Util.getSafePrintChars(Util.fromHexString(fields.get(key)))+")":""));
        }
    }
    
    public static void main(String[] args) throws Exception {
        AID aid = new AID("A0 00 00 01 67 41 30 00 FF");
        JCOPApplication jcopApp = new JCOPApplication(aid, Util.fromHexString("B3 11 01 29 00 00 00 00 50 48 36 35 30 41 01 03 C1 3C 82"), null);
        System.out.println(jcopApp);
        
        CardConnection cardConnection = TerminalUtil.connect(TerminalUtil.State.CARD_INSERTED); //Waits for card insertion
        Log.info(Util.prettyPrintHexNoWrap(cardConnection.getATR()));
        Log.info(ATR_DB.searchATR(cardConnection.getATR()).toString());
        
        CardResponse selectJcopResponse = cardConnection.transmit(Util.fromHexString("00 a4 04 00 09 a0 00 00 01 67 41 30 00 ff 00"));
        System.out.println(selectJcopResponse);
        System.out.println(new JCOPApplication(aid, selectJcopResponse.getData(), null));

    }
}

    //00h is not fused (not personalized), 01h is fused. 
    //If not fused, you need the transport key,
    //as the global platform keys are set randomly.

    //
    // /identify
    // => 00 A4 04 00 09 A0 00 00 01 67 41 30 00 FF          .........gA0..
    // (28350 usec)
    // <= B3 11 01 29 00 00 00 00 50 48 36 35 30 41 01 03    ...)....PH650A..
    //    C1 3C 82 6A 82                                     .<.j.
    // Status: File not found
    // FABKEY ID:   0xB3
    // PATCH ID:    0x11
    // TARGET ID:   0x01 (smartmx)
    // MASK ID:     0x29 (41)
    // CUSTOM MASK: 00000000
    // MASK NAME:   PH650A
    // FUSE STATE:  fused
    // ROM INFO:    C13C82
    // COMBO NAME:  smartmx-m29.B3.11-PH650A
    // 
    //
    
    //RFIDIOT JCOP 41 (Random UID)
//    FABKEY ID: 34
//    PATCH ID: 04 
//    TARGET ID: 01 (smartmx)
//    MASK ID: 24 
//    CUSTOM MASK: 00 00 00 00 
//    MASK NAME 50 48 35 32 32 44 
//    FUSE STATE: 01 
//    ROM INFO: 03 d8 8d 
//    COMBO NAME: 93
    
   //JCOP 31
//    30 
//    04 
//    01 (smartmx)
//    24 
//    00 00 00 00 
//    50 48 35 32 32 44 
//    00 
//    03 d8 8d 
//    93
    
    //Yubikey NEO
//    03 
//    71 
//    01 SmartMX
//    38 (mask56)
//    00 00 00 00 
//    4e 58 31 33 30 41 (NX130A)
//    01 (FUSED)
//    03 c3 10 
//    ea
