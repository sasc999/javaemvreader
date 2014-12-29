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
package sasc.util;

import sasc.terminal.Terminal;
import sasc.terminal.TerminalAPIManager;
import sasc.terminal.TerminalException;
import sasc.terminal.TerminalProvider;

/**
 *
 * @author sasc
 */
public class TerminalEnumerator {

    public static void list() {
        try {
            TerminalProvider terminalProvider = TerminalAPIManager.getProvider(TerminalAPIManager.SelectionPolicy.ANY_PROVIDER);
            for (Terminal terminal : terminalProvider.listTerminals()) {
                System.out.println(terminal.getTerminalInfo());
                if("Yubico Yubikey NEO OTP+CCID 2".equals(terminal.getName())){
                    System.out.println(Util.prettyPrintHexNoWrap(terminal.connect().getATR()));
                }
            }
            System.exit(0);

        } catch (TerminalException ex) {
            ex.printStackTrace(System.err);
            System.exit(-1);
        }
    }
    
    public static void main(String[] args){
        list();
    }
}
