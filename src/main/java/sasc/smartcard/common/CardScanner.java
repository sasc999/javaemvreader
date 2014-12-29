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
package sasc.smartcard.common;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import sasc.smartcard.app.conax.ConaxSession;
import sasc.smartcard.app.yubikey.Yubikey;
import sasc.emv.EMVAPDUCommands;
import sasc.emv.EMVApplication;
import sasc.emv.EMVUtil;
import sasc.emv.SW;
import sasc.iso7816.AID;
import sasc.iso7816.BERTLV;
import sasc.iso7816.Iso7816Commands;
import sasc.iso7816.MasterFile;
import sasc.iso7816.RID;
import sasc.iso7816.SmartCardException;
import sasc.iso7816.TLVException;
import sasc.iso7816.TLVUtil;
import sasc.lookup.RID_DB;
import sasc.smartcard.app.globalplatform.GlobalPlatformDriver;
import sasc.smartcard.app.globalplatform.SecurityDomainFCI;
import sasc.smartcard.app.jcop.JCOPApplication;
import sasc.smartcard.app.usim.USIMHandler;
import sasc.smartcard.pcsc.PCSC;
import sasc.smartcard.pcsc.StorageCardHandler;
import sasc.terminal.CardConnection;
import sasc.terminal.CardResponse;
import sasc.terminal.KnownAIDList;
import sasc.terminal.TerminalException;
import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class CardScanner {

    private SmartCard smartCard;
    private CardConnection terminal;
    private SessionProcessingEnv sessionEnv;

    public CardScanner(SmartCard smartCard, CardConnection terminal, SessionProcessingEnv sessionEnv) {
        this.smartCard = smartCard;
        this.terminal = terminal;
        this.sessionEnv = sessionEnv;
    }

    public SmartCard getCard(){
        return smartCard;
    }

    public void start() throws TerminalException {

        byte[] atr = terminal.getATR();

        int SW1;
        int SW2;
        byte[] command;
        CardResponse response;

        if(sessionEnv.getDiscoverTerminalFeatures()){
            //PC/SC Part 10. Supplement: IFDs with Feature Capabilities
            //v2.02.02
            //Section 2.3.1 GET_FEATURE_REQUEST by Pseudo-APDU

            Log.commandHeader("PC/SC GET FEATURE REQUEST");
            command = Util.fromHexString("FF C2 01 00 00 00");

            response = EMVUtil.sendCmd(terminal, command);

            SW1 = (byte) response.getSW1();
            SW2 = (byte) response.getSW2();

            if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {

            }

            //Try using control command
            try{
                Log.commandHeader("Transmit Control Command to discover the terminal features");
                byte[] ccResponse = terminal.transmitControlCommand(PCSC.CM_IOCTL_GET_FEATURE_REQUEST, new byte[0]);
                if(ccResponse != null){
					Log.info("GET FEATURE REQUEST controlCommandResponse: "+Util.prettyPrintHexNoWrap(ccResponse));
                    //TODO parse response
                    //Ex
                    //12 04 42 33 00 12   13 04 42 00 00 01
                }
            }catch(Exception e){
                Log.debug(e.toString());
            }
        }

        //Register ATR handlers
        //Use Atr handlers for
        //-cards with eg only 1 app (the default selected app)
        //-synchronous cards (storage cards)
        AtrHandler conaxAtrHandler = ConaxSession.getAtrHandler();
        Registry.getInstance().registerAtrHandler(conaxAtrHandler, conaxAtrHandler.getAtrPatterns());

        StorageCardHandler pcscStorageCardHandler = new StorageCardHandler();
        Registry.getInstance().registerAtrHandler(pcscStorageCardHandler, pcscStorageCardHandler.getAtrPatterns());


        //Check if any handlers are registered for the current ATR
        for(AtrHandler atrHandler : Registry.getInstance().getHandlersForAtr(atr)) {
            if(atrHandler.process(smartCard, terminal)) { //Returns true if handle exclusively
            	smartCard.setAllKnownAidsProbed();
                return;
            }
        }

        //Register AID handers
        Yubikey yk = new Yubikey();
        Registry.getInstance().registerAidHandler(yk, Yubikey.NEO_AID);
        GlobalPlatformDriver gpDriver = new GlobalPlatformDriver();
        for(KnownAIDList.KnownAID gpAID : KnownAIDList.getAIDsByType("GP")) {
            Registry.getInstance().registerAidHandler(gpDriver, gpAID.getAID());
        }
        //USIM
        USIMHandler usimHandler = new USIMHandler();
        Registry.getInstance().registerAidHandler(usimHandler, "A0 00 00 00 87"); //3G RID


        //Try to GET DATA from the default selected application?


        //We ALWAYS try to see if there is any GP App(s) present (unless a card (identified by ATR) is registered to be handled exclusively)
        //GP 2.1.1 _allows_ ISD selection using a zero length aid
        //SELECT
        //00 A4 04 00 00
        //GP cards return:
        // 6F File Control Information (FCI) Template
        //    84 Dedicated File (DF) Name
        //       A0000001510000
        //    A5 File Control Information (FCI) Proprietary Template
        //       9F65 Maximum length of data field
        //            FF
        Log.commandHeader("SELECT ISD using zero length AID");
        command = Util.fromHexString("00 A4 04 00 00");

        response = EMVUtil.sendCmd(terminal, command);

        SW1 = (byte) response.getSW1();
        SW2 = (byte) response.getSW2();

        if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
            try{
                SecurityDomainFCI fci = SecurityDomainFCI.parse(response.getData());
                AID isdAid = fci.getSecurityManagerAid();
                if(isdAid != null) {
                    smartCard.addAID(isdAid);
                    Registry.getInstance().registerAidHandler(gpDriver, isdAid);
                }
            } catch(TLVException ex) {
                Log.info(ex.getMessage());
                Log.debug(Util.getStackTrace(ex));
            }
        }

        //Master file is not present on all cards
        if (sessionEnv.getReadMasterFile()) {
            //TODO implement MF data parsing (according to 7816-4:2005)
            Log.commandHeader("SELECT FILE Master File (if available)");

            command = Iso7816Commands.selectMasterFile();

            CardResponse selectMFResponse = EMVUtil.sendCmd(terminal, command);

            SW1 = (byte) selectMFResponse.getSW1();
            SW2 = (byte) selectMFResponse.getSW2();

            if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
                //Example response TODO
                //6f 09
                //      84 07 //DF Name
                //            f0 00 00 00 01 3f 00
                //
                //Another example
                //6f 20
                //      81 02
                //            00 00
                //      82 01
                //            38 //0 - 1 1 1 0 0 0 = DF
                //      83 02
                //            3f 00
                //      84 06
                //            00 00 00 00 00 00
                //      85 01
                //            00
                //      8c 08
                //            1f a1 a1 a1 a1 a1 88 a1

                //3rd example (ATR: 3b 88 80 01 43 44 31 69 a9 00 00 00 ff)
                //6f 20
                //      81 02
                //            00 1a //Number of data bytes in the file
                //      82 01
                //            38 //38=DF
                //      83 02
                //            3f 00
                //      84 06
                //            00 00 00 00 00 00
                //      85 01
                //            00
                //      8c 08
                //            1f a1 a1 a1 a1 a1 88 a1

                //4th example (EMV card w/3 AIDs. 2xVISA ! + 1xBRADESCO)
                //6f 0b
                //      84 09
                //            46 2e 4d 41 45 53 54 52 4f (=MAESTRO)


                //5th example (MASTERCARD :
                //                       CL        3b 8e 80 01 13 78 80 80 02 46 49 4f 4d 4b 5f 30 30 31 4e
                //                       CONTACT   3b ef 00 00 81 31 fe 45 46 49 4f 4d 4b 5f 30 30 31 20 30 31 30 41 00 9c
                //(same response for: 00 a4 00 00 02 3f 00 00)
                //
                //6f 20
                //      81 02 00 1a
                //      82 01 38
                //      83 02 3f 00
                //      84 06 00 00 00 00 00 00
                //      85 01 00
                //      8c 08 1f a1 a1 a1 a1 a1 88 a1


                //6th example (IBM JC testcard from book)
                //Note: Tag 0x63 does not seem to match structure of 'Wrapper' TAG according to 7816-4:2005
                //
                //ATR: 3b ef 00 ff 81 31 66 45 49 42 4d 20 4d 46 43 34 30 30 32 30 38 33 31 a1
                //
                //00 a4 00 00 02 3f 00 00
                //
                //63 0c
                //      1e b4 3f 00 00 00 ff 33 ff 01 01 10
                //90 00 (Success)
                //
                //
                ////EF.DIR
                //00 a4 00 00 02 2f 00
                //
                //63 0d
                //      00 78 2f 00 01 00 03 ff ff 03 02 01 28
                //90 00 (Success)
                //
                //------------------------------------------------
                //[Step 4] Send READ RECORD to read all records in SFI 0
                //------------------------------------------------
                //00 b2 01 04 00
                //
                //61 26
                //      4f 09
                //            d2 76 00 00 22 00 00 00 60
                //      50 10
                //            P  K  C  S  #  1  1     T  o  k  e  n
                //            50 4b 43 53 23 31 31 20 74 6f 6b 65 6e 20 20 20
                //      52 07
                //            a4 a4 00 00 02 c1 10 // command to perform?
                //
                //90 00 (Success)
                //
                //------------------------------------------------
                //[Step 5] Send READ RECORD to read all records in SFI 0
                //------------------------------------------------
                //00 b2 02 04 00
                //
                //61 21
                //      4f 09
                //            d2 76 00 00 22 00 00 00 01
                //      50 0b
                //            S  C  T     L  O  Y  A  L  T  Y
                //            53 43 54 20 4c 4f 59 41 4c 54 59
                //      52 07
                //            a4 a4 00 00 02 10 00
                //
                //      00 00 00 00 00
                //
                //90 00 (Success)
                //
                //------------------------------------------------
                //[Step 6] Send READ RECORD to read all records in SFI 0
                //------------------------------------------------
                //00 b2 03 04 00
                //
                //61 21
                //      4f 09
                //            d2 76 00 00 22 00 00 00 02
                //      50 0d
                //            B  U  S  I  N  E  S  S     C  A  R  D
                //            42 55 53 49 4e 45 53 53 20 43 41 52 44
                //      52 07
                //            a4 a4 00 00 02 10 00
                //
                //      00 00 00
                //
                //90 00 (Success)
                //
                //------------------------------------------------
                //[Step 7] Send READ RECORD to read all records in SFI 0
                //------------------------------------------------
                //00 b2 04 04 00
                //
                //94 04


                MasterFile mf = new MasterFile(selectMFResponse.getData());
                getCard().setMasterFile(mf);
            }
//            else {

            Log.commandHeader("SELECT FILE Master File by identifier (if available)");

            command = Iso7816Commands.selectMasterFileByIdentifier();

            CardResponse selectMFByIdResponse = EMVUtil.sendCmd(terminal, command);

            SW1 = (byte) selectMFByIdResponse.getSW1();
            SW2 = (byte) selectMFByIdResponse.getSW2();

            if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
                //Example response (ATR: 3b 95 95 40 ff d0 00 54 01 32)
                //6f 17
                //      82 01
                //            38
                //      84 06
                //            a0 00 00 00 18 00
                //      8a 01
                //            05
                MasterFile mf = new MasterFile(selectMFByIdResponse.getData());
                getCard().setMasterFile(mf);
            }
//            }

            //OK, master file is available. Try to read some known files
            if (getCard().getMasterFile() != null) {

                //ATR file (path='3F002F01'). contains a set of BER-TLV data objects
                //When the card provides indications in several places,
                //the indication valid for a given EF is the closest one to that
                //EF within the path from the MF to that EF.

                Log.commandHeader("SELECT FILE EF.ATR (if available)");

                command = Util.fromHexString("00 A4 02 00 02 2F 01 00");
//                command = "00 A4 08 0C 02 2F 01 00";

//                CardResponse selectMFResponse2     = EMVUtil.sendCmd(terminal, "00 A4 01 00 02 3F 00");
                CardResponse selectATRFileResponse = EMVUtil.sendCmd(terminal, command);

                SW1 = (byte) selectATRFileResponse.getSW1();
                SW2 = (byte) selectATRFileResponse.getSW2();

                if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {

                    //TODO
                    CardResponse readBinaryResponse = EMVUtil.sendCmd(terminal, "00 B0 00 00 00");

                    int sfi = 0; //TODO what sfi to read? from historical bytes?

                    byte recordNum = 1;
                    do {

                        Log.commandHeader("Send READ RECORD to read all records in SFI " + sfi);

                        command = EMVAPDUCommands.readRecord((int) recordNum, sfi);

                        CardResponse readRecordResponse = EMVUtil.sendCmd(terminal, command);

                        SW1 = (byte) readRecordResponse.getSW1();
                        SW2 = (byte) readRecordResponse.getSW2();

                        if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
                            BERTLV tlv = TLVUtil.getNextTLV(new ByteArrayInputStream(readRecordResponse.getData()));
                            getCard().getMasterFile().addUnhandledRecord(tlv);
                        }

                        recordNum++;

                    } while (SW1 == (byte) 0x90 && SW2 == (byte) 0x00); //while SW1SW2 != 6a83

                } else {
                    Log.commandHeader("SELECT FILE EF.ATR (if available)");

                    command = Util.fromHexString("00 A4 00 00 04 3F 00 2F 01 00");

                    CardResponse selectATRFileAbsPathResponse = EMVUtil.sendCmd(terminal, command);

                    SW1 = (byte) selectATRFileAbsPathResponse.getSW1();
                    SW2 = (byte) selectATRFileAbsPathResponse.getSW2();

                    if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
                        //Do the select ATR File command ever return any data?


                        int sfi = 0; //TODO what sfi to read? from historical bytes?

                        byte recordNum = 1;
                        do {

                            Log.commandHeader("Send READ RECORD to read all records in SFI " + sfi);

                            command = EMVAPDUCommands.readRecord((int) recordNum, sfi);

                            CardResponse readRecordResponse = EMVUtil.sendCmd(terminal, command);

                            SW1 = (byte) readRecordResponse.getSW1();
                            SW2 = (byte) readRecordResponse.getSW2();

                            if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
                                BERTLV tlv = TLVUtil.getNextTLV(new ByteArrayInputStream(readRecordResponse.getData()));
                                getCard().getMasterFile().addUnhandledRecord(tlv);
                            }

                            recordNum++;

                        } while (SW1 == (byte) 0x90 && SW2 == (byte) 0x00); //while SW1SW2 != 6a83

                    }

                }


                //EF.DIR
                //DIR file (path='3F002F00'). contains a set of BER-TLV data objects
                Log.commandHeader("SELECT FILE EF.DIR (if available)");

                command = Util.fromHexString("00 A4 02 00 02 2F 00 00");
          //      command = "00 A4 08 0C 02 2F 00 00";

                CardResponse selectMFResponse2     = EMVUtil.sendCmd(terminal, "00 A4 01 00 02 3F 00");
                CardResponse selectDIRFileResponse = EMVUtil.sendCmd(terminal, command);

                SW1 = (byte) selectDIRFileResponse.getSW1();
                SW2 = (byte) selectDIRFileResponse.getSW2();

                if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
                    //Example response (ATR: 3b 95 95 40 ff d0 00 54 01 32)
                    //6f 12
                    //      82 01
                    //            01
                    //      83 02
                    //            2f 00
                    //      80 02
                    //            00 3e
                    //      8a 01
                    //            05


                    int sfi = 0; //TODO what sfi to read? from historical bytes??

                    //Example from card with empty response to select (9000)
                    //00B2010400
                    //  61154F07A0000000045555500A415044554C6F67676572
                    //00B2020400
                    //  61114F07A00000000460005006436972727573
                    //00B2030400
                    //  -> 6A83

                    byte recordNum = 1;
                    do {

                        Log.commandHeader("Send READ RECORD to read all records in SFI " + sfi);

                        command = EMVAPDUCommands.readRecord((int) recordNum, sfi);

                        CardResponse readRecordResponse = EMVUtil.sendCmd(terminal, command);

                        SW1 = (byte) readRecordResponse.getSW1();
                        SW2 = (byte) readRecordResponse.getSW2();

                        if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
                            BERTLV tlv = TLVUtil.getNextTLV(new ByteArrayInputStream(readRecordResponse.getData()));
                            getCard().getMasterFile().addUnhandledRecord(tlv);
                        }

                        recordNum++;

                    } while (SW1 == (byte) 0x90 && SW2 == (byte) 0x00); //while SW1SW2 != 6a83

                    //Issue READ RECORDs
                    //-> 00 b2 01 04 02
                    //
                    //<- 61 13 (re-read with length 13)
                    //
                    //   90 00
                    //
                    //   READ RECORD
                    //-> 00 b2 01 04 15
                    //
                    //#02
                    //-> 00 b2 02 04 02
                    //...



                } else {
                    //EF.DIR
                    //DIR file (path='3F002F00'). contains a set of BER-TLV data objects
                    Log.commandHeader("SELECT FILE EF.DIR (if available)");

                    command = Util.fromHexString("00 A4 00 00 04 3F 00 2F 00 00");

                    CardResponse selectDIRFileAbsPathResponse = EMVUtil.sendCmd(terminal, command);

                    SW1 = (byte) selectDIRFileAbsPathResponse.getSW1();
                    SW2 = (byte) selectDIRFileAbsPathResponse.getSW2();

                    if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
                        int sfi = 0; //TODO what sfi to read? from historical bytes??

                        byte recordNum = 1;
                        do {

                            Log.commandHeader("Send READ RECORD to read all records in SFI " + sfi);

                            command = EMVAPDUCommands.readRecord((int) recordNum, sfi);

                            CardResponse readRecordResponse = EMVUtil.sendCmd(terminal, command);

                            SW1 = (byte) readRecordResponse.getSW1();
                            SW2 = (byte) readRecordResponse.getSW2();

                            if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
                                BERTLV tlv = TLVUtil.getNextTLV(new ByteArrayInputStream(readRecordResponse.getData()));
                                getCard().getMasterFile().addUnhandledRecord(tlv);
                            }

                            recordNum++;

                        } while (SW1 == (byte) 0x90 && SW2 == (byte) 0x00); //while SW1SW2 != 6a83
                    }
                }

                // When the physical interface does not allow a card to answer to reset, e.g., a universal serial bus or an
                // access by radio frequency, a GET DATA command (see 7.4.2) may retrieve historical bytes (tag '5F52').
            }

        }

        //Try to select all known AIDs
        if(sessionEnv.getProbeAllKnownAIDs()){
            probeAllKnownAIDs();
        }

        //Still nothing found?
        //Select by 5 byte RID
        if(smartCard.getAllAIDs().isEmpty() && sessionEnv.getSelectAllRIDs()) {
            Map<String, RID> ridMap = RID_DB.getAll();
            for(String ridString : ridMap.keySet()) {
                RID rid = ridMap.get(ridString);

                Log.commandHeader("Send SELECT RID " + rid.getApplicant() + " ("+rid.getCountry()+")");

                command = Iso7816Commands.selectByDFName(rid.getRIDBytes(), true, (byte)0);

                response = EMVUtil.sendCmdNoParse(terminal, command);

                SW1 = (byte) response.getSW1();
                SW2 = (byte) response.getSW2();

                if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
                    smartCard.addAID(new AID(rid.getRIDBytes()));
                }
            }
        }


        //Process the AIDs that was found
        for(AID aid : smartCard.getAllAIDs()) {
            List<ApplicationHandler> aidHandlers = Registry.getInstance().getHandlersForAid(aid);
            if(aidHandlers != null) {
                try{
                    for(ApplicationHandler aidHandler : aidHandlers) {
                        if(aidHandler.process(aid, smartCard, terminal)) {
                            //The aid is handled exclusively by this Handler
                            break;
                        }
                    }
                } catch(TerminalException processEx) {
                    Log.info(Util.getStackTrace(processEx));
                } catch(RuntimeException processEx) {
                    Log.info(Util.getStackTrace(processEx));
                }

            }
        }

    }

    public void probeAllKnownAIDs() throws TerminalException {

        smartCard.setAllKnownAidsProbed();

        byte[] command;

        Collection<KnownAIDList.KnownAID> terminalCandidateList = KnownAIDList.getAIDs();

        for (KnownAIDList.KnownAID terminalAIDCandidate : terminalCandidateList) {

            //ICC support for the selection of a DF file using only a
            //partial DF name is not mandatory. However, if the ICC does
            //support partial name selection, it shall comply with the following:
            //If, after a DF file has been successfully selected, the terminal
            //repeats the SELECT command having P2 set to the Next Occurrence
            //option (see Table 42) and with the same partial DF name, the card
            //shall select a different DF file matching the partial name,
            //if such other DF file exists.
            //Repeated issuing of the same command with no intervening application
            //level commands shall retrieve all such files, but shall retrieve
            //no file twice.
            //After all matching DF files have been selected, repeating the same
            //command again shall result in no file being selected, and the card
            //shall respond with SW1 SW2 = '6A82' (file not found).


            Log.commandHeader("Direct selection of Application to generate candidate list - "+terminalAIDCandidate.getName());
            command = EMVAPDUCommands.selectByDFName(terminalAIDCandidate.getAID().getAIDBytes());
            CardResponse selectAppResponse = EMVUtil.sendCmd(terminal, command);

            //TODO merge data if AID already found (to prevent PARTIAL AID being listed as app in EMV card dump)

            if (selectAppResponse.getSW() == SW.FUNCTION_NOT_SUPPORTED.getSW()) { //6a81
                Log.info("'SELECT File using DF name = AID' not supported");
            } else if (selectAppResponse.getSW() == SW.FILE_OR_APPLICATION_NOT_FOUND.getSW()){
                if(Arrays.equals(terminalAIDCandidate.getAID().getAIDBytes(), Util.fromHexString("a0 00 00 01 67 41 30 00 ff"))
                        && selectAppResponse.getData() != null
                        && selectAppResponse.getData().length > 0){
                    //The JCOP identify applet is not selectable (responds with SW = 6a82), but if present, it returns data
                    smartCard.addAID(terminalAIDCandidate.getAID());
                    if(selectAppResponse.getData().length == 19) {
                        //Parse JCOP data
                        smartCard.addApplication(new JCOPApplication(terminalAIDCandidate.getAID(), selectAppResponse.getData(), smartCard));
                    }

                }
            } else if (selectAppResponse.getSW() == SW.SELECTED_FILE_INVALIDATED.getSW()) {
                //App blocked
                Log.info("Application BLOCKED");
            } else if (selectAppResponse.getSW() == SW.SUCCESS.getSW()) {
                smartCard.addAID(terminalAIDCandidate.getAID());

                if (terminalAIDCandidate.partialMatchAllowed()) {
                    Log.debug("Partial match allowed. Selecting next occurrence");

                    EMVApplication appTemplate = new EMVApplication();
                    try {
                        EMVUtil.parseFCIADF(selectAppResponse.getData(), appTemplate); //Check if FCI can be parsed (if the app is a valid EMV app)
                        if (appTemplate.getAID() != null) {
                            smartCard.addAID(appTemplate.getAID());
                        } else {
                            //No AID found in ADF.

                        }
                    } catch (SmartCardException parseEx) {
                        //The application is not a valid EMV app
                        Log.debug(Util.getStackTrace(parseEx));
                        Log.info("Unable to parse FCI ADF for AID=" + terminalAIDCandidate.getAID() + ". Skipping");
                    }

                    byte[] previousResponse = selectAppResponse.getData();

                    boolean hasNextOccurrence = true;
                    while (hasNextOccurrence) {
                        command = EMVAPDUCommands.selectByDFNameNextOccurrence(terminalAIDCandidate.getAID().getAIDBytes());
                        selectAppResponse = EMVUtil.sendCmd(terminal, command);

                        //Workaround: Some cards seem to misbehave.
                        //Abort if current response == previous response
                        if(Arrays.equals(previousResponse, selectAppResponse.getData())){
                            Log.debug("Current response was equal to the previous response. Aborting 'select next occurrence'");
                            break;
                        }

                        Log.debug("Select next occurrence SW: " + Util.short2Hex(selectAppResponse.getSW()) + " (Stop if SW=" + Util.short2Hex(SW.FILE_OR_APPLICATION_NOT_FOUND.getSW())+")");
                        if (selectAppResponse.getSW() == SW.FUNCTION_NOT_SUPPORTED.getSW()) { //6a81
                            Log.info("'SELECT File using DF name = AID' not supported");
                        } else if (selectAppResponse.getSW() == SW.SELECTED_FILE_INVALIDATED.getSW()) {
                            //App blocked
                            Log.info("Application BLOCKED");
                        } else if (selectAppResponse.getSW() == SW.FILE_OR_APPLICATION_NOT_FOUND.getSW()) {
                            hasNextOccurrence = false;
                            Log.debug("No more occurrences");
                        } else if (selectAppResponse.getSW() == SW.SUCCESS.getSW()) {

                            EMVApplication appCandidate = new EMVApplication();
                            try {
                                EMVUtil.parseFCIADF(selectAppResponse.getData(), appCandidate); //Check if FCI can be parsed (if the app is a valid EMV app)
                                if (appTemplate.getAID() != null) {
                                    smartCard.addAID(appTemplate.getAID());
                                } else {
                                    //No AID found in ADF.

                                }
                            } catch (SmartCardException parseEx) {
                                //The application is not a valid EMV app
                                Log.debug(Util.getStackTrace(parseEx));
                                Log.info("Unable to parse FCI ADF for AID=" + terminalAIDCandidate.getAID() + ". Skipping");
                            }
                        }

                    }
                } else {

                    EMVApplication appTemplate = new EMVApplication();
//                    appTemplate.setAID(terminalAIDCandidate.getAID());
                    try {
                        EMVUtil.parseFCIADF(selectAppResponse.getData(), appTemplate); //Check if FCI can be parsed (if the app is a valid EMV app)
                        if (appTemplate.getAID() != null) {
                            smartCard.addAID(appTemplate.getAID());
                        } else {
                            //No AID found in ADF.
                        }
                    } catch (SmartCardException parseEx) {
                        //The application is not a valid EMV app
                        Log.debug(Util.getStackTrace(parseEx));
                        Log.info("Unable to parse FCI ADF for AID=" + terminalAIDCandidate.getAID() + ". Skipping");
                    }
                }
            }
        }
    }
}
