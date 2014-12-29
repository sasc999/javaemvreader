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

import sasc.terminal.smartcardio.SmartcardioTerminalProvider;

/**
 *
 * @author sasc
 */
public class TerminalAPIManager {

    public enum SelectionPolicy{
        ANY_PROVIDER("Any Provider"), SMARTCARD_IO_API("SmartcardIO API");

        private String name;
        private SelectionPolicy(String name){ //Strategy
            this.name = name;
        }

        public String getPolicyName(){
            return name;
        }
    }

    public static TerminalProvider getProvider(SelectionPolicy choice) throws TerminalException{
        boolean findAnyProvider = false;
        switch(choice){
            case ANY_PROVIDER:
                findAnyProvider = true;
                //fall through (providers are listed in priority)
            case SMARTCARD_IO_API:
                if(SmartcardioTerminalProvider.isSmartcardioAvailable()){
                    return new SmartcardioTerminalProvider();
                }
                if(!findAnyProvider){
                    throw new TerminalException("Provider \"SmartcardIO API\" not available");
                }
            default:
                throw new TerminalException("No provider available");
        }
    }

    public static boolean checkAvailability(SelectionPolicy choice){
//        switch(choice){
//            case SMARTCARD_IO_API:
//                return SmartcardioTerminalProvider.isSmartcardioAvailable();
//            case ANY_PROVIDER:
//                //TODO
//        }
//        return false;
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private TerminalAPIManager(){
        //Do not instantiate
    }
}
