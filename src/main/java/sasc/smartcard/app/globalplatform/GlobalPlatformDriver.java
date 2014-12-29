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

import java.util.concurrent.atomic.AtomicBoolean;
import sasc.emv.EMVUtil;
import sasc.iso7816.AID;
import sasc.iso7816.Iso7816Commands;
import sasc.lookup.ATR_DB;
import sasc.terminal.CardConnection;
import sasc.terminal.CardResponse;
import sasc.terminal.TerminalException;
import sasc.terminal.TerminalUtil;
import sasc.util.Log;
import sasc.util.Util;
import sasc.smartcard.common.ApplicationHandler;
import sasc.smartcard.common.SmartCard;

/**
 *
 * @author sasc
 */
public class GlobalPlatformDriver implements ApplicationHandler {

    private CardConnection terminal;
    private AtomicBoolean initialized = new AtomicBoolean(false);
    private static final byte[] GPSD_AID = Util.fromHexString("A000000003 000000");
    private static final byte[] GPSD211_AID = Util.fromHexString("A000000151 0000");

    @Override
    public boolean process(AID aid, SmartCard card, CardConnection terminal) throws TerminalException {

        int SW1;
        int SW2;
        byte[] command;
        byte[] data;
        CardResponse response;
        ISDApplication isdApp = new ISDApplication(aid, card);

        if (card != null) {
            card.addApplication(isdApp);
        }

        Log.commandHeader("Select Global Platform Security Domain");
        command = Iso7816Commands.selectByDFName(aid.getAIDBytes(), true, (byte) 0x00);
        response = EMVUtil.sendCmd(terminal, command);

        SW1 = (byte) response.getSW1();
        SW2 = (byte) response.getSW2();
        data = response.getData();

        if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00 && data.length > 0) {
            try {
                SecurityDomainFCI securityDomainFCI = SecurityDomainFCI.parse(data);
                isdApp.setFCI(securityDomainFCI);
                Log.info(securityDomainFCI.toString());
            } catch (RuntimeException ex) {
                Log.debug(Util.getStackTrace(ex));
            }
        }

        Log.commandHeader("Get Data CPLC (Card Production Life Cycle Data) History File Identifiers");
        command = Util.fromHexString("00 CA 9F 7F 00");

        response = EMVUtil.sendCmdNoParse(terminal, command); //Not TLV encoded

        SW1 = (byte) response.getSW1();
        SW2 = (byte) response.getSW2();

        if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
            try {
                CPLC cplc = CPLC.parse(response.getData());
                isdApp.setCPLC(cplc);
                if (cplc != null) {
                    Log.info(cplc.toString());
                }
            } catch (RuntimeException ex) {
                Log.debug(Util.getStackTrace(ex));
            }
        }


        Log.commandHeader("Get Data (Issuer Identification Number)");
        command = Util.fromHexString("00 CA 00 42 00");
        response = EMVUtil.sendCmdNoParse(terminal, command);
        SW1 = (byte) response.getSW1();
        SW2 = (byte) response.getSW2();
        data = response.getData();
        if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
            try {
                isdApp.setIssuerIdentificationNumber(response.getData());
            } catch (RuntimeException ex) {
                Log.debug(Util.getStackTrace(ex));
            }
        }


        Log.commandHeader("Get Data (Card Image Number)");
        command = Util.fromHexString("00 CA 00 45 00");
        response = EMVUtil.sendCmdNoParse(terminal, command);
        SW1 = (byte) response.getSW1();
        SW2 = (byte) response.getSW2();
        data = response.getData();
        if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
            try {
                isdApp.setCardImageNumber(response.getData());
            } catch (RuntimeException ex) {
                Log.debug(Util.getStackTrace(ex));
            }
        }


        Log.commandHeader("Get Data (Pre-Issuance Data / Serial Number Registers) for Credentsys-J OS");
        command = Util.fromHexString("00 CA 00 46 00");
        response = EMVUtil.sendCmd(terminal, command);
        SW1 = (byte) response.getSW1();
        SW2 = (byte) response.getSW2();
        data = response.getData();
        if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
            try {
                isdApp.setCredentsysJ_preIssuanceData(response.getData());
            } catch (RuntimeException ex) {
                Log.debug(Util.getStackTrace(ex));
            }
        }


        Log.commandHeader("Get Data (Card Data)");
        command = Util.fromHexString("00 CA 00 66 00");
        response = EMVUtil.sendCmd(terminal, command);

        SW1 = (byte) response.getSW1();
        SW2 = (byte) response.getSW2();

        if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
            try {
                CardRecognitionData cardRecognitionData = CardRecognitionData.parse(response.getData());
                isdApp.setCardRecognitionData(cardRecognitionData);
            } catch (RuntimeException ex) {
                Log.debug(Util.getStackTrace(ex));
            }
        }

        Log.commandHeader("Get Data (Key Information Template)");
        command = Util.fromHexString("00 CA 00 E0 00");
        response = EMVUtil.sendCmd(terminal, command);
        SW1 = (byte) response.getSW1();
        SW2 = (byte) response.getSW2();
        data = response.getData();
        if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
            try {
                KeyInformationTemplate keyInfo = KeyInformationTemplate.parse(data);
                    isdApp.setKeyInformationTemplate(keyInfo);
            } catch (RuntimeException ex) {
                Log.debug(Util.getStackTrace(ex));
            }
        }

        Log.commandHeader("Get Data (Sequence Counter of the default Key Version Number)");
        command = Util.fromHexString("00 CA 00 C1 00");
        response = EMVUtil.sendCmdNoParse(terminal, command);
        SW1 = (byte) response.getSW1();
        SW2 = (byte) response.getSW2();
        data = response.getData();
        if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
            try {
                isdApp.setSequenceCounterOfTheDefaultKeyVersionNumber(response.getData());
            } catch (RuntimeException ex) {
                Log.debug(Util.getStackTrace(ex));
            }
        }

        Log.commandHeader("Get Data (Confirmation Counter)");
        command = Util.fromHexString("00 CA 00 C2 00");
        response = EMVUtil.sendCmdNoParse(terminal, command);
        SW1 = (byte) response.getSW1();
        SW2 = (byte) response.getSW2();
        data = response.getData();
        if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
            try {
                isdApp.setConfirmationCounter(response.getData());
            } catch (RuntimeException ex) {
                Log.debug(Util.getStackTrace(ex));
            }
        }

        return false; //Handle non-exclusively
    }

    //SELECT:
    //00 A4 04 00 00
    //    6F File Control Information (FCI) Template
    //       84 Dedicated File (DF) Name
    //          A0 00 00 00 96 02 00
    //       A5 File Control Information (FCI) Proprietary Template
    //          9F6E Unknown tag
    //               48 41 52 53 00 02
    //          9F65 Unknown tag
    //               FF
    //          DF02 Unknown tag
    //               01

    /*
     *
     GET-DATA CPLC (Card Production Life Cycle Data):
     -> 00 CA 9F 7F 00
     <- 6a 88

     GET-DATA (Issuer Identification Number):
     -> 00 CA 00 42 00
     <- 6a 88

     GET-DATA (Card Image Number):
     -> 00 CA 00 45 00
     <- 6a 88

     GET-DATA (Pre-Issuance Data (Credentsys-J)):
     -> 00 CA 00 46 00
     <- 6a 88


     '9F6E' Application production life cycle data
     is an optional data element of the SELECT AID response message. Its presence ensures
     backward compatibility with the previous version of the Card Specification
     Application Life Cycle Status (tag '9F70'). spec 2.0.1

     An example of use would be to search all Applications and Security Domains (if any)
     that are in the Life Cycle State SELECTABLE on the card.
     In such example, the data field of the GET STATUS command would be coded as follows:
     '4F 00 9F 70 01 07'
     card recognition data (get data 66)

     *
     * GP card spec v2.1.1 section F.2 Structure of Card Recognition Data
     *
     GET-DATA (Card Data):
     -> 00 CA 00 66 00
     <- 73 22
     06 07
     2A 86 48 86 FC 6B 01
     60 0C
     06 0A
     2A 86 3A 00 8A 3A 02 04 02 00
     63 09
     06 07
     2A 86 3A 00 8A 3A 03

     90 00

     GET-DATA (Key Information Template):
     -> 00 CA 00 E0 00
     <- 6a 88

     GET-DATA (Sequence Counter of the default Key Version Number):
     -> 00 CA 00 C1 00
     <- 6a 88

     GET-DATA (Confirmation Counter):
     -> 00 CA 00 C2 00
     <- 6a 88

     * GPShell (these require authentication):
     *
     * 80 F2 [cardElement] 00 02 4F 00 00
     *
     * get_status -element e0
     *     List applets and packages and security domains
     * get_status -element 10
     *     List of Ex. Load File (AID state Ex. Module AIDs)
     * get_status -element 20
     *     List packages
     * get_status -element 40
     *     List applets or security domains
     * get_status -element 80
     *     List Card Manager / Security Issuer Domain
     Data Commands
     get_data -identifier identifier
     
     * 
     * 
     * About "chip id":
    * There are a few things that might be considered the "chip id":
    *
    * - Global Platform card: (Get Data from the ISD) tags 0x42 and 0x45
    *
    * - the issuer id and card id (may or may not be set on a generic development
    * card)
    *
    * - Some specific types of cards have Card Production Life Cycle (CPLC) data
    * (JCOP cards have this at GET DATA 0x9f7f.
    *
    * a hash of that data will be unique on a per card basis for at least JCOP
    * cards, and probably for the rest
    */
    public static void main(String[] args) throws Exception {
        CardConnection cardConnection = TerminalUtil.connect(TerminalUtil.State.CARD_INSERTED); //Waits for card present
        Log.info(Util.prettyPrintHexNoWrap(cardConnection.getATR()));
        Log.info(ATR_DB.searchATR(cardConnection.getATR()).toString());
        GlobalPlatformDriver gpd = new GlobalPlatformDriver();
        gpd.process(new AID(GPSD_AID), null, cardConnection);
    }
}
