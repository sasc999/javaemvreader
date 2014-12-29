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

import sasc.smartcard.common.SessionProcessingEnv;
import sasc.smartcard.common.SmartCard;
import sasc.util.Log;
import sasc.iso7816.SmartCardException;
import sasc.iso7816.BERTLV;
import sasc.iso7816.AID;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.security.auth.callback.PasswordCallback;
import sasc.smartcard.common.CardScanner;
import sasc.iso7816.ShortFileIdentifier;
import sasc.iso7816.TLVException;
import sasc.iso7816.TLVUtil;
import sasc.terminal.CardResponse;
import sasc.terminal.TerminalException;
import sasc.terminal.CardConnection;
import sasc.util.Util;

/**
 * Holds EMV session related information
 *
 * @author sasc
 */
public class EMVSession {

    private SmartCard card = null;
    private CardConnection terminal;
    private boolean contextInitialized = false;

    public static EMVSession startSession(SmartCard card, CardConnection terminal) {
        if (card == null || terminal == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        return new EMVSession(card, terminal);
    }

    private EMVSession(SmartCard card, CardConnection terminal) {
        this.card = card;
        this.terminal = terminal;
    }

    public SmartCard getCard() {
        return card;
    }

    /**
     * Initializes the card by reading all Global data and FCI/DDF
     * "1PAY.SYS.DDF01" (and some other data outside of the EMV spec)
     */
    public void initContext() throws TerminalException {

        if (contextInitialized) {
            throw new SmartCardException("EMV context already initalized.");
        }

        byte[] command;
        int SW1;
        int SW2;

        /*
         * The terminal has a list containing the EMVApplication Identifier
         * (AID) of every EMV application that it is configured to support, and
         * the terminal must generate a candidate list of applications that are
         * supported by both the terminal and card. The terminal may attempt to
         * obtain a directory listing of all card applications from the card's
         * PSE. If this is not supported or fails to find a match, the terminal
         * must iterate through its list asking the card whether it supports
         * each individual AID.
         *
         * If there are multiple applications in the completed candidate list,
         * or the application requires it, then the cardholder will be asked to
         * choose an application; otherwise it may be automatically selected
         */


        //First, try the "Payment System Directory selection method".
        //if that fails, try direct selection by using a terminal resident list of supported AIDs

        Log.commandHeader("SELECT FILE 1PAY.SYS.DDF01 to get the PSE directory");

        command = EMVAPDUCommands.selectPSE();

        CardResponse selectPSEdirResponse = EMVUtil.sendCmd(terminal, command);

        //Example result from the command above:

        //6f 20 //FCI Template
        //      84 0e //DF Name
        //            31 50 41 59 2e 53 59 53 2e 44 44 46 30 31
        //      a5 0e //FCI Proprietary Template
        //            88 01 //SFI of the Directory Elementary File
        //                  02
        //            5f 2d 04 //Language Preference
        //                     6e 6f 65 6e
        //            9f 11 01 //Issuer Code Table Index
        //                     01 (=ISO 8859-1)

        SW1 = (byte) selectPSEdirResponse.getSW1();
        SW2 = (byte) selectPSEdirResponse.getSW2();

        if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
            //PSE is available

            try{
                DDF pseDDF = EMVUtil.parseFCIDDF(selectPSEdirResponse.getData(), card);
                card.setType(SmartCard.Type.CONTACTED);
                getCard().setPSE(pseDDF);
                if(pseDDF.getSFI() != null) {
                    readPSERecords(pseDDF.getSFI());
                }
            }catch(TLVException tlvex){
                Log.debug(Util.getStackTrace(tlvex));
            }
        } 

        // Some cards have both PSE and PPSE, where the PSE might not list any SFI or applications
        if (card.getEmvApplications().isEmpty()) {
            //try to select the PPSE (Proximity Payment System Environment) 2PAY.SYS.DDF01

            Log.commandHeader("SELECT FILE 2PAY.SYS.DDF01 to get the PPSE directory");

            command = EMVAPDUCommands.selectPPSE();

            CardResponse selectPPSEdirResponse = EMVUtil.sendCmd(terminal, command);

            SW1 = (byte) selectPPSEdirResponse.getSW1();
            SW2 = (byte) selectPPSEdirResponse.getSW2();

            if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
                //PPSE is available
                try{
                    DDF ppseDDF = EMVUtil.parseFCIDDF(selectPPSEdirResponse.getData(), card);
                    card.setType(SmartCard.Type.CONTACTLESS);
                    getCard().setPSE(ppseDDF);
                    if (ppseDDF.getSFI() != null) {
                        readPSERecords(ppseDDF.getSFI());
                    }
                }catch(TLVException tlvex){
                    Log.debug(Util.getStackTrace(tlvex));
                }

            }
        }

        if (card.getEmvApplications().isEmpty()) { //No PSE/PPSE found, or no apps listed

            //An ICC need not contain PSE, or the PSE might not contain any applications.
            //Direct selection of EMVApplication might be used on some cards.
            //PSE alternatives (EMV book 1 page 161)

            if(!card.allKnownAidsProbed()){
                Log.info("No PSE found. Using direct selection by AID to generate candidate list");
                //TODO only probe for known EMV AIDs
                SessionProcessingEnv sessionEnv = new SessionProcessingEnv();
                sessionEnv.setProbeAllKnownAIDs(true);
                CardScanner scanner = new CardScanner(getCard(), terminal, sessionEnv);
                scanner.probeAllKnownAIDs();
            }
        }

        contextInitialized = true;
    }
    
    private void readPSERecords(ShortFileIdentifier shortFileIdentifier) throws TerminalException {
        
        byte[] command;
        byte SW1;
        byte SW2;
        
        try {
            int sfi = shortFileIdentifier.getValue();

            byte recordNum = 1;
            do {

                Log.commandHeader("Send READ RECORD to read all records in SFI " + sfi);

                command = EMVAPDUCommands.readRecord((int) recordNum, sfi);

                CardResponse readRecordResponse = EMVUtil.sendCmd(terminal, command);

                //Example Response from the command above:

                //70 23
                //      61 21
                //            4f 07 //AID
                //                  a0 00 00 00 03 10 10
                //            50 04 //Application Label
                //                  56 49 53 41 (=VISA)
                //            9f 12 0c //Application Preferred Name
                //                     56 49 53 41 20 43 6c 61 73 73 69 63 (=VISA Classic)
                //            87 01 //Application priority indicator
                //                  02


                SW1 = (byte) readRecordResponse.getSW1();
                SW2 = (byte) readRecordResponse.getSW2();

                if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
                    EMVUtil.parsePSERecord(readRecordResponse.getData(), card);
                }

                recordNum++;

            } while (SW1 == (byte) 0x90 && SW2 == (byte) 0x00); //while SW1SW2 != 6a83

        } catch(TLVException tlvex) {
            Log.debug(Util.getStackTrace(tlvex));
        }

        
    }

    public void selectApplication(EMVApplication app) throws TerminalException {

        if (app == null) {
            throw new IllegalArgumentException("Parameter 'app' cannot be null");
        }
        if (!contextInitialized) {
            throw new SmartCardException("Card not initialized. Call initCard() first");
        }
        EMVApplication currentSelectedApp = card.getSelectedApplication();
        if (currentSelectedApp != null && app.getAID().equals(currentSelectedApp.getAID())) {
            throw new SmartCardException("Application already selected. AID: " + app.getAID());
        }

        AID aid = app.getAID();
        byte[] command;

        Log.commandHeader("Select application by AID");
        command = EMVAPDUCommands.selectByDFName(aid.getAIDBytes());

        CardResponse selectAppResponse = EMVUtil.sendCmd(terminal, command);

        if (selectAppResponse.getSW() == SW.SELECTED_FILE_INVALIDATED.getSW()) {
            //App blocked
            Log.info("Application BLOCKED");
            //TODO abort execution if app blocked?
            //throw new SmartCardException("EMVApplication " + Util.byteArrayToHexString(aid.getAIDBytes()) + " blocked");
        }

        EMVUtil.parseFCIADF(selectAppResponse.getData(), app);

        //Example Response from previous command:
        //      6f 37
        //      84 07 //AID
        //            a0 00 00 00 03 10 10
        //      a5 2c
        //            50 04 //Application Label (= VISA)
        //                  56 49 53 41
        //            87 01 //Application priority indicator
        //                  02
        //            9f 38 06 //PDOL
        //                     9f 1a
        //                           02
        //                     5f 2a
        //                           02
        //            5f 2d 04 //Language Preference
        //                     6e 6f 65 6e (= no en)
        //            9f 11 01 //Issuer code table index
        //                     01
        //            9f 12 0c //Application Preferred name
        //                     56 49 53 41 20 43 6c 61 73 73 69 63 (= VISA Classic)



        //The card supplies the PDOL (if present) to the terminal as part of the FCI
        //provided in response to the SELECT FILE (Application Definition File) command

        //If PDOL present, the ICC requires parameters from the Terminal.
        //In this specific example:
        //9F1A = Indicates the country code of the terminal, represented according to ISO 3166
        //578 Norway = 0x0578
        //5F2A = Transaction Currency Code: Indicates the currency code of the transaction according to ISO 4217 (numeric 3)
        //NORWAY  Norwegian Krone  NOK  578  == 0x0578
        //PDOL response data (used in the GET PROCESSING OPTS command) = 05 78 05 78

        getCard().setSelectedApplication(app);

    }

    public void initiateApplicationProcessing() throws TerminalException {

        EMVApplication app = card.getSelectedApplication();

        if (app == null) {
            throw new SmartCardException("No application selected. Call selectApplication(Application) first");
        }
        if (app.isInitializedOnICC()) {
            throw new SmartCardException("Application already initialized for processing. AID=" + app.getAID());
        }

        // The terminal shall set all bits in the Transaction Status Information (TSI) 
        // and the Terminal Verification Results (TVR) to 0
        EMVTerminal.resetTVR();
        app.getTransactionStatusInformation().reset();

        byte[] command;
        int SW1;
        int SW2;

        Log.commandHeader("Send GET PROCESSING OPTIONS command");



        //If the PDOL does not exist, the GET PROCESSING OPTIONS command uses a command data field of '8300',
        //indicating that the length of the value field in the command data is zero.

        DOL pdol = app.getPDOL();

        command = EMVAPDUCommands.getProcessingOpts(pdol, app);

        CardResponse getProcessingOptsResponse = EMVUtil.sendCmd(terminal, command);

        SW1 = (byte) getProcessingOptsResponse.getSW1();
        SW2 = (byte) getProcessingOptsResponse.getSW2();


        //After receiving the GET PROCESSING OPTIONS C-APDU, the card application checks whether the flow conditions
        //needed to process this command are fulfilled.
        //-First, it checks that there is currently an application selected in the card
        //-Second, the card checks that this is the first time in the current card session that the terminal issues
        // the GET PROCESSING OPTIONS command
        //
        //If any of these conditions are not respected, the card responds with SW1SW2=6985 ("Command not allowed; conditions of use not satisfied")

        
        if (getProcessingOptsResponse.getSW() == SW.COMMAND_NOT_ALLOWED_CONDITIONS_OF_USE_NOT_SATISFIED.getSW()){
            //SW = '6985' (Conditions of use not satisfied), indicates that the transaction cannot be performed with this application.
            //The terminal shall eliminate the current application from consideration and return to the Application Selection function to select another application.
            Log.info("Application did not accept the PDOL returned by the terminal!");
        }else  if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
            //The format of the response message is given in EMV 4.2 book 3, section 6.5.8. 
            EMVUtil.parseProcessingOpts(getProcessingOptsResponse.getData(), app);
            
            if(app.getApplicationInterchangeProfile() == null || app.getApplicationFileLocator() == null){
                throw new SmartCardException("GPO response did not contain AIP and AFL");
            }
            
            app.setInitializedOnICC();

            //read all the records indicated in the AFL
            for (ApplicationElementaryFile aef : app.getApplicationFileLocator().getApplicationElementaryFiles()) {
                int startRecordNumber = aef.getStartRecordNumber();
                int endRecordNumber = aef.getEndRecordNumber();

                for (int recordNum = startRecordNumber; recordNum <= endRecordNumber; recordNum++) {
                    Log.commandHeader("Send READ RECORD to read SFI " + aef.getSFI().getValue() + " record " + recordNum);

                    command = EMVAPDUCommands.readRecord(recordNum, aef.getSFI().getValue());

                    CardResponse readAppDataResponse = EMVUtil.sendCmd(terminal, command);

                    SW1 = (byte) readAppDataResponse.getSW1();
                    SW2 = (byte) readAppDataResponse.getSW2();

                    if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {

                        EMVUtil.parseAppRecord(readAppDataResponse.getData(), app);
                        boolean isInvolvedInOfflineDataAuthentication = (recordNum - startRecordNumber + 1) <= aef.getNumRecordsInvolvedInOfflineDataAuthentication();
                        Record record = new Record(readAppDataResponse.getData(), recordNum, isInvolvedInOfflineDataAuthentication);
                        aef.setRecord(recordNum, record);
                    } else {
                        //Any SW1 SW2 other than '9000' passed to the application layer as a result
                        //of reading any record shall cause the transaction to be terminated [spec]
                        throw new SmartCardException("Reading application data failed for SFI " + aef.getSFI().getValue() + " Record Number: " + recordNum);
                    }
                }

            }
            app.setAllAppRecordsInAFLRead();



            //TODO
            
            //When any mandatory data object is missing, the terminal terminates the transaction. 
            //When an optional data object that is required because of the existence of other data 
            //objects or that is required to support functions that must be performed due to the 
            //setting of bits in the Application Interchange Profile is missing, 
            //the terminal shall set the "ICC data missing" indicator in the Terminal Verification Results (TVR) to 1
            
        }
    }
    
    private enum State {
        SELECTED, APPLICATION_PROCESSING_INITIATED, TRANSACTION_PROCESSING_PREPARED, GENERATE_AC_PERFORMED, TRANSACTION_POST_PROCESSING_PERFORMED
    }
    
    private void verifyProcessingStateMinimum(EMVApplication app, State state) {
        
    }
    
    private void verifyProcessingStateExact(EMVApplication app, State state) {
        if (app == null) {
            throw new SmartCardException("No application selected. Call selectApplication(Application) and initializeApplicationProcessing() first");
        }
    }
    
    //TODO
    //figure 6 transaction flow example
    public void prepareTransactionProcessing() throws TerminalException {
        
        EMVApplication app = card.getSelectedApplication();
        
        verifyAppInitialized(app);
        verifyAllAppRecordsInAFLRead(app); 
        //verifyGenerateAC_NOT_performed(app);
        
        //TODO
        verifyProcessingStateExact(app, State.APPLICATION_PROCESSING_INITIATED);
        
        //10.3 Offline Data Authentication
        performOfflineDataAuthentication(app);
        
        //10.4 Processing restrictions
        processApplicationRestrictions(app);
        
        //10.5 Cardholder Verification (offline/online PIN)
        if(app.getApplicationInterchangeProfile().isCardholderVerificationSupported()) {
            performCardholderVerification(app);
        } else {
            //TODO Set CVM Results to "3F0000" - "No CVM performed"
        }
    
        //10.6 Terminal Risk Management
        //   10.6.1 Floor Limits
        //   10.6.2 Random Transaction Selection
        //   10.6.3 Velocity Checking
        //To better control local risk management, terminals may perform terminal 
        //risk management even when the "Terminal risk management is to be performed" 
        //bit in the Application Interchange Profile is set to 0
        if(app.getApplicationInterchangeProfile().isTerminalRiskManagementToBePerformed() || EMVTerminal.getPerformTerminalRiskManagement()) {
            performTerminalRiskManagement(app);
        } 
    
        //10.7 Terminal Action Analysis
        performTerminalActionAnalysis(app);

        //TODO section 10.8
        
        //Next: perform transaction
    }
    
    private void performOfflineDataAuthentication(EMVApplication app) throws TerminalException {
        //10.3 Offline Data Authentication
        //The terminal shall perform offline data authentication in any order 
        //after Read Application Data but before completion of the terminal action analysis
        
        //Availability of data in the ICC to support offline data authentication is optional; 
        //its presence is indicated in the Application Interchange Profile. 
        //If both the terminal and the ICC support offline data authentication, 
        //the terminal shall perform this function. 
        //Depending on the capabilities of the card and the terminal, SDA or DDA or CDA is performed
        
        //SDA authenticates static data put into the card by the issuer. 
        //DDA and CDA authenticate ICC-resident data, data from the terminal, and the card itself.
        
        
        //TODO verify for TLV encoded records:
        //If the records read for offline data authentication are not TLV-coded 
        //with tag equal to '70' then offline data authentication shall be considered 
        //to have been performed and to have failed; that is, the terminal shall 
        //set the "Offline data authentication was performed" bit in the TSI to 1, 
        //and shall set the appropriate "SDA failed" or "DDA failed" or "CDA failed" bit in the TVR.
        
        //Building of the input list for offline data authentication is considered 
        //the first step in the offline data authentication process. 
        //If the input cannot be built because of a violation of one of the above 
        //rules but offline data authentication should be performed according to the 
        //"Conditions of Execution" above, offline data authentication shall be 
        //considered to have been performed and to have failed; that is, 
        //the terminal shall set the "Offline data authentication was performed" 
        //bit in the TSI to 1 and shall set the appropriate "SDA failed" or 
        //"DDA failed" or "CDA failed" bit in the TVR.
        
        if(app.getApplicationInterchangeProfile().isCDASupported() && EMVTerminal.isCDASupported(app)) {
            //TODO we (the terminal) do not support CDA yet
            
            //Note: 
            //Although the terminal shall commence performing CDA before completion 
            //of Terminal Action Analysis, the terminal will not normally finish 
            //performing CDA until after it has received the response to the GENERATE AC command. 
            //(This is a necessary consequence of the design of CDA.)
            
            processCDA(app);
        } else if(app.getApplicationInterchangeProfile().isDDASupported() && EMVTerminal.isDDASupported(app)) {
            
            internalAuthenticate(app);
            app.getTransactionStatusInformation().setOfflineDataAuthenticationWasPerformed(true);
            
        } else if(app.getApplicationInterchangeProfile().isSDASupported() && EMVTerminal.isSDASupported(app)) {
            if(app.getSignedStaticApplicationData() == null ||  !app.getSignedStaticApplicationData().validate()){
                EMVTerminal.getTerminalVerificationResults().setDDAFailed(true);
            }
            app.getTransactionStatusInformation().setOfflineDataAuthenticationWasPerformed(true);
            
        } else {
            //If neither SDA nor DDA nor CDA is performed, the terminal shall set the 
            //"Offline data authentication was not performed" bit in the TVR to 1.
            EMVTerminal.getTerminalVerificationResults().setOfflineDataAuthenticationWasNotPerformed(true);
        }
 
    }
    
    private void processCDA(EMVApplication app) throws TerminalException {
        //Check if records read for offline data authentication are TLV-coded
        
        //TODO
    }
    
    /**
     * 10.4 Processing restrictions
     * 
     * May be performed at any time after Read Application Data and prior to completion 
     * of the terminal action analysis
     * 
     * The purpose of the Processing Restrictions function is to determine the degree 
     * of compatibility of the application in the terminal with the application in the 
     * ICC and to make any necessary adjustments, including possible rejection of the transaction.
     * 
     * The Processing Restrictions function comprises the following compatibility checks:
     * - Application Version Number
     * - Application Usage Control
     * - Application Effective/Expiration Dates Checking
     * 
     * The terminal shall always execute this function
     * 
     * @param app the application to process
     * @throws TerminalException 
     */
    private void processApplicationRestrictions(EMVApplication app) throws TerminalException {
        
        //10.4.1 Application Version Number
        //The application within both the terminal and the ICC shall maintain an 
        //Application Version Number assigned by the payment system. 
        //The terminal shall use the version number in the ICC to ensure compatibility. 
        //If the Application Version Number is not present in the ICC, 
        //the terminal shall presume the terminal and ICC application versions are compatible, 
        //and transaction processing shall continue. 
        //If the Application Version Number is present in the ICC, it shall be compared 
        //to the Application Version Number maintained in the terminal. 
        //If they are different, the terminal shall set the 
        //"ICC and terminal have different application versions" bit in the TVR to 1.
        
        if(app.getApplicationVersionNumber() != -1 
                && EMVTerminal.getSupportedApplicationVersionNumber(app) != app.getApplicationVersionNumber()) {
            EMVTerminal.getTerminalVerificationResults().setICCAndTerminalHaveDifferentApplicationVersions(true);
        }
        
        //10.4.2 Application Usage Control
        //The Application Usage Control indicates restrictions limiting the 
        //application geographically or to certain types of transactions. 
        //If this data object is present, the terminal shall make the following checks:
        //- If the transaction is being conducted at an ATM, the "Valid at ATMs" 
        //  bit must be on in Application Usage Control.
        //- If the transaction is not being conducted at an ATM, the 
        //  "Valid at terminals other than ATMs" bit must be on in Application Usage Control.
        //
        //If the Application Usage Control and Issuer Country Code are both present 
        //in the ICC, the terminal shall make the checks described in Table 32
        
        if(app.getApplicationUsageControl() != null) {
            if(EMVTerminal.isATM() && !app.getApplicationUsageControl().validAtATMs()
                    || !EMVTerminal.isATM() && !app.getApplicationUsageControl().validAtTerminalsOtherThanATMs()){
                EMVTerminal.getTerminalVerificationResults().setRequestedServiceNotAllowedForCardProduct(true);
            } 
            
            if(app.getIssuerCountryCode() != -1) {
                int issuerCountryCode = app.getIssuerCountryCode();
                
                //TODO table 32
            }
            
            //If any of the above tests fail, the terminal shall set the 
            //"Requested service not allowed for card product" bit in the TVR to 1.
            
        }
        
        
        //10.4.3 Application Effective/Expiration Dates Checking
        //If the Application Effective Date is present in the ICC, the terminal 
        //shall check that the current date is greater than or equal to the 
        //Application Effective Date. If it is not, the terminal shall set the 
        //"Application not yet effective" bit in the TVR to 1. 
        //The terminal shall check that the current date is less than or equal 
        //to the Application Expiration Date. If it is not, the terminal shall 
        //set the "Expired application" bit in the TVR to 1.
        
        Date currentDate = EMVTerminal.getCurrentDate();
        Date effectiveDate = app.getEffectiveDate();
        if(effectiveDate != null && currentDate.before(effectiveDate)) {
            EMVTerminal.getTerminalVerificationResults().setApplicationNotYetEffective(true);
        }
        Date expirationDate = app.getExpirationDate();
        if(expirationDate != null && currentDate.after(expirationDate)) {
            EMVTerminal.getTerminalVerificationResults().setExpiredApplication(true);
        }
        
    }
    
    /**
     * May be performed any time after Read Application Data and before completion of the terminal action analysis
     * 
     * Card holder verification is performed to ensure that the person presenting 
     * the ICC is the person to whom the application in the card was issued.
     * 
     * @param app
     * @throws TerminalException 
     */
    private void performCardholderVerification(EMVApplication app) throws TerminalException {
        
        //TODO finish this
        //see page 108-112
        
        //The terminal shall use the cardholder verification related data in the 
        //ICC to determine whether one of the issuer-specified cardholder 
        //verification methods (CVMs) shall be executed.
        
        //If the CVM List is not present in the ICC, the terminal shall terminate 
        //cardholder verification without setting the "Cardholder verification was performed" bit in the TSI.
        //Note: 
        //A CVM List with no Cardholder Verification Rules is considered to be the same as a CVM List not being present.
        
        CVMList cvmList = app.getCVMList();
        if(cvmList == null || cvmList.getRules().isEmpty()) {
            EMVTerminal.getTerminalVerificationResults().setICCDataMissing(true);
            //TODO Set CVM Results to "3F0000" - "No CVM performed"
            return;
        }
                
        //If the CVM List is present in the ICC, the terminal shall process each 
        //rule in the order in which it appears in the list according to the 
        //following specifications. 
        //Cardholder verification is completed when any one CVM is successfully 
        //performed or when the list is exhausted.
        
        try{
            for(CVRule rule : cvmList.getRules()){
                
//                if(true) {
//                    //TODO fix CVM Processing according to fig 8 page 105 book 3
//                    throw new TerminalException("Must be fixed");
//                }

                //Check condition code (second byte)

                //The conditions expressed in the second byte of the CV Rule are satisfied.
                //The terminal next checks whether it recognises the CVM coded in the first byte of the CV Rule
                if(rule.getConditionAlways()){
                    if(rule.getRule() == CVRule.Rule.FAIL_PROCESSING) {
                        EMVTerminal.getTerminalVerificationResults().setCardholderVerificationWasNotSuccessful(true);
                        return;
                    }
                    if(EMVTerminal.isCVMRecognized(app, rule)){
                        if(!EMVTerminal.isCVMSupported(rule)) {
                            performCVMCodeNotSupportedLogic(rule);
                        }
                        
                    } else {
                        //If the CVM is not recognised, the terminal shall set the "Unrecognised CVM" 
                        //bit in the TVR (b7 of byte 3) to 1 and processing continues at step 2.
                        EMVTerminal.getTerminalVerificationResults().setUnrecognisedCVM(true);
                    }
                    
                } else {
                    
                    
                }
                if(EMVTerminal.isCVMRecognized(app, rule)) {
                    
                    if(rule.getConditionCode() == 0x03) {
                        
                    }

                    //If any of the following is true:
                    //- the conditions expressed in the second byte of a CV Rule are not satisfied, or
                    //- data required by the condition (for example, the Application Currency Code 
                    //  or Amount, Authorised) is not present, or
                    //- the CVM Condition Code is outside the range of codes understood 
                    //  by the terminal (which might occur if the terminal application 
                    //  program is at a different version level than the ICC application),
                    //then the terminal shall bypass the rule and proceed to the next.

                    if(!EMVTerminal.isCVMConditionSatisfied(rule)){ //Checks all 3
                        continue;
                    }
                    
                    
                    if(rule.getRule() == CVRule.Rule.FAIL_PROCESSING) {
                        EMVTerminal.getTerminalVerificationResults().setCardholderVerificationWasNotSuccessful(true);
                        return;
                    }
                    
                    
                    //determine whether the terminal supports the CVM
                    if(EMVTerminal.isCVMSupported(rule)){
                        //If the CVM is supported, the terminal shall attempt to perform it
                        
                        if(rule.isPinRelated() && !EMVTerminal.getDoVerifyPinIfRequired()){
                            //If the terminal bypassed PIN entry at the direction of either the merchant or the cardholder:
                            //Terminal shall set the "PIN entry required, PIN pad present, but PIN was not entered" bit in the TVR to 1. 
                            //The terminal shall consider this CVM unsuccessful and shall continue cardholder
                            //verification processing in accordance with the card's CVM List
                            EMVTerminal.getTerminalVerificationResults().setPinEntryRequired_PINPadPresent_ButPINWasNotEntered(true);
                            if(!rule.applySucceedingCVRuleIfThisCVMIsUnsuccessful()) {
                                EMVTerminal.getTerminalVerificationResults().setCardholderVerificationWasNotSuccessful(true);
                                return;
                            }
                            continue;
                        }
                        
                        performCVMRuleProcessing(rule.getRule());

                    } else {
                        if(rule.isPinRelated()){
                            //In case the CVM was PIN-related, then in addition the terminal shall set the 
                            //"PIN entry required and PIN pad not present or not working" bit (b5 of byte 3) of the TVR to 1
                            EMVTerminal.getTerminalVerificationResults().setPinEntryRequiredAndPINPadNotPresentOrNotWorking(true);
                        }
                    }
                } else {
                    //If the CVM is not recognised, the terminal shall set the "Unrecognised CVM" 
                    //bit in the TVR (b7 of byte 3) to 1 and processing continues at step 2.
                    EMVTerminal.getTerminalVerificationResults().setUnrecognisedCVM(true);
                }

                //Step 2
                //The CVM was not recognised, was not supported, or failed.
                //Check if we should try next rule
                if(!rule.applySucceedingCVRuleIfThisCVMIsUnsuccessful()){
                    EMVTerminal.getTerminalVerificationResults().setCardholderVerificationWasNotSuccessful(true);
                    return;
                }

            }
        } finally {
            //When cardholder verification is completed, the terminal shall:
            //- set the CVM Results according to Book 4 section 6.3.4.5 (TODO)
            //- set the Cardholder verification was performed" bit in the TSI to 1.
            app.getTransactionStatusInformation().setCardholderVerificationWasPerformed(true);
        }

        //All cv rules have been processed and failed
        EMVTerminal.getTerminalVerificationResults().setCardholderVerificationWasNotSuccessful(true);
        

    }

    private void performCVMCodeNotSupportedLogic(CVRule rule) {
        
    }
    
    private boolean performCVMRuleProcessing(CVRule.Rule rule) throws TerminalException {
        switch(rule){
            case NO_CVM_REQUIRED:
                return true;
            case FAIL_PROCESSING:
                //If the CVM just processed was "Fail CVM Processing", the terminal shall 
                //set the "Cardholder verification was not successful" bit in the TVR (b8 of byte 3) 
                //to 1 and no further CVMs shall be processed regardless of the 
                //setting of b7 of byte 1 in the first byte of the CV Rule
                EMVTerminal.getTerminalVerificationResults().setCardholderVerificationWasNotSuccessful(true);
                return false;
            case SIGNATURE_ON_PAPER:
                if(EMVTerminal.hasSignatureOnPaper()) {
                    return true;
                }
                break;
            case ENCIPHERED_PIN_VERIFIED_BY_ICC_AND_SIGNATURE_ON_PAPER:
                if(!EMVTerminal.hasSignatureOnPaper() && processVerifyPIN(true)) {
                    return true;
                }
                break;
            case PLAINTEXT_PIN_VERIFIED_BY_ICC_AND_SIGNATURE_ON_PAPER:
                if(EMVTerminal.hasSignatureOnPaper() && processVerifyPIN(false)) {
                    return true;
                }
                break;
            case ENCIPHERED_PIN_VERIFIED_BY_ICC:
                if(processVerifyPIN(true)){
                    return true;
                }
                break;
            case PLAINTEXT_PIN_VERIFIED_BY_ICC:
                if(processVerifyPIN(false)) {
                    return true;
                }
                break;
            case ENCIPHERED_PIN_VERIFIED_ONLINE:
                if(EMVTerminal.allowVerifyEncipheredPinOnline()) {
                    EMVTerminal.getTerminalVerificationResults().setOnlinePINEntered(true);
                    return true;
                }
                break;
            case RESERVED_FOR_USE_BY_THE_INDIVIDUAL_PAYMENT_SYSTEMS:
                //TODO
            case RESERVED_FOR_USE_BY_THE_ISSUER:
                //TODO
            case NOT_AVAILABLE_FOR_USE:
                //TODO fail

        }
        return false;

    }
    
    public void performTransaction() throws TerminalException {
        EMVApplication app = card.getSelectedApplication();
        //verifyPrepareTransactionProcessingPerformed()
        verifyProcessingStateMinimum(app, State.TRANSACTION_PROCESSING_PREPARED);
        
        //(Generate AC)    
        
    
        //10.8 Card Action Analysis (After Generate AC)
    }
    
    /**
     * Terminal risk management is that portion of risk management performed by 
     * the terminal to protect the acquirer, issuer, and system from fraud. 
     * It provides positive issuer authorization for high-value transactions and 
     * ensures that transactions initiated from ICCs go online periodically to 
     * protect against threats that might be undetectable in an offline environment. 
     * 
     * The result of terminal risk management is the setting of appropriate bits in the TVR.
     * 
     * Terminal risk management may be performed at any time after Read 
     * Application Data but before issuing the first GENERATE AC command.
     * 
     */
    private void performTerminalRiskManagement(EMVApplication app) throws TerminalException {
       
        //10.6 Terminal Risk Management
        //  10.6.1 Floor Limits
        //  10.6.2 Random Transaction Selection
        //  10.6.3 Velocity Checking
        
        try {
            checkFloorLimits(app);
        
            checkForRandomTransactionSelection(app);
            
            if(app.getLowerConsecutiveOfflineLimit() != -1 && app.getUpperConsecutiveOfflineLimit() != -1) {
                //If both the Lower Consecutive Offline Limit (tag '9F14') and 
                //Upper Consecutive Offline Limit (tag '9F23') exist, 
                //the terminal shall perform velocity checking.
                performVelocityCheck(app);            
            }
        
        } finally {
            //Upon completion of terminal risk management, the terminal shall set the
            //"Terminal risk management was performed" bit in the TSI to 1
            app.getTransactionStatusInformation().setTerminalRiskManagementWasPerformed(true);
        }

    }
    
    /**
     * 10.6.1 Floor Limits
     * To prevent split sales, the terminal may have a transaction log of approved 
     * transactions stored in the terminal consisting of at least the Application PAN 
     * and transaction amount and possibly the Application PAN Sequence Number and Transaction Date.
     * 
     * @param app
     * @throws TerminalException 
     */
    private void checkFloorLimits(EMVApplication app) throws TerminalException {
        
        //During terminal risk management floor limit checking, the terminal checks 
        //the transaction log (if available) to determine if there is a log entry 
        //with the same Application PAN, and, optionally, the same Application PAN Sequence Number. 
        //If there are several log entries with the same PAN, the terminal selects 
        //the most recent entry. The terminal adds the Amount, Authorised for the 
        //current transaction to the amount stored in the log for that PAN to 
        //determine if the sum exceeds the Terminal Floor Limit. If the sum is 
        //greater than or equal to the Terminal Floor Limit, the terminal shall 
        //set the "Transaction exceeds floor limit" bit in the TVR to 1
        
        if(!app.isTransactionLogProcessed()) {
            checkForTransactionLogRecords();
        }
        
        TransactionLog transactionLog = app.getTransactionLog();
        
        boolean panFound = false;
        if(transactionLog != null && !transactionLog.isEmpty()) {
            List<TransactionLog.Record> logRecords = transactionLog.getRecords();
            for(int i = logRecords.size()-1; i >= 0; i--) {
                TransactionLog.Record logRecord = logRecords.get(i);
                //TODO 
//                if(logRecord.getPAN().equals(app.getPAN()) {
//                    panFound = true;
//                    //add the Amount, Authorised for the current transaction to 
//                    //the amount stored in the log for that PAN to determine if 
//                    //the sum exceeds the Terminal Floor Limit. 
//                    if(sum >= terminalFloorLimit) {
//                        EMVTerminal.getTerminalVerificationResults().setTransactionExceedsFloorLimit(true);
//                    }
//                    
//                    break;
//                }
                
            }
        } 
        if(!panFound){
            //If the terminal does not have a transaction log available or if there 
            //is no log entry with the same PAN, the Amount, Authorised is compared 
            //to the appropriate floor limit. If the amount authorised is equal to 
            //or greater than the floor limit, the terminal sets the 
            //"Transaction exceeds floor limit" bit to 1 in the TVR

            //TODO
//            if(transactionAmount > floorLimit) {
//                EMVTerminal.getTerminalVerificationResults().setTransactionExceedsFloorLimit(true);
//            }
        }
        
    }
    
    /**
     * 10.6.2 Random Transaction Selection
     * 
     * @param app
     * @throws TerminalException 
     */
    private void checkForRandomTransactionSelection(EMVApplication app) throws TerminalException {
        //Not implemented
        
        //If the transaction is selected through the process described in this section, the
        //terminal shall set the "Transaction selected randomly for online processing" bit in
        //the TVR to 1.
//        if(transactionRandomlySelected) {
//            EMVTerminal.getTerminalVerificationResults().setTransactionSelectedRandomlyForOnlineProcessing(true);
//        }
    } 

    /**
     * 10.6.3: Velocity Checking
     * The purpose of velocity checking is to allow an issuer to request that, 
     * after a certain number of consecutive offline transactions (the Lower Consecutive Offline Limit), 
     * transactions should be completed online. 
     * However, if the terminal is incapable of going online, transactions may 
     * still be completed offline until a second (Upper Consecutive Offline Limit) 
     * limit is reached. After the upper limit is reached, the recommendation 
     * of the issuer might be to reject any transaction that cannot be completed online. 
     * Once a transaction has been completed online with successful issuer authentication, 
     * the count begins anew, so that transactions may be processed offline 
     * until the lower limit is again reached.
     * 
     * @param app
     * @throws TerminalException 
     */
    private void performVelocityCheck(EMVApplication app) throws TerminalException {

        //The ATC and Last Online ATC Register shall be read from the ICC using GET DATA commands. 
        readATCData(app);
        
        int atc = app.getATC();
        int lastOnlineAtc = app.getLastOnlineATC();

        if(lastOnlineAtc == 0) {
            EMVTerminal.getTerminalVerificationResults().setNewCard(true);
        }
        
        //If either of the required data objects is not returned by the ICC in response to the GET DATA command, 
        //or if the value of the ATC is less than or equal to the value in the Last Online ATC Register, the terminal shall:
        //- Set both the "Lower consecutive offline limit exceeded" and the "Upper consecutive offline limit exceeded" bits in the TVR to 1.
        //- Not set the "New card" indicator in the TVR unless the Last Online ATC Register is returned and equals zero.
        //- End velocity checking for this transaction.
        if(atc == -1 || lastOnlineAtc == -1 || atc <= lastOnlineAtc){
            EMVTerminal.getTerminalVerificationResults().setLowerConsecutiveOfflineLimitExceeded(true);
            EMVTerminal.getTerminalVerificationResults().setUpperConsecutiveOfflineLimitExceeded(true);
            return;
        }
        
        //If the required data objects are available, the terminal shall compare the 
        //difference between the ATC and the Last Online ATC Register with the 
        //Lower Consecutive Offline Limit to see if the limit has been exceeded. 
        //If the difference is equal to the Lower Consecutive Offline Limit, 
        //this means that the limit has not yet been exceeded. If the limit has been exceeded, 
        //the terminal shall set the "Lower consecutive offline limit exceeded" bit in the TVR to 1 
        //and also compare the difference with the Upper Consecutive Offline Limit 
        //to see if the upper limit has been exceeded. If it has, the terminal shall set the 
        //"Upper consecutive offline limit exceeded" bit in the TVR to 1. 
        //The terminal shall also check the Last Online ATC Register for a zero value. 
        //If it is zero, the terminal shall set the "New card" bit in the TVR to 1.
        
        int diff = atc - lastOnlineAtc;
        if(diff > app.getLowerConsecutiveOfflineLimit()) {
            EMVTerminal.getTerminalVerificationResults().setLowerConsecutiveOfflineLimitExceeded(true);
            
            if(diff > app.getUpperConsecutiveOfflineLimit()) {
                EMVTerminal.getTerminalVerificationResults().setUpperConsecutiveOfflineLimitExceeded(true);
            }
        }
    }
    
    /**
     * 10.7 Terminal Action Analysis
     * 
     * Once terminal risk management and application functions related to a normal 
     * offline transaction have been completed, the terminal makes the first decision 
     * as to whether the transaction should be approved offline, declined offline, or transmitted online.
     * - If the outcome of this decision process is to proceed offline, the 
     *   terminal issues a GENERATE AC command to ask the ICC to return a TC.
     * - If the outcome of the decision is to go online, the terminal issues 
     *   a GENERATE AC command to ask the ICC for an Authorization Request Cryptogram (ARQC).
     * - If the decision is to reject the transaction, the terminal issues 
     *   a GENERATE AC to ask for an Application Authentication Cryptogram (AAC).
     * 
     * An offline decision made here is not final. If the terminal asks for a TC 
     * from the ICC, the ICC, as a result of card risk management, may return an ARQC or AAC
     * 
     * @param app
     * @throws TerminalException 
     */
    private void performTerminalActionAnalysis(EMVApplication app) throws TerminalException {
        //TODO
    }
    
    /**
     * A convenience method for reading ATC data manually
     * 
     */ 
    public void testReadATCData() throws TerminalException {
        EMVApplication app = card.getSelectedApplication();
        readATCData(app);
    }
    
    private void readATCData(EMVApplication app) throws TerminalException {

        byte[] command;
        int SW1;
        int SW2;

        Log.commandHeader("Send GET DATA command to find the Application Transaction Counter (ATC)");
        command = EMVAPDUCommands.getApplicationTransactionCounter();
        CardResponse getDataATCResponse = EMVUtil.sendCmd(terminal, command);

        SW1 = (byte) getDataATCResponse.getSW1();
        SW2 = (byte) getDataATCResponse.getSW2();

        if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
            BERTLV tlv = TLVUtil.getNextTLV(new ByteArrayInputStream(getDataATCResponse.getData()));
            app.setATC(Util.byteToInt(tlv.getValueBytes()[0], tlv.getValueBytes()[1]));
        }

        Log.commandHeader("Send GET DATA command to find the Last Online ATC Register");
        command = EMVAPDUCommands.getLastOnlineATCRegister();
        CardResponse getDataLastOnlineATCRegisterResponse = EMVUtil.sendCmd(terminal, command);

        SW1 = (byte) getDataLastOnlineATCRegisterResponse.getSW1();
        SW2 = (byte) getDataLastOnlineATCRegisterResponse.getSW2();

        if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
            BERTLV tlv = TLVUtil.getNextTLV(new ByteArrayInputStream(getDataLastOnlineATCRegisterResponse.getData()));
            app.setLastOnlineATC(Util.byteToInt(tlv.getValueBytes()[0],
                    tlv.getValueBytes()[1]));
        }
    }

    /**
     * EMV 4.2 Book 3, section 6.5.6
     * The GET CHALLENGE command is used to obtain an unpredictable number from 
     * the ICC for use in a security-related procedure. 
     * The challenge shall be valid only for the next issued command.
     * 
     * Used eg when enciphering the PIN
     * 
     * @return an 8-byte unpredictable number generated by the ICC, 
     *    or a zero length byte array if the card does not support the command or the response data length != 8 
     * @throws TerminalException 
     */
    private byte[] getChallenge() throws TerminalException {

        byte[] command;

        Log.commandHeader("Send GET CHALLENGE command");
        command = EMVAPDUCommands.getChallenge();

        //Parse raw bytes only, no BERTLV
        CardResponse getChallengeResponse = EMVUtil.sendCmdNoParse(terminal, command);
        
        
        if (getChallengeResponse.getSW() == SW.SUCCESS.getSW()) {
            //The data field of the response message should contain an 
            //8-byte unpredictable number generated by the ICC
            byte[] challenge = getChallengeResponse.getData();
            return challenge;
        }
        return new byte[0];
    }

    private boolean processVerifyPIN(boolean encipherPin) throws TerminalException {

        EMVApplication app = card.getSelectedApplication();
        
        if(app.getPINTryCounter() == -1) { //-1 means we have not tried reading the PIN Try Counter before
            readPINTryCounter();
        }
        
        if(app.getPINTryCounter() == 0) {
            Log.debug("PIN Try limit exeeded. Unable to verify PIN.");
            EMVTerminal.getTerminalVerificationResults().setPinTryLimitExceeded(true);
            return false;
        }
        
        byte[] command;
        
        while(app.getPINTryCounter() != 0) {
        
            PasswordCallback pinInput = EMVTerminal.getPinInput();
            
            char[] pin = pinInput.getPassword();
            pinInput.clearPassword();
            
            if(pin == null) { //Input aborted by user or failed
                Log.debug("PIN input aborted or failed.");
                return false;
            }
            
            if(encipherPin) {                
                //Recover the public key to be used for PIN encipherment
                ICCPublicKeyCertificate iccPKCert = app.getICCPublicKeyCertificate();
                if(iccPKCert == null || !iccPKCert.validate() || iccPKCert.getICCPublicKey() == null) {
                    Log.debug("Unable to encipher PIN: ICC Public Key Certificate not valid");
                    return false;
                }
                
                //Get unpredictable number from ICC
                byte[] challenge = getChallenge();
                if(challenge.length != 8) {
                    Log.debug("Unable to encipher PIN: GET CHALLENGE returned response length "+challenge.length);
                    return false;
                }
                
                //TODO encipher PIN
            }
            
            command = EMVAPDUCommands.verifyPIN(pin, !encipherPin);
            Arrays.fill(pin, ' ');
            
            Log.commandHeader("Send VERIFY (PIN) command");

            CardResponse verifyResponse = EMVUtil.sendCmd(terminal, command);

            if (verifyResponse.getSW() == SW.SUCCESS.getSW()) {
                //Try to update PTC
                if(app.getPINTryCounter() != -2) { //-2 means app does not support the command
                    readPINTryCounter();
                }
                return true;
            } else {
                if (verifyResponse.getSW() == SW.COMMAND_NOT_ALLOWED_AUTHENTICATION_METHOD_BLOCKED.getSW()
                        || verifyResponse.getSW() == SW.COMMAND_NOT_ALLOWED_REFERENCE_DATA_INVALIDATED.getSW()) {
                    Log.info("No more retries left. CVM blocked");
                    EMVTerminal.getTerminalVerificationResults().setPinTryLimitExceeded(true);
                    app.setPINTryCounter(0);
                    return false;
                } else if (verifyResponse.getSW1() == (byte) 0x63 && (verifyResponse.getSW2() & (byte)0xF0) == (byte) 0xC0) {
                    int numRetriesLeft = (verifyResponse.getSW2() & 0x0F);
                    Log.info("Wrong PIN. Retries left: "+numRetriesLeft);
                    app.setPINTryCounter(numRetriesLeft);
                } else {
                    String description = SW.getSWDescription(verifyResponse.getSW());
                    Log.info("Application returned unexpected Status: 0x"+Util.short2Hex(verifyResponse.getSW()) 
                            + (description != null && !description.isEmpty()?" ("+description+")":""));
                    return false;
                }
            }
        }
        EMVTerminal.getTerminalVerificationResults().setPinTryLimitExceeded(true);
        return false;
    }

    public void readPINTryCounter() throws TerminalException {

        EMVApplication app = card.getSelectedApplication();

        verifyAppInitialized(app);

        byte[] command;
        int SW1;
        int SW2;

        Log.commandHeader("Send GET DATA command to find the PIN Try Counter");
        command = EMVAPDUCommands.getPINTryConter();
        CardResponse getDataPINTryCounterResponse = EMVUtil.sendCmd(terminal, command);

        SW1 = (byte) getDataPINTryCounterResponse.getSW1();
        SW2 = (byte) getDataPINTryCounterResponse.getSW2();

        if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
            BERTLV tlv = TLVUtil.getNextTLV(new ByteArrayInputStream(getDataPINTryCounterResponse.getData()));
            app.setPINTryCounter(tlv.getValueBytes()[0]);
        } else {
            app.setPINTryCounter(-2);
        }
    }
    
    private void verifyAppInitialized(EMVApplication app) {
        if (app == null) {
            throw new SmartCardException("No application selected. Call selectApplication(Application) and initializeApplicationProcessing() first");
        }
        if (!app.isInitializedOnICC()) {
            throw new SmartCardException("Application " + Util.prettyPrintHexNoWrap(app.getAID().getAIDBytes()) + " not initialized on ICC initializeApplicationProcessing() first");
        }
    }

    private void verifyAllAppRecordsInAFLRead(EMVApplication app) {
        if (app == null) {
            throw new SmartCardException("No application selected. Call selectApplication(Application) and initializeApplicationProcessing() first");
        }
        
        verifyAppInitialized(app);
        
        if (!app.isAllAppRecordsInAFLRead()) {
            throw new SmartCardException("Records indicated in the Application File Locator have not been read.");
        }
    }
    
    private boolean canDDABePerformed(EMVApplication app) {
        if (app == null) {
            return false;
        }
        
        //TODO verify required records/data
        //verifyAllAppRecordsInAFLRead(app);
        
        if (!app.getApplicationInterchangeProfile().isDDASupported()) {
            return false;
        }
        if (app.getICCPublicKeyCertificate() == null) {
            return false;
        }
        //Do not validate ICCPK Certificate here, as we might want to test INTERNAL AUTHENTICATE regardless of they ICCPK's validity
        //(for a production ready EMV terminal, we would want to validate the cert though)
        //TODO validate ICCPublicKey Cert if in production mode

        return true;
    }

    private void internalAuthenticate(EMVApplication app) throws TerminalException {

        if(!canDDABePerformed(app)){
            EMVTerminal.getTerminalVerificationResults().setDDAFailed(true);
            return;
        }
        
        if (app.getSignedDynamicApplicationData() != null) {
            throw new SmartCardException("Signed Dynamic Application Data exists. DDA already performed?");
        }
        
        byte[] command;
        int SW1;
        int SW2;

        Log.commandHeader("Send INTERNAL AUTHENTICATE command");

        byte[] authenticationRelatedData = null; //data according to DDOL

        DOL ddol = app.getDDOL();
        if (ddol != null) {
            authenticationRelatedData = EMVTerminal.constructDOLResponse(ddol, app);
        }
        if (authenticationRelatedData == null) {
            authenticationRelatedData = EMVTerminal.getDefaultDDOLResponse(app);
        }

        command = EMVAPDUCommands.internalAuthenticate(authenticationRelatedData);
        //The data field of the command message contains the authentication-related data proprietary to an application. 
        //It is coded according to the DDOL as defined in Book 2.

        //The response contains the "Signed Dynamic EMVApplication Data"
        //See Table 15, book 2 (page 79)
        CardResponse internalAuthenticateResponse = EMVUtil.sendCmd(terminal, command);

        SW1 = (byte) internalAuthenticateResponse.getSW1();
        SW2 = (byte) internalAuthenticateResponse.getSW2();

        if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
            EMVUtil.processInternalAuthResponse(internalAuthenticateResponse.getData(), authenticationRelatedData, app);
            
        }
    }

    /**
     *
     * @param cryptogram mandatory 8 bytes from issuer
     * @param proprietaryData optional 1-8 bytes (proprietary)
     * @throws TerminalException
     */
    public void externalAuthenticate(byte[] cryptogram, byte[] proprietaryData) throws TerminalException {

        EMVApplication app = card.getSelectedApplication();

        verifyAppInitialized(app);

        byte[] command;

        Log.commandHeader("Send EXTERNAL AUTHENTICATE command");

        command = EMVAPDUCommands.externalAuthenticate(cryptogram, proprietaryData);
        CardResponse externalAuthenticateResponse = EMVUtil.sendCmd(terminal, command);
        //No data field is returned in the response message
        //'9000' indicates a successful execution of the command.
        //'6300' indicates "Issuer authentication failed".

        if (externalAuthenticateResponse.getSW() != SW.SUCCESS.getSW()) {
            if (externalAuthenticateResponse.getSW() == SW.AUTHENTICATION_FAILED.getSW()) {
                throw new SmartCardException("Issuer authentication failed");
            }
            throw new SmartCardException("Unexpected response: " + Util.short2Hex(externalAuthenticateResponse.getSW()));
        }
    }

//
//    80 12
//      80 Cryptogram Information Data
//      01 15
//      AB EA C7 B0 31 10 CE 74
//      06 10 0A 03 A0 00 00 Issuer Application Data (Contains proprietary application data for transmission to the issuer in an online transaction.(
//
//   80 12
//      40 Cryptogram Information Data
//      01 15
//      53 41 D1 18 4D EF 41 A2
//      06 10 0A 03 60 00 00

    public void generateAC(byte[] iccDynamicNumber) throws TerminalException {
        /**
         * p1 &= 0b00111111 = AAC = reject transaction (EMVApplication Authentication Cryptogram)
         * p1 &= 0b10111111 = TC = proceed offline (Transaction Certificate)
         * p1 &= 0b01111111 = ARQC = go online (Authorization Request Cryptogram                                                                            ) +
         * 0x00 = CDA signature not requested
         * 0x10 = CDA signature requested
         */

        EMVApplication app = card.getSelectedApplication();

        verifyAppInitialized(app);

        byte[] command;

        Log.commandHeader("Send GENERATE APPLICATION CRYPTOGRAM command");

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] authorizedAmount = Util.fromHexString("00 00 00 00 00 01");
        byte[] secondaryAmount = Util.fromHexString("00 00 00 00 00 00");
        byte[] tvr = Util.fromHexString("00 00 00 00 00");
        byte[] transactionCurrencyCode = Util.fromHexString("09 78");
        byte[] transactionDate = Util.fromHexString("09 07 30");
        byte[] transactionType = Util.fromHexString("21");
        byte[] terminalUnpredictableNumber = Util.generateRandomBytes(4);
        //iccDynamicNumber
        byte[] dataAuthCode = app.getSignedStaticApplicationData().getDataAuthenticationCode();

        buf.write(authorizedAmount, 0, authorizedAmount.length);
        buf.write(secondaryAmount, 0, secondaryAmount.length);
        buf.write(tvr, 0, tvr.length);
        buf.write(transactionCurrencyCode, 0, transactionCurrencyCode.length);
        buf.write(transactionDate, 0, transactionDate.length);
        buf.write(transactionType, 0, transactionType.length);
        buf.write(terminalUnpredictableNumber, 0, terminalUnpredictableNumber.length);
        buf.write(iccDynamicNumber, 0, iccDynamicNumber.length);
        buf.write(dataAuthCode, 0, dataAuthCode.length);

        //0x40 = TC
        //0x80 = ARQC
        command = EMVAPDUCommands.generateAC((byte) 0x40, buf.toByteArray());
        CardResponse generateACResponse = EMVUtil.sendCmd(terminal, command);
        //'9000' indicates a successful execution of the command.

        if (generateACResponse.getSW() != SW.SUCCESS.getSW()) {
            throw new SmartCardException("Unexpected response: " + Util.short2Hex(generateACResponse.getSW()));
        } else {
            //TODO
            Log.info("TODO GenerateAC success");

            //80 response message template 1 contatenated values
            //77 BER-TLV encoded
        }
    }

    /**
     * This is an optional command that can be issued (when?)
     * 
     * @throws TerminalException 
     */
    public void checkForTransactionLogRecords() throws TerminalException {
        
        EMVApplication app = card.getSelectedApplication();

        verifyAppInitialized(app);

        if(app.isTransactionLogProcessed()) {
            throw new SmartCardException("Transaction Log has already been processed");
        }
        
        app.setTransactionLogProcessed();
        
        byte[] command;
        int SW1;
        int SW2;
        
        //If the Log Entry data element is present in the FCI Issuer Discretionary Data,
        //then get the Log Format (and proceed to read the log records...)
        Log.commandHeader("Send GET DATA command to find the Log Format");
        command = EMVAPDUCommands.getLogFormat();
        CardResponse getDataLogFormatResponse = EMVUtil.sendCmd(terminal, command);

        SW1 = (byte) getDataLogFormatResponse.getSW1();
        SW2 = (byte) getDataLogFormatResponse.getSW2();

        if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
            BERTLV tlv = TLVUtil.getNextTLV(new ByteArrayInputStream(getDataLogFormatResponse.getData()));
            app.setLogFormat(new LogFormat(tlv.getValueBytes()));

            //Log Entry data element should be located in the FCI Issuer Discretionary Data
            //If it is not, then the app does not support transaction logging.
            //But we try to read the Log Entry with GET DATA if not present in FCI
            if (app.getLogEntry() != null) {
                readTransactionLog(app);
            } else {
				Log.commandHeader("Send GET DATA command to find the Log Entry SFI");
				command = EMVAPDUCommands.getData((byte)0x9f, (byte)0x4d);
				CardResponse getDataLogEntryResponse = EMVUtil.sendCmd(terminal, command);

				SW1 = (byte) getDataLogEntryResponse.getSW1();
				SW2 = (byte) getDataLogEntryResponse.getSW2();

        		if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
					app.setLogEntry(new LogEntry(getDataLogEntryResponse.getData()[0], getDataLogEntryResponse.getData()[1]));
					readTransactionLog(app);
				}
			}

        }
    }
    
    /**
     * Based on a patch by Thomas Souvignet -FR-
     */
    private void readTransactionLog(EMVApplication app) throws TerminalException {
        //read all the log records
        LogEntry logEntry = app.getLogEntry();
        int sfi = logEntry.getSFI().getValue();
        for (int recordNum = 1; recordNum <= logEntry.getNumberOfRecords(); recordNum++) {
            Log.commandHeader("Send READ RECORD to read LOG ENTRY SFI " + sfi + " record " + recordNum);

            byte[] command = EMVAPDUCommands.readRecord(recordNum, logEntry.getSFI().getValue());

            CardResponse readAppDataResponse = EMVUtil.sendCmdNoParse(terminal, command);

            byte SW1 = (byte) readAppDataResponse.getSW1();
            byte SW2 = (byte) readAppDataResponse.getSW2();

            if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
                app.addTransactionLogRecord(readAppDataResponse.getData());
            } else if (SW1 == (byte) 0x6a && SW2 == (byte) 0x83) {
                return;
            } else {
                //Any SW1 SW2 other than '9000' passed to the application layer as a result
                //of reading any record shall cause the transaction (not entry log) to be terminated [spec]
                throw new SmartCardException("Reading application data failed for SFI " + sfi + " Record Number: " + recordNum);
            }
        }
    }

    /**
     * This method is only for debugging. Not to be used in normal processing
     * 
     * @throws TerminalException 
     */
    public void testBruteForceRecords() throws TerminalException {

        EMVApplication app = card.getSelectedApplication();

        if (app == null) {
            throw new SmartCardException("No application selected. Call selectApplication(Application) and initializeApplicationProcessing() first");
        }

        byte[] command;
        byte SW1;
        byte SW2;

        Log.commandHeader("Brute force SFI & Record numbers (Send READ RECORD)");

        //Valid SFI: 1 to 30
        //Valid Record numbers: 1 to 255

        int numRecordsFound = 0;

        for (int sfi = 1; sfi <= 30; sfi++) {

            for (int recordNum = 1; recordNum <= 255; recordNum++) {

                command = EMVAPDUCommands.readRecord(recordNum, sfi);

                CardResponse readRecordsResponse = EMVUtil.sendCmd(terminal, command);

                SW1 = (byte) readRecordsResponse.getSW1();
                SW2 = (byte) readRecordsResponse.getSW2();

                if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
//                if (SW1 != (byte) 0x6a && (SW2 != (byte) 0x83 || SW2 != (byte) 0x82)) { //This is used to see if we can get any other responses
                    System.out.println("***** BRUTE FORCE FOUND SOMETHING ***** SFI=" + Util.byte2Hex((byte) sfi) + " File=" + recordNum + " SW=" + Util.short2Hex(readRecordsResponse.getSW()));
                    System.out.println(Util.prettyPrintHex(readRecordsResponse.getData()));
                    EMVUtil.parseAppRecord(readRecordsResponse.getData(), app);
                    numRecordsFound++;
                }
            }
        }
        System.out.println("Number of Records found: " + numRecordsFound);
    }
    
    /**
     * This method is only for debugging. Not to be used in normal processing
     * 
     * Tests whether the application/card supports the GET CHALLENGE command
     * 
     * @return 
     * @throws TerminalException 
     */
    public byte[] testGetChallenge() throws TerminalException {
        EMVApplication app = card.getSelectedApplication();

        verifyAppInitialized(app);
        verifyAllAppRecordsInAFLRead(app);
        
        return getChallenge();
    }
    
    public void testRNGSpeed() throws TerminalException {
        // test RNG speed bits/sec
        int NUM_ROUNDS = 100;
        int numBytes = 0;
        long start = System.nanoTime();
        for(int i=0; i<NUM_ROUNDS; i++){
            
            numBytes += testGetChallenge().length;
        }
        long time = System.nanoTime() - start;
        double secs = time/1000000000;
        int numBits = numBytes*8;

        double bitsPrSec = numBits/secs;
        System.out.println("time: "+time+"ns");
        System.out.println("secs: "+secs);
        System.out.println("numBits: "+numBits);
        System.out.println("Bits/sec: "+bitsPrSec);
    }


    public static void main(String[] args){

    }
}
