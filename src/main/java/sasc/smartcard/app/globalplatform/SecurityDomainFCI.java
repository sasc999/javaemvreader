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
import sasc.emv.EMVTags;
import sasc.iso7816.AID;
import sasc.iso7816.BERTLV;
import sasc.iso7816.TLVException;
import sasc.iso7816.TLVUtil;
import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * Based on code by nelenkov
 */
public class SecurityDomainFCI {

    private static String GLOBAL_PLATFORM_OID = "2a864886fc6b";
    private AID securityManagerAid = null;
    private int dataFieldMaxLength = -1;
    private String applicationProductionLifecycleData = null;
    private String tagAllocationAuthorityOID = null;
    private String cardManagementTypeAndVersion = null;
    private String cardIdentificationScheme = null;
    private String gpVersion = null;
    private String secureChannelVersion = null;
    private String cardConfigurationDetails = null;
    private String cardChipDetails = null;

    private SecurityDomainFCI() {
    }

//Example response to select:
//
//00 a4 04 00 05 a0 00 00 00 03 00
//
//6f 65 -- File Control Information (FCI) Template
//      84 08 -- Dedicated File (DF) Name
//            a0 00 00 00 03 00 00 00 (BINARY)
//      a5 59 -- File Control Information (FCI) Proprietary Template
//            9f 65 01 -- Maximum length of data field in command message
//                     ff (BINARY)
//            9f 6e 06 -- Application production life cycle data
//                     47 91 73 51 2f 00 (BINARY)
//            73 4a -- Directory Discretionary Template
//                  06 07 -- [UNHANDLED TAG]
//                        2a 86 48 86 fc 6b 01 (BINARY)
//                  60 0c -- [UNHANDLED TAG]
//                        06 0a -- [UNHANDLED TAG]
//                              2a 86 48 86 fc 6b 02 02 01 01 (BINARY)
//                  63 09 -- [UNHANDLED TAG]
//                        06 07 -- [UNHANDLED TAG]
//                              2a 86 48 86 fc 6b 03 (BINARY)
//                  64 0b -- [UNHANDLED TAG]
//                        06 09 -- [UNHANDLED TAG]
//                              2a 86 48 86 fc 6b 04 02 15 (BINARY)
//                  65 0b -- [UNHANDLED TAG]
//                        06 09 -- [UNHANDLED TAG]
//                              2b 85 10 86 48 64 02 01 01 (BINARY)
//                  66 0c -- [UNHANDLED TAG]
//                        06 0a -- [UNHANDLED TAG]
//                              2b 06 01 04 01 2a 02 6e 01 02 (BINARY)
    public static SecurityDomainFCI parse(byte[] raw) {
        SecurityDomainFCI result = new SecurityDomainFCI();
        BERTLV tlv = TLVUtil.getNextTLV(new ByteArrayInputStream(raw));

        if (!tlv.getTag().equals(EMVTags.FCI_TEMPLATE)) {
            throw new TLVException("Invalid Security Domain FCI format");
        }

        ByteArrayInputStream templateStream = tlv.getValueStream();
        while (templateStream.available() >= 2) {
            tlv = TLVUtil.getNextTLV(templateStream);
            if (tlv.getTag().equals(EMVTags.DEDICATED_FILE_NAME)) {
                result.securityManagerAid = new AID(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.FCI_PROPRIETARY_TEMPLATE)) {
                ByteArrayInputStream fciBis = new ByteArrayInputStream(tlv.getValueBytes());
                int totalLen = fciBis.available();
                int templateLen = tlv.getLength();
                while (fciBis.available() > (totalLen - templateLen)) {
                    tlv = TLVUtil.getNextTLV(fciBis);
                    if (tlv.getTag().equals(GPTags.SECURITY_DOMAIN_MANAGEMENT_DATA)) {
                        ByteArrayInputStream sdmBis = new ByteArrayInputStream(tlv.getValueBytes());
                        int diff = sdmBis.available() - tlv.getLength();
                        while (sdmBis.available() > diff) {
                            tlv = TLVUtil.getNextTLV(sdmBis);
                            if (tlv.getTag().equals(EMVTags.UNIVERSAL_TAG_FOR_OID)) {
                                result.tagAllocationAuthorityOID = Util.byteArrayToHexString(tlv.getValueBytes());
                                result.tagAllocationAuthorityOID = result.tagAllocationAuthorityOID.replace(GLOBAL_PLATFORM_OID, "globalPlatform ");
                            } else if (tlv.getTag().equals(GPTags.CARD_MANAGEMENT_TYPE_AND_VERSION_OID)) {
                                ByteArrayInputStream cmBis = new ByteArrayInputStream(tlv.getValueBytes());
                                tlv = TLVUtil.getNextTLV(cmBis);
                                if (tlv.getTag().equals(EMVTags.UNIVERSAL_TAG_FOR_OID)) {
                                    result.cardManagementTypeAndVersion = Util.byteArrayToHexString(tlv.getValueBytes());
                                    result.cardManagementTypeAndVersion = result.cardManagementTypeAndVersion.replace(GLOBAL_PLATFORM_OID, "globalPlatform ");
                                    //2A864886FC6B 02 v == {globalPlatform 2 v}
                                    int prefixLength = 7;
                                    int valueLength = tlv.getValueBytes().length;
                                    byte[] version = Arrays.copyOfRange(tlv.getValueBytes(), prefixLength, valueLength);
                                    StringBuilder buff = new StringBuilder();
                                    for (int i = 0; i < version.length; i++) {
                                        buff.append(0xff & version[i]);
                                        if (i != version.length - 1) {
                                            buff.append(".");
                                        }
                                    }
                                    result.gpVersion = buff.toString();
                                }
                            } else if (tlv.getTag().equals(GPTags.CARD_IDENTIFICATION_SCHEME_OID)) {
                                ByteArrayInputStream cisBis = new ByteArrayInputStream(tlv.getValueBytes());
                                tlv = TLVUtil.getNextTLV(cisBis);
                                if (tlv.getTag().equals(EMVTags.UNIVERSAL_TAG_FOR_OID)) {
                                    result.cardIdentificationScheme = Util.byteArrayToHexString(tlv.getValueBytes());
                                    result.cardIdentificationScheme = result.cardIdentificationScheme.replace(GLOBAL_PLATFORM_OID, "globalPlatform ");
                                }
                            } else if (tlv.getTag().equals(GPTags.SECURE_CHANNEL_OID)) {
                                int len = tlv.getValueBytes().length;
                                result.secureChannelVersion = String.format("SCP%02d (options: 0x%02X)", tlv.getValueBytes()[len - 2], tlv.getValueBytes()[len - 1]);
                            } else if (tlv.getTag().equals(GPTags.CARD_CONFIGURATION_DETAILS)) {
                                result.cardConfigurationDetails = Util.byteArrayToHexString(tlv.getValueBytes());
                            } else if (tlv.getTag().equals(GPTags.CARD_CHIP_DETAILS)) {
                                result.cardChipDetails = Util.byteArrayToHexString(tlv.getValueBytes());
                                //TODO use parseCardRecData function
                            }
                        }
                    } else if (tlv.getTag().equals(GPTags.APPLICATION_PRODUCTION_LIFECYCLE_DATA)) {

                        //from Open Platform Version 2.0.1' - 9.8.3.1

                        //'9F6E' Application production life cycle data mandatory
                        //----
                        //from "Sm@rtCafe Expert 2.0 reference manual" a op 2.0.1' card
                        //
                        //Card Manager Response "9F 6E" Card Manager production life cycle
                        //
                        //The Card Manager production life cycle contains the operating system identifier, the operating system release date and the operating system release level of the card.
                        //----
                        //from cyberflex access manual http://www2.cs.uh.edu/~shah/teaching/fall06/CyberflexPG.pdf
                        //from page 130:
                        //
                        //2 B 9F 6E Tag indicating that the Card Manager production life cycle
                        //(CMPLC) data follows (as described in the SetStatus
                        //command on page 133).
                        //1 B 06 Length of the CMPLC data.
                        //6 B varies Value of the CMPLC data =
                        //- Card OS ID (2 B),
                        //- Card OS release level (2 B), and
                        //- Card OS release date (2 B).

                        //For Yubikey NEO:
                        //Card OS ID 2 B
                        //Card OS release date 2 B
                        //Card OS release level 2 B


                        result.applicationProductionLifecycleData = Util.byteArrayToHexString(tlv.getValueBytes());

                    } else if (tlv.getTag().equals(GPTags.MAXIMUM_LENGTH_COMMAND_DATA_FIELD)) {
                        if (tlv.getValueBytes().length == 1) {
                            result.dataFieldMaxLength = 0xff & tlv.getValueBytes()[0];
                        } else if (tlv.getValueBytes().length == 2) {
                            result.dataFieldMaxLength = Util.byte2Short(tlv.getValueBytes()[0], tlv.getValueBytes()[1]);
                        } else {
                            //NO-OP
                        }
                    }
                }
            }
        }
        return result;
    }

    public AID getSecurityManagerAid() {
        return securityManagerAid;
    }

    public int getDataFieldMaxLength() {
        return dataFieldMaxLength;
    }

    public String getApplicationProductionLifecycleData() {
        return applicationProductionLifecycleData;
    }

    public String getTagAllocationAuthorityOID() {
        return tagAllocationAuthorityOID;
    }

    public String getCardManagementTypeAndVersion() {
        return cardManagementTypeAndVersion;
    }

    public String getCardIdentificationScheme() {
        return cardIdentificationScheme;
    }

    public String getGpVersion() {
        return gpVersion;
    }

    public String getSecureChannelVersion() {
        return secureChannelVersion;
    }

    public String getCardConfigurationDetails() {
        return cardConfigurationDetails;
    }

    public String getCardChipDetails() {
        return cardChipDetails;
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "Security Domain FCI");

        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);

        if (securityManagerAid != null) {
            securityManagerAid.dump(pw, indent + Log.INDENT_SIZE);
        }

        if (dataFieldMaxLength != -1) {
            pw.println(indentStr + "Max Length in Command Data Field      : " + dataFieldMaxLength + " bytes");
        }
        if (applicationProductionLifecycleData != null) {
            pw.println(indentStr + "Application Prod. Life-Cycle Data     : " + applicationProductionLifecycleData);
        }
        if (tagAllocationAuthorityOID != null) {
            pw.println(indentStr + "Tag Allocation Authority (OID)        : " + tagAllocationAuthorityOID);
        }
        if (cardManagementTypeAndVersion != null) {
            pw.println(indentStr + "Card Management Type and Version (OID): " + cardManagementTypeAndVersion);
        }
        if (cardIdentificationScheme != null) {
            pw.println(indentStr + "Card Identification Scheme (OID)      : " + cardIdentificationScheme);
        }
        if (gpVersion != null) {
            pw.println(indentStr + "Global Platform Version               : " + gpVersion);
        }
        if (secureChannelVersion != null) {
            pw.println(indentStr + "Secure Channel Version                : " + secureChannelVersion);
        }
        if (cardConfigurationDetails != null) {
            pw.println(indentStr + "Card Config Details                   : " + cardConfigurationDetails);
        }
        if (cardChipDetails != null) {
            pw.println(indentStr + "Card/Chip Details                     : " + cardChipDetails);
        }
    }
}