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
package sasc.emv;

import sasc.iso7816.ShortFileIdentifier;
import sasc.smartcard.common.SmartCard;
import sasc.iso7816.TagValueType;
import sasc.iso7816.TagAndLength;
import sasc.iso7816.Tag;
import sasc.util.Log;
import sasc.iso7816.SmartCardException;
import sasc.iso7816.BERTLV;
import sasc.iso7816.AID;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import sasc.emv.system.visa.VISATags;
import sasc.iso7816.ATR;
import sasc.iso7816.TLVException;
import sasc.iso7816.TLVUtil;
import sasc.terminal.CardResponse;
import sasc.terminal.TerminalException;
import sasc.terminal.CardConnection;
import static sasc.util.Log.COMMAND_HEADER_FRAMING;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class EMVUtil {

    /**
     * No response parsing
     *
     * @param terminal
     * @param command
     * @return
     * @throws TerminalException
     */
    //TODO remove this and replace with CLS/INS bit indication (response formatted in a TLV structure)
    public static CardResponse sendCmdNoParse(CardConnection terminal, byte[] cmd) throws TerminalException {
        return sendCmdInternal(terminal, cmd, false);
    }

    public static CardResponse sendCmd(CardConnection terminal, byte[] cmd) throws TerminalException {
        return sendCmdInternal(terminal, cmd, true);
    }
    public static CardResponse sendCmdNoParse(CardConnection terminal, String cmd) throws TerminalException {
        return sendCmdInternal(terminal, Util.fromHexString(cmd), false);
    }

    public static CardResponse sendCmd(CardConnection terminal, String cmd) throws TerminalException {
        return sendCmdInternal(terminal, Util.fromHexString(cmd), true);
    }

    //TODO move this to generic ISO7816 routine?
    private static CardResponse sendCmdInternal(CardConnection terminal, byte[] cmd, boolean doParseTLVData) throws TerminalException {
        byte[] cmdBytes = checkAndAddLeIfMissing(cmd);
        Log.command(Util.prettyPrintHex(cmdBytes));
        long startTime = System.nanoTime();
        CardResponse response = terminal.transmit(cmdBytes);

        //handle procedure bytes here, and not in the lower level TerminalProvider Implementations.
        //That way we can process procedure bytes from any Provider (if they are not handled at that level)

        byte sw1 = (byte) response.getSW1();
        byte sw2 = (byte) response.getSW2();
        byte[] data = response.getData(); //Copy
        Log.debug("Received data+SW1+SW2: " + Util.byteArrayToHexString(data) + " " + Util.byte2Hex(sw1) + " " + Util.byte2Hex((byte) sw2));
        Log.debug("data.length: 0x"+Util.int2Hex(data.length) + " ("+data.length+")");
        if (sw1 == (byte) 0x6c) { //"Wrong length" (resend last command with correct length)
            //Re-issue command with correct length
            cmdBytes[4] = sw2;
            Log.procedureByte("Received procedure byte SW1=0x6c. Re-issuing command with correct length (" + Util.byte2Hex(sw2)+"): "+ Util.byteArrayToHexString(cmdBytes));
            response = terminal.transmit(cmdBytes);
            sw1 = (byte) response.getSW1();
            sw2 = (byte) response.getSW2();
            data = response.getData(); //Copy
            Log.procedureByte("Received data+SW1+SW2: " + Util.byteArrayToHexString(data) + " " + Util.byte2Hex(sw1) + " " + Util.byte2Hex(sw2));
        }

        //Note some non-EMV cards (and terminal software) seem to re-issue the last command with length=SW2 when getting SW1=61
        while (sw1 == (byte) 0x61) { //Procedure byte: send GET RESPONSE to receive more data
            boolean emvMode = true;
            if(emvMode){
                //this command is EMV specific, since EMV locks CLA to 0x00 only (Book 1, 9.3.1.3). ISO7816-4 specifies CLS in GET RESPONSE in "section 5.4.1 Class byte" to be 0x0X
                cmdBytes = new byte[]{(byte) 0x00, (byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) sw2};
            }else{
                cmdBytes = new byte[]{cmdBytes[0], (byte) 0xC0, (byte) 0x00, (byte) 0x00, (byte) sw2};
            }
            Log.procedureByte("Received procedure byte SW1=0x61. Sending GET RESPONSE command: " + Util.byteArrayToHexString(cmdBytes));
            response = terminal.transmit(cmdBytes);
            byte[] newData = response.getData();
            byte[] tmpData = new byte[data.length + newData.length];
            System.arraycopy(data, 0, tmpData, 0, data.length);
            System.arraycopy(newData, 0, tmpData, data.length, newData.length);
            sw1 = (byte) response.getSW1();
            sw2 = (byte) response.getSW2();
            Log.procedureByte("Received newData+SW1+SW2: " + Util.byteArrayToHexString(newData) + " " + Util.byte2Hex(sw1) + " " + Util.byte2Hex(sw2));
            data = tmpData;
        }


        long endTime = System.nanoTime();
        printResponse(response, doParseTLVData);
        Log.debug("Time: " + Util.getFormattedNanoTime(endTime - startTime));
        return response;
    }

    public static void printResponse(CardResponse response, boolean doParseTLVData) {
        printResponse(response.getData(), response.getSW1(), response.getSW2(), response.getSW(), doParseTLVData);
    }

    //TODO support extended length
    public static byte[] checkAndAddLeIfMissing(byte[] cmd) {
        if(cmd == null) {
            throw new IllegalArgumentException("Cmd cannot be null");
        }
        if(cmd.length < 4) {
            throw new IllegalArgumentException("Cmd length must be >= 4, but was "+cmd.length);
        }
        if(cmd.length == 4) {
            //Add Le
            byte[] cmdWithLe = new byte[5];
            System.arraycopy(cmd, 0, cmdWithLe, 0, cmd.length);
            cmdWithLe[4] = 0x00;
            return cmdWithLe;
        }
        if(cmd.length > 5){
            int lc = Util.byteToInt(cmd[4]);
            if(lc < cmd.length-6 //Lc is less than payload(with Le) length 
                    || lc > cmd.length-5 //Lc is larger than payload(no Le) length
                    ) {
                throw new IllegalArgumentException("Lc was "+lc+", but payload length was "+(cmd.length-5) + " (Le presence unknown)");
            }
            if(lc == cmd.length-5) {
                //Add Le
                byte[] cmdWithLe = new byte[cmd.length+1];
                System.arraycopy(cmd, 0, cmdWithLe, 0, cmd.length);
                cmdWithLe[cmdWithLe.length-1] = 0x00;
                return cmdWithLe;
            }
        }
        
        //Le already present
        return cmd;
    }

    public static void printResponse(byte[] dataAndSw, boolean doParseTLVData){
        byte[] tmp = new byte[dataAndSw.length-2];
        System.arraycopy(dataAndSw, 0, tmp, 0, tmp.length);
        byte sw1 = dataAndSw[dataAndSw.length-2];
        byte sw2 = dataAndSw[dataAndSw.length-1];
        short sw = Util.byte2Short(sw1, sw2);
        printResponse(tmp, sw1, sw2, sw, doParseTLVData);
    }

    public static void printResponse(byte[] data, byte sw1, byte sw2, short sw, boolean doParseTLVData) {
        Log.info("response hex    :\n" + Util.prettyPrintHex(data));

        String swDescription = "";
        String tmp = SW.getSWDescription(sw);
        if (tmp != null && tmp.trim().length() > 0) {
            swDescription = " (" + tmp + ")";
        }
        Log.info("response SW1SW2 : " + Util.byte2Hex(sw1) + " " + Util.byte2Hex(sw2) + swDescription);
        Log.info("response ascii  : " + Util.getSafePrintChars(data));
        if (doParseTLVData) {
            try{
                Log.info("response parsed :\n" + TLVUtil.prettyPrintAPDUResponse(data));
            }catch(TLVException ex){
                Log.debug(ex.getMessage()); //Util.getStackTrace(ex)
            }
        }
    }

    //TODO split PSE/PPSE?
    
    //PPSE (Book B):
    //If the Kernel Identifier (Tag '9F2A') is absent in the Directory Entry,
    //then Entry Point shall use a default value for the
    //Requested Kernel ID, based on the matching AID, as indicated
    //
    //American Express AID kernel 4
    //MasterCard AID kernel 2
    //Visa AID kernel 3
    //Other 0
    public static DDF parseFCIDDF(byte[] data, SmartCard card) {

        DDF ddf = new DDF();

        BERTLV tlv = TLVUtil.getNextTLV(new ByteArrayInputStream(data));

        if (tlv.getTag().equals(EMVTags.FCI_TEMPLATE)) {
            ByteArrayInputStream templateStream = tlv.getValueStream();

            while (templateStream.available() >= 2) {
                tlv = TLVUtil.getNextTLV(templateStream);
                if (tlv.getTag().equals(EMVTags.DEDICATED_FILE_NAME)) {
                    ddf.setName(tlv.getValueBytes());
                } else if (tlv.getTag().equals(EMVTags.FCI_PROPRIETARY_TEMPLATE)) {
                    ByteArrayInputStream bis2 = new ByteArrayInputStream(tlv.getValueBytes());
                    int totalLen = bis2.available();
                    int templateLen = tlv.getLength();
                    while (bis2.available() > (totalLen - templateLen)) {
                        tlv = TLVUtil.getNextTLV(bis2);

                        if (tlv.getTag().equals(EMVTags.SFI)) {
                            ShortFileIdentifier sfi = new ShortFileIdentifier(Util.byteArrayToInt(tlv.getValueBytes()));
                            ddf.setSFI(sfi);
                        } else if (tlv.getTag().equals(EMVTags.LANGUAGE_PREFERENCE)) {
                            LanguagePreference languagePreference = new LanguagePreference(tlv.getValueBytes());
                            ddf.setLanguagePreference(languagePreference);
                        } else if (tlv.getTag().equals(EMVTags.ISSUER_CODE_TABLE_INDEX)) {
                            int index = Util.byteArrayToInt(tlv.getValueBytes());
                            ddf.setIssuerCodeTableIndex(index);
                        } else if (tlv.getTag().equals(EMVTags.APPLICATION_LABEL)) {
							//TODO is this tag expected at this point? Should be located in APP_TEMPLATE! Are there any info in book 1?
						    String label = Util.getSafePrintChars(tlv.getValueBytes());
                            //ddf.setApplicationLabel(label);
                        } else if (tlv.getTag().equals(EMVTags.FCI_ISSUER_DISCRETIONARY_DATA)) { //PPSE
                            ByteArrayInputStream discrStream = new ByteArrayInputStream(tlv.getValueBytes());
                            int total3Len = discrStream.available();
                            int template3Len = tlv.getLength();
                            while (discrStream.available() > (total3Len - template3Len)) {
                                tlv = TLVUtil.getNextTLV(discrStream);

                                if (tlv.getTag().equals(EMVTags.APPLICATION_TEMPLATE)) {
                                    ByteArrayInputStream appTemplateStream = new ByteArrayInputStream(tlv.getValueBytes());
                                    int appTemplateTotalLen = appTemplateStream.available();
                                    int template4Len = tlv.getLength();
                                    EMVApplication app = new EMVApplication();
                                    while (appTemplateStream.available() > (appTemplateTotalLen - template4Len)) {
                                        tlv = TLVUtil.getNextTLV(appTemplateStream);

                                        if (tlv.getTag().equals(EMVTags.AID_CARD)) {
                                            app.setAID(new AID(tlv.getValueBytes()));
                                        } else if (tlv.getTag().equals(EMVTags.APPLICATION_LABEL)) {
                                            String label = Util.getSafePrintChars(tlv.getValueBytes()); //Use only safe print chars, just in case
                                            app.setLabel(label);
                                        } else if (tlv.getTag().equals(EMVTags.APPLICATION_PRIORITY_INDICATOR)) {
                                            ApplicationPriorityIndicator api = new ApplicationPriorityIndicator(tlv.getValueBytes()[0]);
                                            app.setApplicationPriorityIndicator(api);
                                        } else {
                                            //TODO call ddf instead of card?
                                            card.addUnhandledRecord(tlv);
                                        }
                                    }
                                    //Verify that the app template is valid
                                    if(app.getAID() != null){
                                        card.addEMVApplication(app);
                                    }else{
                                        Log.debug("Found invalid application template: "+app.toString());
                                    }
                                } else {
                                    //TODO call ddf instead of card?
                                    card.addUnhandledRecord(tlv);
                                }
                            }
                        } else {
                            //TODO call ddf instead of card?
                            card.addUnhandledRecord(tlv);
                        }
                    }
                } else {
                    //TODO call ddf instead of card?
                    card.addUnhandledRecord(tlv);
                }
            }
        } else {
            //TODO call ddf instead of card?
            card.addUnhandledRecord(tlv);
        }

        return ddf;
    }

    public static void parsePSERecord(byte[] data, SmartCard card) {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);

        while (bis.available() >= 2) {
            BERTLV tlv = TLVUtil.getNextTLV(bis);
            if (tlv.getTag().equals(EMVTags.RECORD_TEMPLATE)) {
                ByteArrayInputStream valueBytesBis = new ByteArrayInputStream(tlv.getValueBytes());
                while (valueBytesBis.available() >= 2) {
                    tlv = TLVUtil.getNextTLV(valueBytesBis);
                    if (tlv.getTag().equals(EMVTags.APPLICATION_TEMPLATE)) { //Application Template
                        ByteArrayInputStream bis2 = new ByteArrayInputStream(tlv.getValueBytes());
                        int totalLen = bis2.available();
                        int templateLen = tlv.getLength();
                        EMVApplication app = new EMVApplication();
                        while (bis2.available() > (totalLen - templateLen)) {

                            tlv = TLVUtil.getNextTLV(bis2);

                            if (tlv.getTag().equals(EMVTags.AID_CARD)) {
                                app.setAID(new AID(tlv.getValueBytes()));
                            } else if (tlv.getTag().equals(EMVTags.APPLICATION_LABEL)) {
                                String label = Util.getSafePrintChars(tlv.getValueBytes()); //Use only safe print chars, just in case
                                app.setLabel(label);
                            } else if (tlv.getTag().equals(EMVTags.APP_PREFERRED_NAME)) {
                                String preferredName = Util.getSafePrintChars(tlv.getValueBytes()); //Use only safe print chars, just in case
                                app.setPreferredName(preferredName);
                            } else if (tlv.getTag().equals(EMVTags.APPLICATION_PRIORITY_INDICATOR)) {
                                ApplicationPriorityIndicator api = new ApplicationPriorityIndicator(tlv.getValueBytes()[0]);
                                app.setApplicationPriorityIndicator(api);
                            } else if (tlv.getTag().equals(EMVTags.ISSUER_CODE_TABLE_INDEX)) {
                                int index = Util.byteArrayToInt(tlv.getValueBytes());
                                app.setIssuerCodeTableIndex(index);
                            } else if (tlv.getTag().equals(EMVTags.LANGUAGE_PREFERENCE)) {
                                LanguagePreference languagePreference = new LanguagePreference(tlv.getValueBytes());
                                app.setLanguagePreference(languagePreference);
                            } else {
                                checkForProprietaryTagOrAddToUnhandled(app, tlv);
                            }
                        }
                        Log.debug("Adding application: " + Util.prettyPrintHexNoWrap(app.getAID().getAIDBytes()));
//                        apps.add(app);
                        //Verify that the app template is valid
                        if(app.getAID() != null){
                            card.addEMVApplication(app);
                        }else{
                            Log.debug("Found invalid application template: "+app.toString());
                        }
                    } else {
                        card.addUnhandledRecord(tlv);
                    }
                }

            } else if (tlv.getTag().equals(EMVTags.RESPONSE_MESSAGE_TEMPLATE_2)) {
                card.addUnhandledRecord(tlv);
            }
        }
//        return apps;
    }

    public static void parseFCIADF(byte[] data, EMVApplication app) {

        if(data == null || data.length < 2){
            return;
        }

        BERTLV tlv = TLVUtil.getNextTLV(new ByteArrayInputStream(data));

        if (tlv.getTag().equals(EMVTags.FCI_TEMPLATE)) {
            ByteArrayInputStream templateStream = tlv.getValueStream();
            while (templateStream.available() >= 2) {


                tlv = TLVUtil.getNextTLV(templateStream);
                if (tlv.getTag().equals(EMVTags.DEDICATED_FILE_NAME)) {
                    app.setAID(new AID(tlv.getValueBytes()));
                    Log.debug("ADDED AID to app. AID after set: "+Util.prettyPrintHexNoWrap(app.getAID().getAIDBytes()) + " - AID in FCI: " + Util.prettyPrintHexNoWrap(tlv.getValueBytes()));
                } else if (tlv.getTag().equals(EMVTags.FCI_PROPRIETARY_TEMPLATE)) { //Proprietary Information Template
                    ByteArrayInputStream bis2 = tlv.getValueStream();
                    int totalLen = bis2.available();
                    int templateLen = tlv.getLength();
                    while (bis2.available() > (totalLen - templateLen)) {
                        tlv = TLVUtil.getNextTLV(bis2);

                        if (tlv.getTag().equals(EMVTags.APPLICATION_LABEL)) {
                            app.setLabel(Util.getSafePrintChars(tlv.getValueBytes()));
                        } else if (tlv.getTag().equals(EMVTags.PDOL)) {
                            app.setPDOL(new DOL(DOL.Type.PDOL, tlv.getValueBytes()));
                        } else if (tlv.getTag().equals(EMVTags.LANGUAGE_PREFERENCE)) {
                            LanguagePreference languagePreference = new LanguagePreference(tlv.getValueBytes());
                            app.setLanguagePreference(languagePreference);
                        } else if (tlv.getTag().equals(EMVTags.APP_PREFERRED_NAME)) {
                            //TODO: "Use Issuer Code Table Index"
                            String preferredName = Util.getSafePrintChars(tlv.getValueBytes()); //Use only safe print chars, just in case
                            app.setPreferredName(preferredName);
                        } else if (tlv.getTag().equals(EMVTags.ISSUER_CODE_TABLE_INDEX)) {
                            int index = Util.byteArrayToInt(tlv.getValueBytes());
                            app.setIssuerCodeTableIndex(index);
                        } else if (tlv.getTag().equals(EMVTags.APPLICATION_PRIORITY_INDICATOR)) {
                            ApplicationPriorityIndicator api = new ApplicationPriorityIndicator(tlv.getValueBytes()[0]);
                            app.setApplicationPriorityIndicator(api);
                        } else if (tlv.getTag().equals(EMVTags.FCI_ISSUER_DISCRETIONARY_DATA)) { // File Control Information (FCI) Issuer Discretionary Data
                            ByteArrayInputStream bis3 = tlv.getValueStream();
                            int totalLenFCIDiscretionary = bis3.available();
                            int tlvLen = tlv.getLength();
                            while (bis3.available() > (totalLenFCIDiscretionary - tlvLen)) {
                                tlv = TLVUtil.getNextTLV(bis3);
                                if (tlv.getTag().equals(EMVTags.LOG_ENTRY)) {
                                    app.setLogEntry(new LogEntry(tlv.getValueBytes()[0], tlv.getValueBytes()[1]));
							    } else if (tlv.getTag().equals(VISATags.VISA_LOG_ENTRY)) { //TODO add this to VISAApp
							    	//app.setVisaLogEntry(new LogEntry(tlv.getValueBytes()[0], tlv.getValueBytes()[1]));
                                } else if (tlv.getTag().equals(EMVTags.ISSUER_URL)) {
                                    app.setIssuerUrl(Util.getSafePrintChars(tlv.getValueBytes()));
                                } else if (tlv.getTag().equals(EMVTags.ISSUER_IDENTIFICATION_NUMBER)) {
                                    IssuerIdentificationNumber iin = new IssuerIdentificationNumber(tlv.getValueBytes());
                                    app.setIssuerIdentificationNumber(iin);
                                } else if (tlv.getTag().equals(EMVTags.ISSUER_COUNTRY_CODE_ALPHA3)) {
                                    app.setIssuerCountryCodeAlpha3(Util.getSafePrintChars(tlv.getValueBytes()));
                                } else {
                                    checkForProprietaryTagOrAddToUnhandled(app, tlv);
                                }
                            }
                        } else {
                            checkForProprietaryTagOrAddToUnhandled(app, tlv);
                        }

                    }
                }
            }

        } else {
            checkForProprietaryTagOrAddToUnhandled(app, tlv);
            throw new SmartCardException("Error parsing ADF. Expected FCI Template. Data: " + Util.byteArrayToHexString(data));
        }
    }
    
    private static void checkForProprietaryTagOrAddToUnhandled(EMVApplication app, BERTLV tlv) {
        Tag tagFound = EMVTags.get(app, tlv.getTag());
        if(tagFound != null) {
            app.addUnprocessedRecord(tlv);
        } else {
            app.addUnknownRecord(tlv);
        }
    }

    public static void parseProcessingOpts(byte[] data, EMVApplication app) {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);

        if (bis.available() < 2) {
            throw new SmartCardException("Error parsing Processing Options. Invalid TLV Length. Data: " + Util.byteArrayToHexString(data));
        }
        BERTLV tlv = TLVUtil.getNextTLV(bis);

        ByteArrayInputStream valueBytesBis = tlv.getValueStream();

        if (valueBytesBis.available() < 2) {
            throw new SmartCardException("Error parsing Processing Options: Invalid ValueBytes length: " + valueBytesBis.available());
        }

        if (tlv.getTag().equals(EMVTags.RESPONSE_MESSAGE_TEMPLATE_1)) {
            //AIP & AFL concatenated without delimiters (that is, excluding tag and length)
            ApplicationInterchangeProfile aip = new ApplicationInterchangeProfile((byte) valueBytesBis.read(), (byte) valueBytesBis.read());
            app.setApplicationInterchangeProfile(aip);

            if (valueBytesBis.available() % 4 != 0) {
                throw new SmartCardException("Error parsing Processing Options: Invalid AFL length: " + valueBytesBis.available());
            }

            byte[] aflBytes = new byte[valueBytesBis.available()];
            valueBytesBis.read(aflBytes, 0, aflBytes.length);

            ApplicationFileLocator afl = new ApplicationFileLocator(aflBytes);
            app.setApplicationFileLocator(afl);
        } else if (tlv.getTag().equals(EMVTags.RESPONSE_MESSAGE_TEMPLATE_2)) {
            //AIP (& AFL) WITH delimiters (that is, including, including tag and length) and possibly other BER TLV tags (that might be proprietary)
            while (valueBytesBis.available() >= 2) {
                tlv = TLVUtil.getNextTLV(valueBytesBis);
                
//   Example:
//                77 4e -- Response Message Template Format 2
//                    82 02 -- Application Interchange Profile
//                          00 00 (BINARY)
//                    9f 36 02 -- Application Transaction Counter (ATC)
//                             00 01 (BINARY)
//                    57 13 -- Track 2 Equivalent Data
//                          40 23 60 09 00 12 50 08 d1 80 52 21 15 15 29 93
//                          00 00 0f (BINARY)
//                    9f 10 07 -- Issuer Application Data
//                             06 0a 0a 03 a0 00 00 (BINARY)
//                    9f 26 08 -- Application Cryptogram
//                             4e 29 20 46 bf 43 38 51 (BINARY)
//                    5f 34 01 -- Application Primary Account Number (PAN) Sequence Number
//                             01 (NUMERIC)
//                    9f 6c 02 -- [UNHANDLED TAG]
//                             30 00 (BINARY)
//                    5f 20 0f -- Cardholder Name
//                             56 49 53 41 20 43 41 52 44 48 4f 4c 44 45 52 (=VISA CARDHOLDER)
                
                if (tlv.getTag().equals(EMVTags.APPLICATION_INTERCHANGE_PROFILE)) {
                    byte[] aipBytes = tlv.getValueBytes();
                    ApplicationInterchangeProfile aip = new ApplicationInterchangeProfile(aipBytes[0], aipBytes[1]);
                    app.setApplicationInterchangeProfile(aip);
                } else if (tlv.getTag().equals(EMVTags.APPLICATION_FILE_LOCATOR)) {
                    byte[] aflBytes = tlv.getValueBytes();
                    ApplicationFileLocator afl = new ApplicationFileLocator(aflBytes);
                    app.setApplicationFileLocator(afl);
                } else {
                    checkForProprietaryTagOrAddToUnhandled(app, tlv);
                }
            }
        } else {
            checkForProprietaryTagOrAddToUnhandled(app, tlv);
        }
    }

    public static void parseAppRecord(byte[] data, EMVApplication app) {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);

        if (bis.available() < 2) {
            throw new SmartCardException("Error parsing Application Record. Data: " + Util.byteArrayToHexString(data));
        }
        BERTLV tlv = TLVUtil.getNextTLV(bis);

        if (!tlv.getTag().equals(EMVTags.RECORD_TEMPLATE)) {
            throw new SmartCardException("Error parsing Application Record: No Response Template found. Data=" + Util.byteArrayToHexString(tlv.getValueBytes()));
        }

        bis = new ByteArrayInputStream(tlv.getValueBytes());

        while (bis.available() >= 2) {
            tlv = TLVUtil.getNextTLV(bis);
            if (tlv.getTag().equals(EMVTags.CARDHOLDER_NAME)) {
                app.setCardholderName(Util.getSafePrintChars(tlv.getValueBytes()));
            } else if (tlv.getTag().equals(EMVTags.TRACK1_DISCRETIONARY_DATA)) {
                app.setTrack1DiscretionaryData(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.TRACK2_DISCRETIONARY_DATA)) {
                app.setTrack2DiscretionaryData(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.TRACK_2_EQV_DATA)) {
                Track2EquivalentData t2Data = new Track2EquivalentData(tlv.getValueBytes());
                app.setTrack2EquivalentData(t2Data);
            } else if (tlv.getTag().equals(EMVTags.APP_EXPIRATION_DATE)) {
                app.setExpirationDate(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.APP_EFFECTIVE_DATE)) {
                app.setEffectiveDate(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.PAN)) {
                PAN pan = new PAN(tlv.getValueBytes());
                app.setPAN(pan);
            } else if (tlv.getTag().equals(EMVTags.PAN_SEQUENCE_NUMBER)) {
                app.setPANSequenceNumber(tlv.getValueBytes()[0]);
            } else if (tlv.getTag().equals(EMVTags.APP_USAGE_CONTROL)) {
                ApplicationUsageControl auc = new ApplicationUsageControl(tlv.getValueBytes()[0], tlv.getValueBytes()[1]);
                app.setApplicationUsageControl(auc);
            } else if (tlv.getTag().equals(EMVTags.CVM_LIST)) {
                CVMList cvmList = new CVMList(tlv.getValueBytes());
                app.setCVMList(cvmList);
            } else if (tlv.getTag().equals(EMVTags.LANGUAGE_PREFERENCE)) {
                LanguagePreference languagePreference = new LanguagePreference(tlv.getValueBytes());
                app.setLanguagePreference(languagePreference);
            } else if (tlv.getTag().equals(EMVTags.ISSUER_ACTION_CODE_DEFAULT)) {
                app.setIssuerActionCodeDefault(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ISSUER_ACTION_CODE_DENIAL)) {
                app.setIssuerActionCodeDenial(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ISSUER_ACTION_CODE_ONLINE)) {
                app.setIssuerActionCodeOnline(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ISSUER_COUNTRY_CODE)) {
                int issuerCountryCode = Util.binaryHexCodedDecimalToInt(Util.byteArrayToHexString(tlv.getValueBytes()));
                app.setIssuerCountryCode(issuerCountryCode);
            } else if (tlv.getTag().equals(EMVTags.APPLICATION_CURRENCY_CODE)) {
                int currencyCode = Util.binaryHexCodedDecimalToInt(Util.byteArrayToHexString(tlv.getValueBytes()));
                app.setApplicationCurrencyCode(currencyCode);
            } else if (tlv.getTag().equals(EMVTags.APP_CURRENCY_EXPONENT)) {
                int applicationCurrencyExponent = Util.binaryHexCodedDecimalToInt(Util.byteArrayToHexString(tlv.getValueBytes()));
                app.setApplicationCurrencyExponent(applicationCurrencyExponent);
            } else if (tlv.getTag().equals(EMVTags.APP_VERSION_NUMBER_CARD)) {
                app.setApplicationVersionNumber(Util.byteArrayToInt(tlv.getValueBytes()));
            } else if (tlv.getTag().equals(EMVTags.CDOL1)) {
                DOL cdol1 = new DOL(DOL.Type.CDOL1, tlv.getValueBytes());
                app.setCDOL1(cdol1);
            } else if (tlv.getTag().equals(EMVTags.CDOL2)) {
                DOL cdol2 = new DOL(DOL.Type.CDOL2, tlv.getValueBytes());
                app.setCDOL2(cdol2);
            } else if (tlv.getTag().equals(EMVTags.LOWER_CONSEC_OFFLINE_LIMIT)) {
                app.setLowerConsecutiveOfflineLimit(Util.byteArrayToInt(tlv.getValueBytes()));
            } else if (tlv.getTag().equals(EMVTags.UPPER_CONSEC_OFFLINE_LIMIT)) {
                app.setUpperConsecutiveOfflineLimit(Util.byteArrayToInt(tlv.getValueBytes()));
            } else if (tlv.getTag().equals(EMVTags.SERVICE_CODE)) {
                int serviceCode = Util.binaryHexCodedDecimalToInt(Util.byteArrayToHexString(tlv.getValueBytes()));
                app.setServiceCode(serviceCode);
            } else if (tlv.getTag().equals(EMVTags.SDA_TAG_LIST)) {
                StaticDataAuthenticationTagList staticDataAuthTagList = new StaticDataAuthenticationTagList(tlv.getValueBytes());
                app.setStaticDataAuthenticationTagList(staticDataAuthTagList);
            } else if (tlv.getTag().equals(EMVTags.CA_PUBLIC_KEY_INDEX_CARD)) {
                IssuerPublicKeyCertificate issuerCert = app.getIssuerPublicKeyCertificate();
                if (issuerCert == null) {
                    CA ca = CA.getCA(app.getAID());

                    if (ca == null) {
                        //ca == null is permitted (we might not have the CA public keys for every exotic CA)
                        Log.info("No CA configured for AID: " + app.getAID().toString());
//                        throw new SmartCardException("No CA configured for AID: "+app.getAID().toString());
                    }
                    issuerCert = new IssuerPublicKeyCertificate(ca);
                    app.setIssuerPublicKeyCertificate(issuerCert);
                }
                issuerCert.setCAPublicKeyIndex(Util.byteArrayToInt(tlv.getValueBytes()));
            } else if (tlv.getTag().equals(EMVTags.ISSUER_PUBLIC_KEY_CERT)) {
                IssuerPublicKeyCertificate issuerCert = app.getIssuerPublicKeyCertificate();
                if (issuerCert == null) {
                    issuerCert = new IssuerPublicKeyCertificate(CA.getCA(app.getAID()));
                    app.setIssuerPublicKeyCertificate(issuerCert);
                }
                issuerCert.setSignedBytes(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ISSUER_PUBLIC_KEY_EXP)) {
                IssuerPublicKeyCertificate issuerCert = app.getIssuerPublicKeyCertificate();
                if (issuerCert == null) {
                    issuerCert = new IssuerPublicKeyCertificate(CA.getCA(app.getAID()));
                    app.setIssuerPublicKeyCertificate(issuerCert);
                }
                issuerCert.getIssuerPublicKey().setExponent(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ISSUER_PUBLIC_KEY_REMAINDER)) {
                IssuerPublicKeyCertificate issuerCert = app.getIssuerPublicKeyCertificate();
                if (issuerCert == null) {
                    issuerCert = new IssuerPublicKeyCertificate(CA.getCA(app.getAID()));
                    app.setIssuerPublicKeyCertificate(issuerCert);
                }
                issuerCert.getIssuerPublicKey().setRemainder(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.SIGNED_STATIC_APP_DATA)) {
                SignedStaticApplicationData ssad = app.getSignedStaticApplicationData();
                if (ssad == null) {
                    ssad = new SignedStaticApplicationData(app);
                    app.setSignedStaticApplicationData(ssad);
                }
                ssad.setSignedBytes(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ICC_PUBLIC_KEY_CERT)) {
                ICCPublicKeyCertificate iccCert = app.getICCPublicKeyCertificate();
                if (iccCert == null) {
                    iccCert = new ICCPublicKeyCertificate(app, app.getIssuerPublicKeyCertificate());
                    app.setICCPublicKeyCertificate(iccCert);
                }
                iccCert.setSignedBytes(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ICC_PUBLIC_KEY_EXP)) {
                ICCPublicKeyCertificate iccCert = app.getICCPublicKeyCertificate();
                if (iccCert == null) {
                    iccCert = new ICCPublicKeyCertificate(app, app.getIssuerPublicKeyCertificate());
                    app.setICCPublicKeyCertificate(iccCert);
                }
                iccCert.getICCPublicKey().setExponent(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ICC_PUBLIC_KEY_REMAINDER)) {
                ICCPublicKeyCertificate iccCert = app.getICCPublicKeyCertificate();
                if (iccCert == null) {
                    iccCert = new ICCPublicKeyCertificate(app, app.getIssuerPublicKeyCertificate());
                    app.setICCPublicKeyCertificate(iccCert);
                }
                iccCert.getICCPublicKey().setRemainder(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ICC_PIN_ENCIPHERMENT_PUBLIC_KEY_CERT)) {
                ICCPinEnciphermentPublicKeyCertificate iccPinEnciphermentCert = app.getICCPinEnciphermentPublicKeyCertificate();
                if (iccPinEnciphermentCert == null) {
                    iccPinEnciphermentCert = new ICCPinEnciphermentPublicKeyCertificate(app, app.getIssuerPublicKeyCertificate());
                    app.setICCPinEnciphermentPublicKeyCertificate(iccPinEnciphermentCert);
                }
                iccPinEnciphermentCert.setSignedBytes(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ICC_PIN_ENCIPHERMENT_PUBLIC_KEY_EXP)) {
                ICCPinEnciphermentPublicKeyCertificate iccPinEnciphermentCert = app.getICCPinEnciphermentPublicKeyCertificate();
                if (iccPinEnciphermentCert == null) {
                    iccPinEnciphermentCert = new ICCPinEnciphermentPublicKeyCertificate(app, app.getIssuerPublicKeyCertificate());
                    app.setICCPinEnciphermentPublicKeyCertificate(iccPinEnciphermentCert);
                }
                iccPinEnciphermentCert.getICCPublicKey().setExponent(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.ICC_PIN_ENCIPHERMENT_PUBLIC_KEY_REM)) {
                ICCPinEnciphermentPublicKeyCertificate iccPinEnciphermentCert = app.getICCPinEnciphermentPublicKeyCertificate();
                if (iccPinEnciphermentCert == null) {
                    iccPinEnciphermentCert = new ICCPinEnciphermentPublicKeyCertificate(app, app.getIssuerPublicKeyCertificate());
                    app.setICCPinEnciphermentPublicKeyCertificate(iccPinEnciphermentCert);
                }
                iccPinEnciphermentCert.getICCPublicKey().setRemainder(tlv.getValueBytes());
            } else if (tlv.getTag().equals(EMVTags.DDOL)) {
                DOL ddol = new DOL(DOL.Type.DDOL, tlv.getValueBytes());
                app.setDDOL(ddol);
            } else if (tlv.getTag().equals(EMVTags.IBAN)) {
                app.setIBAN(new IBAN(tlv.getValueBytes()));
            } else if (tlv.getTag().equals(EMVTags.BANK_IDENTIFIER_CODE)) {
                app.setBIC(new BankIdentifierCode(tlv.getValueBytes()));
            } else if (tlv.getTag().equals(EMVTags.APP_DISCRETIONARY_DATA)) {
                app.setDiscretionaryData(tlv.getValueBytes());
            } else {
                checkForProprietaryTagOrAddToUnhandled(app, tlv);
            }

        }
    }

    public static void processInternalAuthResponse(byte[] data, byte[] authenticationRelatedData, EMVApplication app) {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);

        if (bis.available() < 2) {
            throw new SmartCardException("Error parsing Internal Auth Response. Invalid TLV Length. Data: " + Util.byteArrayToHexString(data));
        }
        BERTLV tlv = TLVUtil.getNextTLV(bis);

        ByteArrayInputStream valueBytesBis = tlv.getValueStream();

        if (valueBytesBis.available() < 2) {
            throw new SmartCardException("Error parsing Internal Auth Response: Invalid ValueBytes length: " + valueBytesBis.available());
        }

        if (tlv.getTag().equals(EMVTags.RESPONSE_MESSAGE_TEMPLATE_1)) {
            if (!app.getIssuerPublicKeyCertificate().validate() || !app.getICCPublicKeyCertificate().validate()) {
                EMVTerminal.getTerminalVerificationResults().setDDAFailed(true);
                return;
            }
            try {
                SignedDynamicApplicationData sdad = SignedDynamicApplicationData.parseSignedData(tlv.getValueBytes(), app.getICCPublicKeyCertificate().getICCPublicKey(), authenticationRelatedData);
                app.setSignedDynamicApplicationData(sdad);
            } catch (SignedDataException ex) {
                Log.debug(ex.getMessage());
                EMVTerminal.getTerminalVerificationResults().setDDAFailed(true);
            }
        } else if (tlv.getTag().equals(EMVTags.RESPONSE_MESSAGE_TEMPLATE_2)) {
            //AIP & AFL WITH delimiters (that is, including, including tag and length) and possibly other BER TLV tags (that might be proprietary)
            while (valueBytesBis.available() >= 2) {
                tlv = TLVUtil.getNextTLV(valueBytesBis);
                if (tlv.getTag().equals(EMVTags.SIGNED_DYNAMIC_APPLICATION_DATA)) {
                    try {
                        SignedDynamicApplicationData sdad = SignedDynamicApplicationData.parseSignedData(tlv.getValueBytes(), app.getICCPublicKeyCertificate().getICCPublicKey(), authenticationRelatedData);
                        app.setSignedDynamicApplicationData(sdad);
                        app.getTransactionStatusInformation().setOfflineDataAuthenticationWasPerformed(true); //TODO
                    } catch (SignedDataException ex) {
                        Log.debug(ex.getMessage());
                        EMVTerminal.getTerminalVerificationResults().setDDAFailed(true);
                    }
                } else {
                    checkForProprietaryTagOrAddToUnhandled(app, tlv);
                }
            }
        } else {
            checkForProprietaryTagOrAddToUnhandled(app, tlv);
        }

    }

    public static void main(String[] args) {

        System.out.println(TLVUtil.readTagLength(new ByteArrayInputStream(new byte[]{(byte)0x81, (byte)0x97})));

//        EMVApplication app = new EMVApplication();
//        parseFCIADF(Util.fromHexString("6f198407a0000000038002a50e5009564953412041757468870101"), app);
//
//        String gpoResponse = "77 0e 82 02 38 00 94 08 08 01 03 01 10 01 01 00";
//
//        parseProcessingOpts(Util.fromHexString(gpoResponse), app);
//
//        System.out.println(app.getApplicationFileLocator().toString());
//        System.out.println(app.getApplicationInterchangeProfile().toString());

//        System.out.println(EMVUtil.prettyPrintAPDUResponse(Util.fromHexString("6f 20 81 02 00 00 82 01 38 83 02 3f 00 84 06 00 00 00 00 00 00 85 01 00 8c 08 1f a1 a1 a1 a1 a1 88 a1")));

//        System.out.println(EMVUtil.prettyPrintAPDUResponse(Util.fromHexString("6F388407 A0000000 031010A5 2D500B56 69736144 616E6B6F 72748701 015F2D08 6461656E 6E6F7376 9F110101 9F120B56 69736144 616E6B6F 7274")));

        
    }

    private static void printCmdHdr(String hex){

            Log.info("\n"+COMMAND_HEADER_FRAMING
                + "\n[CMD] " + hex
                + "\n"+COMMAND_HEADER_FRAMING);
    }
}
