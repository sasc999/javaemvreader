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

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.jdesktop.application.SingleFrameApplication;
import sasc.emv.EMVApplication;
import sasc.smartcard.common.CardExplorer;
import sasc.smartcard.common.SmartCard;
import sasc.emv.EMVTerminal;
import sasc.util.Log;

/**
 *
 * @author sasc
 */
public class GUI extends SingleFrameApplication {

    JTextArea console;

    @Override
    protected void startup() {
        JFrame mainFrame = this.getMainFrame();
        mainFrame.setName("mainFrame");

        console = new JTextArea("");

        redirectSystemStreams();

        JScrollPane scrollPane = new JScrollPane(console);
        console.setName("console");
        show(scrollPane);

        this.getMainFrame().setSize(800, 600);
        // Get the size of the screen
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

        // Determine the new location of the JFrame
        int w = mainFrame.getSize().width;
        int h = mainFrame.getSize().height;
        int x = (dim.width - w) / 2;
        int y = (dim.height - h) / 2;

        // Move the JFrame
        mainFrame.setLocation(x, y);

        new Thread(new ExplorerRunner()).start();
    }

    public void addText(String text) {
        console.append(text);
    }

    private void updateTextArea(final String text) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                console.append(text);
            }
        });
    }

    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {

            @Override
            public void write(final int b) throws IOException {
                updateTextArea(String.valueOf((char) b));
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                updateTextArea(new String(b, off, len));
            }

            @Override
            public void write(byte[] b) throws IOException {
                write(b, 0, b.length);
            }
        };

        Log.setPrintWriter(new PrintWriter(new PrintStream(out, true)));
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));

    }

    private class ExplorerRunner implements Runnable {

        @Override
        public void run() {
            EMVTerminal.setPinCallbackHandler(new PinCallbackHandlerGui());
            CardExplorer explorer = new CardExplorer();
            try {
                explorer.start();
            } catch (Exception ex) {
                StringWriter st = new StringWriter();
                ex.printStackTrace(new PrintWriter(st));
                console.append(st.toString());
            } finally {
                //Show submit feedback dialogue
                boolean foundUnhandledRecords = false;
                SmartCard card = explorer.getEMVCard();
                if(card != null){
                    if(card.getUnhandledRecords().size() > 0){
                        foundUnhandledRecords = true;
                    }
                    for(EMVApplication app : card.getEmvApplications()){
                        if(app != null && app.getUnknownRecords().size() > 0){
                            foundUnhandledRecords = true;
                        }
                    }
                }

                if (!console.getText().contains("Finished Processing card.") 
                        || console.getText().contains("Error processing app")) {
                    //Assume something failed. Show Popup with option to send email
                    submitFeedback("[JavaEMVReader-BUGREPORT]", "Error", "Something failed. Would you like to send an email report?");
                }else if(foundUnhandledRecords){
                    submitFeedback("[JavaEMVReader-UNKNOWN-RECORDS]", "Unknown Record(s)", "Found unknown records. Would you like to send an email report?");
                }
            }
        }
    }
//    public static void main(String[] args) {
//        org.jdesktop.application.Application.launch(GUI.class, args);
//    }

    private void submitFeedback(String subject, String dialogTitle, String dialogText) {
        
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.MAIL)) {
                int choice = JOptionPane.showConfirmDialog(console.getRootPane(), dialogText, dialogTitle, JOptionPane.OK_CANCEL_OPTION);
                if (choice == JOptionPane.OK_OPTION) {
                    try {
                        desktop.mail(new URI("mailto", getEAddr() + "?SUBJECT="+subject+"&BODY=(Please also include the complete output from JavaEMVReader, so we can understand what caused the problem)", null));
                    } catch (Exception ex) {
                        StringWriter st = new StringWriter();
                        ex.printStackTrace(new PrintWriter(st));
                        console.append(st.toString());
                    }
                }

            }
        }
    }

    private static String getEAddr() {
        //Damn bots...
        StringBuilder sb = new StringBuilder();
        String _a = "9";
        String _b = "g";
        String _c = "a";
        String _d = ".";
        String _e = "c";
        String _f = "@";
        String _g = "m";
        String _h = "s";
        String _i = "l";
        String _j = "i";
        String _k = "o";
        sb.append(_h).append(_c).append(_h).append(_e).append(_a).append(_a);
        sb.append(_a).append(_f).append(_b).append(_g).append(_c).append(_j);
        sb.append(_i).append(_d).append(_e).append(_k).append(_g);
        return sb.toString();
    }
    
    class PinCallbackHandlerGui implements CallbackHandler {
        
//        private static final char ZEROIZE_CHAR = ' '; //(char)0;
        
        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for(Callback callback : callbacks){
                if(callback instanceof PasswordCallback) {
                    PasswordCallback pcall = (PasswordCallback)callback;
                    
                    JPasswordField passwordField = new JPasswordField();

                    if (!pcall.isEchoOn()) {
                        passwordField.setEchoChar('*');
                    }

                    int okCancel = JOptionPane.showConfirmDialog(null, passwordField, pcall.getPrompt(), 
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                    if (okCancel == JOptionPane.OK_OPTION) {
                        pcall.setPassword(passwordField.getPassword());//Makes a copy of the array
                    } else {
                        pcall.setPassword(null); //Input aborted by the user
                    }
                    passwordField.setText(null); //Attempt to clear the pin
                    return;
                } else {
                    throw new UnsupportedCallbackException(callback, "Only PasswordCallback is supported, but found "+callback.getClass().getName());
                }
            }
        }
    }
}
