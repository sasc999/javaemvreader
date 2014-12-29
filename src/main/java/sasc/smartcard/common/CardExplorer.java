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

import sasc.emv.CA;
import sasc.emv.EMVApplication;
import sasc.emv.EMVSession;
import sasc.emv.EMVTerminal;
import sasc.iso7816.SmartCardException;
import sasc.lookup.ATR_DB;
import sasc.terminal.CardConnection;
import sasc.terminal.TerminalException;
import sasc.terminal.TerminalUtil;
import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class CardExplorer {

    //Declare SmartCard here, so in case some exception is thrown, we can still try to dump all the information we found
    SmartCard smartCard = null;

    public SmartCard getEMVCard(){
        return smartCard;
    }

    public void start() {
        //Add test keys so we can read and validate acquirer test cards (DO NOT use this if validating production cards only!)
        CA.addFromXmlFile("/certificationauthorities_test.xml");

        CardConnection cardConnection = null;
        try {
            cardConnection = TerminalUtil.connect(TerminalUtil.State.CARD_INSERTED);

            if(cardConnection == null){
                Log.debug("TerminalUtil.connect returned null");
                return;
            }
            //TODO check for warm ATR
            //If the ATR received following a cold reset as described in EMV Book 1 section 6.1.3.1 does not
            //conform to the specification in EMV Book 1 section 8, the terminal shall initiate a warm reset
            //and obtain an ATR from the ICC as follows (see Figure 8)

            SessionProcessingEnv env = new SessionProcessingEnv();
            env.setReadMasterFile(true);
            env.setProbeAllKnownAIDs(true);
//            env.setDiscoverTerminalFeatures(true);

            CardSession cardSession = CardSession.createSession(cardConnection, env);

            smartCard = cardSession.initCard();

            EMVSession session = EMVSession.startSession(smartCard, cardConnection);

			//This will override any callback handler set previously (eg by the GUI class)
//            EMVTerminal.setPinCallbackHandler(new CallbackHandler(){
//
//                @Override
//                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
//                    for(Callback callback : callbacks){
//                        if(callback instanceof PasswordCallback) {
//                            PasswordCallback pinCallback = (PasswordCallback)callback;
//                            //Set static PIN, or display input dialog here
//                            char[] pin = new char[]{'1','2','3','4'};
//                            pinCallback.setPassword(pin);
//                            Arrays.fill(pin, ' '); //Zeroize PIN data
//                            return;
//                        }
//                        throw new UnsupportedCallbackException(callback);
//                    }
//                }
//            });


//            AID visaClassicAID = new AID("a0 00 00 00 03 10 10");

            session.initContext();
            for (EMVApplication app : smartCard.getEmvApplications()) {
                try{ //If the processing of this app fails, just skip it
                    session.selectApplication(app);
                    session.initiateApplicationProcessing(); //GET PROCESSING OPTIONS + READ RECORD(s)

                    if (!app.isInitializedOnICC()) {
                        //Skip if GPO failed (might not be a EMV card, or conditions not satisfied)
                        continue;
                    }

                    //Be VERY CAREFUL when setting this, as it WILL block the application if the PIN Try Counter reaches 0
                    //Must be combined with a PIN callback handler
                    EMVTerminal.setDoVerifyPinIfRequired(false);

                    session.prepareTransactionProcessing();

//                    session.performTransaction();


                    //Check if the transaction processing skipped some steps
                    if(app.getATC() == -1 || app.getLastOnlineATC() == -1) {
                        session.testReadATCData(); //ATC, Last Online ATC
                    }
                    //If PIN Try Counter has not been read, try to read it
                    if(app.getPINTryCounter() == -1) {
                        session.readPINTryCounter();
                    }
                    if(!app.isTransactionLogProcessed()) {
                        session.checkForTransactionLogRecords();
                    }

                    //testGetChallenge (see if the app supports generating an unpredictable number)
                    session.testGetChallenge();

                } catch(Exception e) {
                    e.printStackTrace(System.err);
                    Log.info(String.format("Error processing app: %s. Skipping app: %s", e.getMessage(), app.toString()));
                    continue;
                }
            }

            Log.info("\n");
            Log.info("Finished Processing card.");
            Log.info("Now dumping card data in a more readable form:");
            Log.info("\n");
            //See the finally clause
        } catch (TerminalException ex) {
            ex.printStackTrace(System.err);
            Log.info(ex.toString());
        } catch (UnsupportedCardException ex) {
            System.err.println("Unsupported card: " + ex.getMessage());
            Log.info(ex.toString());
            if (cardConnection != null) {
                System.err.println("ATR: " + Util.prettyPrintHexNoWrap(cardConnection.getATR()));
                System.err.println(ATR_DB.searchATR(cardConnection.getATR()));
            }
        } catch (SmartCardException ex) {
            ex.printStackTrace(System.err);
            Log.info(ex.toString());
        } finally {
            if (cardConnection != null){
                try{
                    cardConnection.disconnect(true);
                }catch(TerminalException ex){
                    ex.printStackTrace(System.err);
                }
            }
            if (smartCard != null) {
                try {
                    int indent = 0;
                    Log.getPrintWriter().println("======================================");
                    Log.getPrintWriter().println("             [Smart Card]             ");
                    Log.getPrintWriter().println("======================================");
                    smartCard.dump(Log.getPrintWriter(), indent);
                    Log.getPrintWriter().println("---------------------------------------");
                    Log.getPrintWriter().println("                FINISHED               ");
                    Log.getPrintWriter().println("---------------------------------------");
                    Log.getPrintWriter().flush();
                } catch (RuntimeException ex) {
                    ex.printStackTrace(System.err);
                }
                Log.info("");
            } else if (cardConnection != null) {
                Log.info(new sasc.iso7816.ATR(cardConnection.getATR()).toString());
            }
        }
    }
}
