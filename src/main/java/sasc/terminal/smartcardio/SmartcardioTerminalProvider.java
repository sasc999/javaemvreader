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
package sasc.terminal.smartcardio;

import java.util.List;
import sasc.terminal.CardConnection;
import sasc.terminal.Terminal;
import sasc.terminal.TerminalException;
import sasc.terminal.TerminalProvider;

/**
 * Reflection Wrapper
 * 
 * @author sasc
 */
public class SmartcardioTerminalProvider implements TerminalProvider {

    private static boolean isSmartcardIOAvailable = false;
    private static TerminalProvider terminalProvider = null;
    private static final String implementationClassName = SmartcardioTerminalProvider.class.getName() + "Impl";

    //Reflection stuff:
    // http://www.jroller.com/eu/entry/dealing_with_api_compatibility
    // http://wiki.forum.nokia.com/index.php/How_to_use_an_optional_API_in_Java_ME
    static {

        //Hack:
        //since the SmartcardIO classes have not been loaded yet, we can set these
        //system properties here (and not depend on the user setting them on the Command Line):
        System.setProperty("sun.security.smartcardio.t0GetResponse", "false");
        System.setProperty("sun.security.smartcardio.t1GetResponse", "false");

        //call SmartcardIO via reflection, since it might not be available on all platforms
        try {
            // this will throw an exception if "JSR-268 Java Smart Card I/O API" is missing
            Class.forName("javax.smartcardio.TerminalFactory");
            Class c = Class.forName(implementationClassName);
            terminalProvider = (TerminalProvider) (c.newInstance());
            isSmartcardIOAvailable = true;
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace(System.err);
        } catch (InstantiationException ex) {
            ex.printStackTrace(System.err);
        } catch (IllegalAccessException ex) {
            ex.printStackTrace(System.err);
        }
    }
    
//    static void setTerminalProvider(String newName){
//        implementationClassName = newName;
//    }

    public static boolean isSmartcardioAvailable() {
        return isSmartcardIOAvailable;
    }

    @Override
    public List<Terminal> listTerminals() throws TerminalException {
        return terminalProvider.listTerminals();
    }

    @Override
    public CardConnection connectAnyTerminal() throws TerminalException {
        return terminalProvider.connectAnyTerminal();
    }
    
    @Override
    public CardConnection connectAnyTerminalWithCardPresent(String protocol) throws TerminalException {
        return terminalProvider.connectAnyTerminalWithCardPresent(protocol);
    }
    
    @Override
    public CardConnection connectAnyTerminal(String protocol) throws TerminalException {
        return terminalProvider.connectAnyTerminal(protocol);
    }

    @Override
    public CardConnection connectTerminal(String name) throws TerminalException {
        return terminalProvider.connectTerminal(name);
    }

    @Override
    public CardConnection connectTerminal(int index) throws TerminalException {
        return terminalProvider.connectTerminal(index);
    }

    @Override
    public String getProviderInfo() {
        return terminalProvider.getProviderInfo();
    }
}
