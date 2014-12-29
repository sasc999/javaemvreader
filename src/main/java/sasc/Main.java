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
package sasc;

import sasc.smartcard.common.CardExplorer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import sasc.terminal.Terminal;
import sasc.terminal.TerminalAPIManager;
import sasc.terminal.TerminalException;
import sasc.terminal.TerminalProvider;

/**
 *
 * @author sasc
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        //Default values
        boolean noGUI = Boolean.getBoolean("java.awt.headless");
        boolean emulate = false;
        boolean listTerminals = false;
        boolean verbose = false;

        //Commons CLI
        //http://commons.apache.org/cli/usage.html

        Option helpOption = new Option("help", "print this message");
        Option noGUIOption = new Option("noGUI", "use command line version");
        Option emulateOption = new Option("emulate", "emulate communication with an EMV card");
        Option listTerminalsOption = new Option("listTerminals", "list all available terminals");
        Option terminalOption = new Option("terminal", "the name of the terminal to use");
        Option verboseOption = new Option("verbose", "print debug messages");

        Options options = new Options();

        options.addOption(helpOption);
        options.addOption(noGUIOption);
        options.addOption(emulateOption);
        options.addOption(listTerminalsOption);
        options.addOption(terminalOption);
        options.addOption(verboseOption);

        // create the cmd line parser
        CommandLineParser parser = new GnuParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("help")) {
                // automatically generate the help statement
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("BLABLA", options, true);
                System.exit(0);
            }
            if (line.hasOption("listTerminals")) {
                listTerminals = true;
            }

            if (line.hasOption("noGUI")) {
                noGUI = true;
            }
            if (line.hasOption("emulate")) {
                emulate = true;
            }
            if (line.hasOption("verbose")) {
                verbose = true;
            }
        } catch (ParseException ex) {
            // oops, something went wrong
            System.err.println("Parsing failed.  Reason: " + ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(-1);
        }

        if (listTerminals) {
            try{
                TerminalProvider terminalProvider = TerminalAPIManager.getProvider(TerminalAPIManager.SelectionPolicy.ANY_PROVIDER);
                for(Terminal terminal : terminalProvider.listTerminals()){
                    System.out.println(terminal.getTerminalInfo());
                }
                System.exit(0);

            }catch(TerminalException ex){
                ex.printStackTrace(System.err);
                System.exit(-1);
            }
        }

        if (emulate) {
            try{
                CardEmulatorMain.main(null);
                System.exit(0);
            }catch(TerminalException ex){
                ex.printStackTrace(System.err);
                System.exit(-1);
            }
        } 

        if (noGUI) {
            //No Swing/GUI
            new CardExplorer().start();
        } else {
            // Create swing app using appframework
            // http://java.dzone.com/news/jsr-296-end-jframe
            // https://appframework.dev.java.net/
            org.jdesktop.application.Application.launch(GUI.class, args);
        }
    }
}
