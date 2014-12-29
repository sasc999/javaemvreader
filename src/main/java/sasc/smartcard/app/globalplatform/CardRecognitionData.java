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
import sasc.emv.EMVTags;
import sasc.emv.EMVUtil;
import sasc.iso7816.BERTLV;
import sasc.iso7816.SmartCardException;
import sasc.iso7816.TLVUtil;
import sasc.util.Log;
import sasc.util.OIDUtil;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class CardRecognitionData {
    
    String tagAllocationAuthorityOID = null;
    String cardManagementTypeAndVersion = null;
    String cardIdentificationScheme = null;
    String scpVersionAndOptions = null;
    String cardConfigurationDetails = null;
    String cardChipDetails = null;
    
    private CardRecognitionData() {
        
    }
    
    public static CardRecognitionData parse(byte[] data) {
        CardRecognitionData cardRecognitionData = new CardRecognitionData();
        
        ByteArrayInputStream bis = new ByteArrayInputStream(data);

        if (bis.available() < 2) {
            throw new SmartCardException("Error parsing Card Recognition Data: Length < 2. Data: " + Util.prettyPrintHexNoWrap(data));
        }
        
        BERTLV tlv = TLVUtil.getNextTLV(bis);

        if (!tlv.getTag().equals(GPTags.SECURITY_DOMAIN_MANAGEMENT_DATA)) {
            throw new SmartCardException("Error parsing Card Recognition Data: No Response Template found. Data=" + Util.prettyPrintHexNoWrap(tlv.getValueBytes()));
        }

        bis = new ByteArrayInputStream(tlv.getValueBytes());

        while (bis.available() >= 2) {
            tlv = TLVUtil.getNextTLV(bis);
            if (tlv.getTag().equals(EMVTags.UNIVERSAL_TAG_FOR_OID)) {
                cardRecognitionData.tagAllocationAuthorityOID = OIDUtil.decodeOID(tlv.getValueBytes());
                Log.debug(cardRecognitionData.tagAllocationAuthorityOID);
            }else if(tlv.getTag().equals(GPTags.CARD_MANAGEMENT_TYPE_AND_VERSION_OID)) {
                tlv = TLVUtil.getNextTLV(new ByteArrayInputStream(tlv.getValueBytes()));
                cardRecognitionData.cardManagementTypeAndVersion = OIDUtil.decodeOID(tlv.getValueBytes());
                Log.debug(cardRecognitionData.cardManagementTypeAndVersion);
            }else if(tlv.getTag().equals(GPTags.CARD_IDENTIFICATION_SCHEME_OID)) {
                tlv = TLVUtil.getNextTLV(new ByteArrayInputStream(tlv.getValueBytes()));
                cardRecognitionData.cardIdentificationScheme = OIDUtil.decodeOID(tlv.getValueBytes());
                Log.debug(cardRecognitionData.cardIdentificationScheme);
            }else if(tlv.getTag().equals(GPTags.SECURE_CHANNEL_OID)) {
                tlv = TLVUtil.getNextTLV(new ByteArrayInputStream(tlv.getValueBytes()));
                cardRecognitionData.scpVersionAndOptions = OIDUtil.decodeOID(tlv.getValueBytes());
                Log.debug(cardRecognitionData.scpVersionAndOptions);
            }else if(tlv.getTag().equals(GPTags.CARD_CONFIGURATION_DETAILS)) {
                BERTLV tlvCcd = TLVUtil.getNextTLV(new ByteArrayInputStream(tlv.getValueBytes()));
                if (tlvCcd.getTag().equals(EMVTags.UNIVERSAL_TAG_FOR_OID)) {
                    cardRecognitionData.cardConfigurationDetails = OIDUtil.decodeOID(tlvCcd.getValueBytes());
                } else {
                    //Fallback
                    cardRecognitionData.cardConfigurationDetails = Util.prettyPrintHexNoWrap(tlv.getValueBytes());
                }
                Log.debug(cardRecognitionData.cardConfigurationDetails);
            }else if(tlv.getTag().equals(GPTags.CARD_CHIP_DETAILS)) {
                BERTLV tlvCcd = TLVUtil.getNextTLV(new ByteArrayInputStream(tlv.getValueBytes()));
                if (tlvCcd.getTag().equals(EMVTags.UNIVERSAL_TAG_FOR_OID)) {
                    cardRecognitionData.cardChipDetails = OIDUtil.decodeOID(tlvCcd.getValueBytes());
                } else {
                    //Fallback
                    cardRecognitionData.cardChipDetails = Util.prettyPrintHexNoWrap(tlv.getValueBytes());
                }
                Log.debug(cardRecognitionData.cardChipDetails);
            }
        }

        return cardRecognitionData;
    }
    
    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }
    
    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "Card Recognition Data");

        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);
        
        if(tagAllocationAuthorityOID != null) {
            pw.println(indentStr+"Tag Allocation Authority (OID)        : " + tagAllocationAuthorityOID);
        }
        if(cardManagementTypeAndVersion != null) {
            pw.println(indentStr+"Card Management Type and Version (OID): " + cardManagementTypeAndVersion);
        }
        if(cardIdentificationScheme != null) {
            pw.println(indentStr+"Card Identification Scheme (OID)      : " + cardIdentificationScheme);
        }
        if(scpVersionAndOptions != null) {
            pw.println(indentStr+"SCP Version and Options               : " + scpVersionAndOptions);
        }
        if(cardConfigurationDetails != null) {
            pw.println(indentStr+"Card Config Details                   : " + cardConfigurationDetails);
        }
        if(cardChipDetails != null) {
            pw.println(indentStr+"Card/Chip Details                     : " + cardChipDetails);
        }

    }
}
