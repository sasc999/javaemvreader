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
package sasc.smartcard.app.globalplatform;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import sasc.emv.EMVUtil;
import sasc.iso7816.BERTLV;
import sasc.iso7816.SmartCardException;
import sasc.iso7816.TLVUtil;
import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class KeyInformationTemplate {
    
    List<KeyData> keys = new ArrayList<KeyData>();
    
    private static class KeyData {
        int keyIdentifier = -1;
        int keyVersionNumber = -1;
        byte keyType;
        int keyLength = -1;
        
        @Override
        public String toString() {
            StringWriter sw = new StringWriter();
            dump(new PrintWriter(sw), 0);
            return sw.toString();
        }

        public void dump(PrintWriter pw, int indent) {
            pw.println(Util.getSpaces(indent) + "Key");

            String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);

            pw.println(indentStr+"Identifier     : "+keyIdentifier);
            pw.println(indentStr+"Version Number : "+keyVersionNumber);
            pw.println(indentStr+"Type           : 0x"+Util.byte2Hex(keyType) + " ("+KeyInformationTemplate.getKeyTypeCoding(keyType)+")");
            pw.println(indentStr+"Length         : "+keyLength + " bytes");
        }
    }
    
    private KeyInformationTemplate() {
        
    }
    
//    c0 04 -- Key Info Data
//          01 ff 80 10 (BINARY)
//    c0 04 -- Key Info Data
//          02 ff 80 10 (BINARY)
//    c0 04 -- Key Info Data
//          03 ff 80 10 (BINARY)

    
    public static KeyInformationTemplate parse(byte[] data) {
        KeyInformationTemplate keyInformationTemplate = new KeyInformationTemplate();
        
        ByteArrayInputStream bis = new ByteArrayInputStream(data);

        if (bis.available() < 2) {
            throw new SmartCardException("Error parsing Key Information Template: Length < 2. Data: " + Util.prettyPrintHexNoWrap(data));
        }
        
        BERTLV tlv = TLVUtil.getNextTLV(bis);

        if (tlv.getTag().equals(GPTags.KEY_INFO_TEMPLATE)) { //Strip template tag
            bis = tlv.getValueStream();
            tlv = TLVUtil.getNextTLV(bis);
        }
        
        while(true) {
            if (!tlv.getTag().equals(GPTags.KEY_INFO_DATA)) { 
                throw new SmartCardException("Error parsing Key Information Template: Expected Key Info Data 0xc0, but found: " + Util.prettyPrintHexNoWrap(tlv.toBERTLVByteArray()));
            }
            
            byte[] values = tlv.getValueBytes();
            
            if (values.length != 4) { 
                throw new SmartCardException("Error parsing Key Information Template: Expected 4 bytes, but found: " + Util.prettyPrintHexNoWrap(values));
            }
            
            Log.debug("Key Info Data: "+Util.prettyPrintHexNoWrap(values));
            
            KeyData keyData = new KeyData();
            keyData.keyIdentifier = Util.byteToInt(values[0]);
            keyData.keyVersionNumber = Util.byteToInt(values[1]);
            keyData.keyType = values[2];
            keyData.keyLength = Util.byteToInt(values[3]);
          
            keyInformationTemplate.keys.add(keyData);
            
            if(bis.available() < 2) {
                break;
            }
            tlv = TLVUtil.getNextTLV(bis);
        } 
        
        return keyInformationTemplate;
    }
    
    public static String getKeyTypeCoding(byte value) {
        
        int type = Util.byteToInt(value);
        
        if(type <= 0x7f){
            return "Reserved for private use";
        }
        
        if(type >= 0x81 && type <= 0x9f) {
            return "RFU (symmetric algorithms)";
        }
        
        if(type >= 0xA9 && type <= 0xFE) {
            return "RFU (asymmetric algorithms)";
        }
        
        switch(type) {
            case 0x80:
                return "DES - mode (EBC/CBC) implicitly known";
            case 0xA0:
                return "RSA Public Key - public exponent e component (clear text)";
            case 0xA1:
                return "RSA Public Key - modulus N component (clear text)";
            case 0xA2:
                return "RSA Private Key - modulus N component";
            case 0xA3:
                return "RSA Private Key - private exponent d component";
            case 0xA4:
                return "RSA Private Key - Chinese Remainder P component";
            case 0xA5:
                return "RSA Private Key - Chinese Remainder Q component";
            case 0xA6:
                return "RSA Private Key - Chinese Remainder PQ component";
            case 0xA7:
                return "RSA Private Key - Chinese Remainder DP1 component";
            case 0xA8:
                return "RSA Private Key - Chinese Remainder DQ1 component";
            case 0xff:
                return "Not Available";       
        }
        
        throw new IllegalArgumentException("Unexpected value: 0x"+Util.byte2Hex(value));
    }
    
    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }
    
    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "Key Information");
        
        for(KeyData keyData : keys) {
            keyData.dump(pw, indent + Log.INDENT_SIZE);
        }
    }
}
