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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import sasc.emv.EMVUtil;
import sasc.iso7816.BERTLV;
import sasc.iso7816.TLVUtil;
import sasc.util.Log;
import sasc.util.Util;

/**
 * Card Production Life-Cycle Data (CPLC)
 * 
 * Provides information on "who did what" prior to card issuance.
 *
 * Based on code by nelenkov
 */
public class CPLC {

    private static final Map<String, Integer> FIELD_NAMES_LENGTHS = new LinkedHashMap<String, Integer>();
    private Map<String, String> fields = new LinkedHashMap<String, String>();

    // 'Visa GP 2.1.1 Card Implementation Requirements version 1.0' on page 22, 3.2.1.2 Card Production Life Cycle
    static {
        FIELD_NAMES_LENGTHS.put("IC Fabricator", 2);
        FIELD_NAMES_LENGTHS.put("IC Type", 2);
        FIELD_NAMES_LENGTHS.put("Operating System Provider Identifier", 2);
        FIELD_NAMES_LENGTHS.put("Operating System Release Date", 2);
        FIELD_NAMES_LENGTHS.put("Operating System Release Level", 2);
        FIELD_NAMES_LENGTHS.put("IC Fabrication Date", 2);
        FIELD_NAMES_LENGTHS.put("IC Serial Number", 4);
        FIELD_NAMES_LENGTHS.put("IC Batch Identifier", 2);
        FIELD_NAMES_LENGTHS.put("IC ModuleFabricator", 2);
        FIELD_NAMES_LENGTHS.put("IC ModulePackaging Date", 2);
        FIELD_NAMES_LENGTHS.put("ICC Manufacturer", 2);
        FIELD_NAMES_LENGTHS.put("IC Embedding Date", 2);
        FIELD_NAMES_LENGTHS.put("Prepersonalizer Identifier", 2);
        FIELD_NAMES_LENGTHS.put("Prepersonalization Date", 2);
        FIELD_NAMES_LENGTHS.put("Prepersonalization Equipment", 4);
        FIELD_NAMES_LENGTHS.put("Personalizer Identifier", 2);
        FIELD_NAMES_LENGTHS.put("Personalization Date", 2);
        FIELD_NAMES_LENGTHS.put("Personalization Equipment", 4);
    }

    private CPLC() {
    }

    public static CPLC parse(byte[] raw) {
        CPLC result = new CPLC();
        
        byte[] cplc = null;
        if (raw.length == 42){
            cplc = raw;
        }else if(raw.length == 45){
            BERTLV tlv = TLVUtil.getNextTLV(new ByteArrayInputStream(raw));
            if(!tlv.getTag().equals(GPTags.CPLC)){
                throw new IllegalArgumentException("CPLC data not valid. Found tag: " + tlv.getTag());
            }
            cplc = tlv.getValueBytes();
        }else{
            throw new IllegalArgumentException("CPLC data not valid.");
        }
        int idx = 0;

        for (String fieldName : FIELD_NAMES_LENGTHS.keySet()) {
            int length = FIELD_NAMES_LENGTHS.get(fieldName);
            byte[] value = Arrays.copyOfRange(cplc, idx, idx + length);
            idx += length;
            String valueStr = Util.byteArrayToHexString(value);
            result.fields.put(fieldName, valueStr);
        }
        return result;
    }
    
    /**
     * Global Platform CUID
     * 
     * Concatenating four data fields from the Global Platform Card Production Life Cycle (CPLC) data
     * in the following sequence forms a card unique identifier (CUID):
     * ICFabricatorID || ICType || ICBatchIdentifier || ICSerialNumber
     * (10 bytes)
     * @return 
     */
    public String createCardUniqueIdentifier() {
        return fields.get("IC Fabricator") + fields.get("IC Type") + fields.get("IC Batch Identifier") + fields.get("IC Serial Number");
    }
    
    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }
    
    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "Card Production Life Cycle Data (CPLC)");

        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);
        
        for (String key : fields.keySet()) {
            pw.println(indentStr+String.format("%s: %s", key, fields.get(key) + ("IC Fabricator".equals(key)?" ("+getICFabricationName(fields.get(key))+")":"")));
        }
        pw.println(indentStr + " -> Card Unique Identifier: "+createCardUniqueIdentifier());
    }
    
    public static String getICFabricationName(String id) {
        if("4180".equals(id)){
            return "Atmel";
        }
        if("4250".equals(id)){
            return "Samsung";
        }
        if("4790".equals(id)){
            return "NXP";
        }
        if("4090".equals(id)){
            return "Infineon";
        }
        if("3060".equals(id)){
            return "Renesas";
        }
        return "";
    }
    
    public static void main(String[] args){
        System.out.println(parse(Util.fromHexString("9F7F2A47905168479112 10380022230018499621 33481222300000000005 182B3031383439000000 0000000000")));
    }
}

/**
 *
*
* IC Fabricator :
* 4180 Atmel
* 4250 Samsung
* 4790 NXP
* 4090 Infineon
* ???? ST
* 3060 Renesas http://www.cryptsoft.com/fips140/unpdf/140sp749-11.html
*
* IC Type :
* 2599 S3CC9P9
* 5032 Phillips
* 0107 Atmel AT90SC12872RCFT Revision J
*
* Operating System ID :
* 4051 IBM
* ???? Feitian
* 0755 Athena OS755
* 
* CardLogix:
* IC Type: 0107 Atmel AT90SC12872RCFT Revision J
* Operating system release date: 6250
* Operating system release level: x7x4 
*       7: Firmware Version Part 1
*       4: Firmware Version Part 2
*       x: N/A
* 
* It is possible to verify that a module that was known to be in the approved mode 
* of operation is still in the approved mode of operation.
* The Card Administrator must:
* 1. SELECT the ISD and send a GET DATA APDU command with the CPLC Data tag
* '9F7F' and verify that the returned data contains fields as follows (other fields are not
* relevant here). This verifies the version of the OS.
* Data Element Length Value Version
* IC type 2 '0107' Atmel AT90SC12872RCFT
* Revision J
* Operating system release date 2 '6250'
* Operating system release level 2 'x7x4' 7: Firmware Version Part 1
* 4: Firmware Version Part 2
* x: N/A
* 
* 
* Aspects Software OS755 for Renesas XMobile Card Module
* Firmware Version: OS755 version 2.4.6
* Hardware Version: AE46C1 Version 0.1
* GP 2.1 JC 2.11
* 
* IC fabricator 2 '3060'
* IC type 2 '4643'
* Operating system identifier 2 '0755'
* Operating system release date 2 'xxxx' In the format specified by Visa GP
* Operating system release level 2 '0246'
* IC fabrication date 2 'xxxx'
* IC serial number 4 'xxxx'
* IC batch identifier 2 'xxxx'
* IC module fabricator 2 'xxxx'
* IC module packaging date 2 'xxxx'
* ICC manufacturer 2 '3060'
* IC embedding date 2 '0000'
*
* Yubico NEO
* IC FAB 4790 NXP
* IC TYP 5168 SmartMX
* OS  ID 4791 JCOP
* ICMODFAB 4812 NXP
* 
* Dates:
* 1210 2011/07/29
* 2223 2012/08/10
* 2230 2012/08/17
* 
*/
