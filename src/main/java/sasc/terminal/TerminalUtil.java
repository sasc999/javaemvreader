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
package sasc.terminal;

import sasc.smartcard.common.Context;
import sasc.util.BuildProperties;
import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class TerminalUtil {

	public enum State{
		CARD_PRESENT, CARD_INSERTED
	}

    public static CardConnection connect(State state) throws TerminalException {
        CardConnection cardConnection = null;
        Context.init();
        TerminalProvider terminalProvider = TerminalAPIManager.getProvider(TerminalAPIManager.SelectionPolicy.ANY_PROVIDER);
        Log.info(BuildProperties.getProperty("APP_NAME", "JER") + " built on " + BuildProperties.getProperty("BUILD_TIMESTAMP", "N/A"));
        Log.info("Java " + System.getProperty("java.version") + " on " + System.getProperty("os.name"));

        while (true) {
            if (terminalProvider.listTerminals().isEmpty()) {
                Log.info("No smart card readers found. Please attach readers(s)");

            }
            while (terminalProvider.listTerminals().isEmpty()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    Log.debug(ex.toString());
                    Thread.currentThread().interrupt();
                    throw new TerminalException(ex);
                }
            }
            Log.info("Please insert a Smart Card into any attached reader.");
            try {
				switch(state){
					case CARD_INSERTED:
                		cardConnection = terminalProvider.connectAnyTerminal(); //Waits for card inserted
                		break;
                    case CARD_PRESENT:
                        cardConnection = terminalProvider.connectAnyTerminalWithCardPresent("*");
                        break;
				}

                break; // Outer while
            } catch (NoTerminalsAvailableException ntex) {
                Log.debug(Util.getStackTrace(ntex));
                //All Terminals were removed while waiting for card
                //go back and try again
            }
        }
        if(cardConnection == null) { //eg InterruptedException
            return null;
        }
        Log.info("OK, card found");
        Log.debug("ATR: " + Util.prettyPrintHexNoWrap(cardConnection.getATR()));
        Log.info("Using terminal: " + cardConnection.getTerminal().getName());
        return cardConnection;

    }
}
