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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import nanoxml.XMLElement;
import sasc.iso7816.AID;
import sasc.emv.EMVSession;
import sasc.util.Log;
import sasc.emv.SW;
import sasc.smartcard.common.SessionProcessingEnv;
import sasc.terminal.CardResponse;
import sasc.terminal.Terminal;
import sasc.terminal.TerminalException;
import sasc.terminal.CardConnection;
import sasc.util.Util;

/**
 * An ICC emulator that uses data loaded from XML-file
 *
 * Emulate the external behavior of a Smart Card.
 *
 * @author sasc
 */
public class CardEmulator implements CardConnection {

    private final static byte[] SELECT_DDF_PSE = Util.fromHexString("00 A4 04 00 0E 31 50 41 59 2E 53 59 53 2E 44 44 46 30 31");
    private final static byte[] SELECT_MASTER_FILE = Util.fromHexString("00 A4 00 00 00");
    Card card = new Card();

    public CardEmulator(String filename) throws TerminalException {
        _initFromFile(filename);
    }

    @Override
    public void resetCard() throws TerminalException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getProtocol() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public byte[] transmitControlCommand(int controlCode, byte[] data) throws TerminalException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //Some simple data containers
    private class Card {

        Application selectedApp = null;
        byte[] atr = null;
        byte[] masterFile = null;
        byte[] ddf = null;
        Map<Integer, File> filesMap = null;
        Map<AID, Application> applicationsMap = new LinkedHashMap<AID, Application>();
    }

    private class Application {

        AID aid = null;
        int pin = -1;
        int pinTryCounter = -1;
        int atc = -1;
        int lastOnlineATC = -1;
        byte[] logFormat = null;
        byte[] getProcessingOpts = null;
        byte[] adf = null;
        Map<Integer, File> filesMap = null;
    }

    private class File {

        int sfi = -1;
        Map<Integer, Record> recordsMap = new LinkedHashMap<Integer, Record>();
    }

    private class Record {

        byte[] data = null;
    }

    private void _initFromFile(String filename) {
        try {
            XMLElement emvCardElement = new XMLElement();
            emvCardElement.parseFromReader(new InputStreamReader(Util.loadResource(CardEmulator.class, filename)));

            if (!"EMVCard".equalsIgnoreCase(emvCardElement.getName())) {
                throw new RuntimeException("Unexpected Root Element: <" + emvCardElement.getName() + "> . Expected <EMVCard>");
            }
            for (Object emvChildObject : emvCardElement.getChildren()) {
                XMLElement emvCardChildElement = (XMLElement) emvChildObject;
                String emvCardChildElementName = emvCardChildElement.getName();
                if ("ATR".equalsIgnoreCase(emvCardChildElementName)) {
                    card.atr = Util.fromHexString(Util.removeCRLFTab(emvCardChildElement.getContent().trim()));
                } else if ("MasterFile".equalsIgnoreCase(emvCardChildElementName)) {
                    card.masterFile = Util.fromHexString(Util.removeCRLFTab(emvCardChildElement.getContent().trim()));
                } else if ("DirectoryDefinitionFile".equalsIgnoreCase(emvCardChildElementName)) {
                    card.ddf = Util.fromHexString(Util.removeCRLFTab(emvCardChildElement.getContent().trim()));
                } else if ("Files".equalsIgnoreCase(emvCardChildElementName)) {
                    card.filesMap = parseFilesElement(emvCardChildElement);
                } else if ("Applications".equalsIgnoreCase(emvCardChildElementName)) {
                    card.applicationsMap = parseApplicationsElement(emvCardChildElement);
                } else {
                    throw new RuntimeException("Unexpected XML Element: <" + emvCardChildElementName + "> : " + emvCardChildElement);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    private Map<Integer, File> parseFilesElement(XMLElement filesElement) {
        Map<Integer, File> map = new LinkedHashMap<Integer, File>();
        for (Object efObject : filesElement.getChildren()) {
            XMLElement efElement = (XMLElement) efObject;
            String efElementName = efElement.getName();
            if ("ElementaryFile".equalsIgnoreCase(efElementName)) {
                File file = new File();
                file.sfi = efElement.getIntAttribute("sfi");
                map.put(Integer.valueOf(file.sfi), file);
                for (Object recordObject : efElement.getChildren()) {
                    XMLElement recordElement = (XMLElement) recordObject;
                    String recordElementName = recordElement.getName();
                    if ("Record".equalsIgnoreCase(recordElementName)) {
                        int recordNumber = recordElement.getIntAttribute("number");
                        Record record = new Record();
                        record.data = Util.fromHexString(Util.removeCRLFTab(recordElement.getContent().trim()));
                        file.recordsMap.put(Integer.valueOf(recordNumber), record);
                    } else {
                        throw new RuntimeException("Unexpected XML Element: <" + recordElementName + "> : " + recordElement);
                    }
                }
            } else {
                throw new RuntimeException("Unexpected XML Element: <" + efElementName + "> : " + efElement);
            }
        }
        return map;
    }

    private Map<AID, Application> parseApplicationsElement(XMLElement applicationsElement) {
        Map<AID, Application> map = new LinkedHashMap<AID, Application>();
        for (Object appObject : applicationsElement.getChildren()) {
            XMLElement appElement = (XMLElement) appObject;
            String appElementName = appElement.getName();
            if ("Application".equalsIgnoreCase(appElementName)) {
                Application app = new Application();
                AID aid = new AID(appElement.getStringAttribute("AID"));
                app.aid = aid;
                map.put(aid, app);
                for (Object appChildObject : appElement.getChildren()) {
                    XMLElement appChildElement = (XMLElement) appChildObject;
                    String appChildElementName = appChildElement.getName();
                    if ("PIN".equalsIgnoreCase(appChildElementName)) {
                        app.pin = Integer.parseInt(appChildElement.getContent().trim());
                    } else if ("ApplicationDefinitionFile".equalsIgnoreCase(appChildElementName)) {
                        app.adf = Util.fromHexString(Util.removeCRLFTab(appChildElement.getContent().trim()));
                    } else if ("GetDataElements".equalsIgnoreCase(appChildElementName)) {
                        parseGetDataElement(appChildElement, app);
                    } else if ("GetProcessingOptions".equalsIgnoreCase(appChildElementName)) {
                        app.getProcessingOpts = Util.fromHexString(Util.removeCRLFTab(appChildElement.getContent().trim()));
                    } else if ("Files".equalsIgnoreCase(appChildElementName)) {
                        app.filesMap = parseFilesElement(appChildElement);
                    } else {
                        throw new RuntimeException("Unexpected XML Element: <" + appChildElementName + "> : " + appChildElement);
                    }
                }
            } else {
                throw new RuntimeException("Unexpected XML Element: <" + appElementName + "> : " + appElement);
            }
        }
        return map;
    }

    private void parseGetDataElement(XMLElement getDataElement, Application app) {
        for (Object getDataChildObject : getDataElement.getChildren()) {
            XMLElement getDataChildElement = (XMLElement) getDataChildObject;
            String getDataChildElementName = getDataChildElement.getName();
            if ("PINTryCounter".equalsIgnoreCase(getDataChildElementName)) {
                app.pinTryCounter = Util.byteArrayToInt(Util.fromHexString(getDataChildElement.getContent().trim()));
            } else if ("ATC".equalsIgnoreCase(getDataChildElementName)) {
                app.atc = Util.byteArrayToInt(Util.fromHexString(getDataChildElement.getContent().trim()));
            } else if ("LastOnlineATC".equalsIgnoreCase(getDataChildElementName)) {
                app.lastOnlineATC = Util.byteArrayToInt(Util.fromHexString(getDataChildElement.getContent().trim()));
            } else if ("LogFormat".equalsIgnoreCase(getDataChildElementName)) {
                app.logFormat = Util.fromHexString(getDataChildElement.getContent().trim());
            } else {
                throw new RuntimeException("Unexpected XML Element: <" + getDataChildElementName + "> : " + getDataChildElement);
            }
        }
    }

    private static boolean hasLe(byte[] cmd){
        if(cmd.length < 5){
            return false;
        }

        if(cmd.length == 5){
            return true;
        }

        if(Util.byteToInt(cmd[4]) == cmd.length-5-1){
            return true;
        }
        return false;
    }

    @Override
    public CardResponse transmit(byte[] cmd) throws TerminalException {
        CardResponse response = null;

        if(hasLe(cmd)){ //Strip Le
            byte[] tmp = new byte[cmd.length-1];
            System.arraycopy(cmd, 0, tmp, 0, tmp.length);
            cmd = tmp;
        }

        String cmdStr = Util.byteArrayToHexString(cmd).trim().toUpperCase();

        byte cls = cmd[0];
        byte ins = cmd[1];
        byte p1 = cmd[2];
        byte p2 = cmd[3];

        Log.debug("Emulator.transmit() cmdStr: " + cmdStr);

        byte[] responseBytes = null;

        switch (cls & (byte) 0xF0) { //High nibble MASK
            case (byte) 0x00: //0x
                switch (ins) {
                    case (byte) 0xA4: //SELECT
                        responseBytes = processSelect(cmd);
                        break;
                    case (byte) 0xB2: //READ RECORD
                        responseBytes = processReadRecord(cmd);
                        break;
                    case (byte) 0x20: //VERIFY
                        responseBytes = processVerify(cmd);
                        break;
                    default:
                        throw new RuntimeException("INS " + Util.byte2Hex(ins) + " not implemented yet. cmd=" + cmdStr);
                }
                break;
            case (byte) 0x80: //8x
                switch (ins) {
                    case (byte) 0xA8: //GET PROCESSING OPTS
                        responseBytes = processGetProcessingOpts(cmd);
                        break;
                    case (byte) 0xCA: //GET DATA
                        responseBytes = processGetData(cmd);
                        break;
                    default:
                        throw new RuntimeException("INS " + Util.byte2Hex(ins) + " not implemented yet. cmd=" + cmdStr);
                }
                break;
            case (byte) 0xF0:
                if(cls == (byte) 0xFF){
                    responseBytes = new byte[]{0x67, 0x00};
                    break;
                }
            case (byte) 0x90: //9x
//                break;
            case (byte) 0xE0: //Ex
//                break;
            default:
                throw new RuntimeException("CLS " + Util.byte2Hex(cls) + " not implemented yet. cmd=" + cmdStr);
        }

        if (responseBytes == null) {
            throw new RuntimeException("No response found for cmd: " + cmdStr + ". AID=" + card.selectedApp.aid);
        }

        Log.debug("Emulator response:: " + Util.prettyPrintHex(responseBytes));

        response = new CardResponseImpl(responseBytes);

        return response;
    }

    private byte[] processSelect(byte[] cmd) {
        if (Arrays.equals(cmd, SELECT_MASTER_FILE)) {
            if (card.masterFile != null) {
                return createResponse(card.masterFile, SW.SUCCESS);
            } else {
                return createResponse(null, SW.INSTRUCTION_CODE_NOT_SUPPORTED_OR_INVALID);
            }
        }
        if (cmd.length <= 5){ //Zero length AID
            return createResponse(null, SW.FILE_OR_APPLICATION_NOT_FOUND);
        }
        if (Arrays.equals(cmd, SELECT_DDF_PSE)) {
            return createResponse(card.ddf, SW.SUCCESS);
        }
        //Assume SELECT APPLICATION
        AID aid = new AID(getDataBytes(cmd));
        if (card.applicationsMap.containsKey(aid)) {
            card.selectedApp = card.applicationsMap.get(aid);
            return createResponse(card.selectedApp.adf, SW.SUCCESS);
        } else {
            return createResponse(null, SW.COMMAND_NOT_ALLOWED_CONDITIONS_OF_USE_NOT_SATISFIED); //TODO check what SW to return
        }
    }

    private byte[] processReadRecord(byte[] cmd) {
        int recordNumber = Integer.valueOf(cmd[2]);
        int sfi = cmd[3] >>> 3 & 0x1F;
        if (card.selectedApp != null) {
            if (card.selectedApp.filesMap.containsKey(sfi)) {
                File file = card.selectedApp.filesMap.get(sfi);
                if (file.recordsMap.containsKey(recordNumber)) {
                    return createResponse(file.recordsMap.get(recordNumber).data, SW.SUCCESS);
                } else {
                    return createResponse(null, SW.RECORD_NOT_FOUND);
                }
            } else {
                createResponse(null, SW.FILE_OR_APPLICATION_NOT_FOUND);
            }
        } else {
            if (card.filesMap.containsKey(sfi)) {
                File file = card.filesMap.get(sfi);
                if (file.recordsMap.containsKey(recordNumber)) {
                    return createResponse(file.recordsMap.get(recordNumber).data, SW.SUCCESS);
                } else {
                    return createResponse(null, SW.RECORD_NOT_FOUND);
                }
            } else {
                createResponse(null, SW.FILE_OR_APPLICATION_NOT_FOUND);
            }
        }

        return null;
    }

    private byte[] processGetData(byte[] cmd) {
        if (cmd[2] != (byte) 0x9F || cmd.length > 5 || card.selectedApp == null) {
            return createResponse(null, SW.INSTRUCTION_CODE_NOT_SUPPORTED_OR_INVALID); //TODO check correct SW
        }
        switch (cmd[3]) {
            case (byte) 0x36: //ATC
                if (card.selectedApp.atc != -1) {
                    byte[] responseBytes = new byte[5];
                    responseBytes[0] = (byte) 0x9f;
                    responseBytes[1] = (byte) 0x36;
                    responseBytes[2] = (byte) 0x02;

                    byte[] atcBytes = Util.intToByteArray4(card.selectedApp.atc); //Returns an array of length 4
                    System.arraycopy(atcBytes, 2, responseBytes, 3, 2); //We only want the last 2 elements of the array

                    return createResponse(responseBytes, SW.SUCCESS);
                } else {
                    return createResponse(null, SW.FUNCTION_NOT_SUPPORTED);
                }
            case (byte) 0x13: //Last Online ATC
                if (card.selectedApp.lastOnlineATC != -1) {
                    byte[] responseBytes = new byte[5];
                    responseBytes[0] = (byte) 0x9f;
                    responseBytes[1] = (byte) 0x13;
                    responseBytes[2] = (byte) 0x02;

                    byte[] lastOnlineATCBytes = Util.intToByteArray4(card.selectedApp.lastOnlineATC); //Returns an array of length 4
                    System.arraycopy(lastOnlineATCBytes, 2, responseBytes, 3, 2); //We only want the last 2 elements of the array

                    return createResponse(responseBytes, SW.SUCCESS);
                } else {
                    return createResponse(null, SW.FUNCTION_NOT_SUPPORTED);
                }

            case (byte) 0x17: //PIN Try Counter
                if (card.selectedApp.pinTryCounter != -1) {
                    byte[] responseBytes = new byte[4];
                    responseBytes[0] = (byte) 0x9f;
                    responseBytes[1] = (byte) 0x17;
                    responseBytes[2] = (byte) 0x01;
                    responseBytes[3] = (byte) Util.intToByteArray(card.selectedApp.pinTryCounter)[0];
                    return createResponse(responseBytes, SW.SUCCESS);
                } else {
                    return createResponse(null, SW.FUNCTION_NOT_SUPPORTED);
                }

            case (byte) 0x4F: //Log Format
                if (card.selectedApp.logFormat != null) {
                    return createResponse(card.selectedApp.logFormat, SW.SUCCESS);
                } else {
                    return createResponse(null, SW.FUNCTION_NOT_SUPPORTED);
                }

            default:
                return createResponse(null, SW.FUNCTION_NOT_SUPPORTED);

        }
    }

    private byte[] processGetProcessingOpts(byte[] cmd) {
        if (card.selectedApp == null) {
            return createResponse(null, SW.COMMAND_NOT_ALLOWED_CONDITIONS_OF_USE_NOT_SATISFIED); //TODO check correct SW
        }
        return createResponse(card.selectedApp.getProcessingOpts, SW.SUCCESS);
    }

    private byte[] processVerify(byte[] cmd) {
        if (card.selectedApp == null) {
            return createResponse(null, SW.COMMAND_NOT_ALLOWED_CONDITIONS_OF_USE_NOT_SATISFIED); //TODO check correct SW
        }
        if (card.selectedApp.pinTryCounter == 0){
            return createResponse(null, SW.COMMAND_NOT_ALLOWED_AUTHENTICATION_METHOD_BLOCKED);
        }
        byte[] pinBlock = null;
        switch (cmd[3]) { //P2 Qualifier
            case (byte) 0x80: //Plaintext PIN
                pinBlock = getDataBytes(cmd);
                break;
            case (byte) 0x88: //Enciphered PIN
                //TODO decipher data
                throw new UnsupportedOperationException("Emulation of 'VERIFY Enciphered PIN' not implemented yet");

        }
        if ((pinBlock[0] & 0xF0) != 0x20) { //Control Field
            return createResponse(null, SW.INSTRUCTION_CODE_NOT_SUPPORTED_OR_INVALID);
        }
        int pinLength = pinBlock[0] & 0x0F;
        boolean highNibble = true; //Alternate between high and low nibble
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < pinLength; i++) { //Put each PIN digit into its own nibble
            int pos = i / 2;
            if (highNibble) {
                buf.append(String.valueOf(pinBlock[1 + pos] >>> 4));
            } else {
                buf.append(String.valueOf(pinBlock[1 + pos] & 0x0F));
            }
//            System.out.println(buf.toString());
            highNibble = !highNibble;
        }
        if (buf.toString().equals(String.valueOf(card.selectedApp.pin))) {
            return new byte[]{(byte) 0x90, (byte) 0x00};
        } else {
            //When for the currently selected application the comparison between
            //the Transaction PIN Data and the reference PIN data performed by
            //the VERIFY command fails, the ICC shall return SW2 = 'Cx', where 'x'
            //represents the number of retries still possible.
            //When the card returns 'C0', no more retries are left, and the CVM
            //shall be blocked. Any subsequent VERIFY command applied in the
            //context of that application shall then fail with SW1 SW2 = '6983'.
            card.selectedApp.pinTryCounter--;
            byte sw2 = (byte)(0xc0 | card.selectedApp.pinTryCounter);
            return new byte[]{(byte) 0x63, sw2};
        }
    }

    private byte[] getDataBytes(byte[] cmd) {
        byte[] tmp = new byte[cmd.length - 5];
        System.arraycopy(cmd, 5, tmp, 0, tmp.length);
        return tmp;
    }

    private byte[] createResponse(byte[] data, byte sw1, byte sw2) {
        int length = data != null ? data.length + 2 : 2;
        ByteArrayOutputStream stream = new ByteArrayOutputStream(length);
        if (data != null) {
            stream.write(data, 0, data.length);
        }
        stream.write(sw1);
        stream.write(sw2);
        return stream.toByteArray();
    }

    private byte[] createResponse(byte[] data, SW sw) {
        return createResponse(data, sw.getSW1(), sw.getSW2());
    }

    @Override
    public byte[] getATR() {
        return card.atr;
    }

    @Override
    public String getConnectionInfo() {
        return "EMV Card Emulator";
    }

    @Override
    public Terminal getTerminal() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean disconnect(boolean attemptReset) throws TerminalException {
        return false;
    }

    private class CardResponseImpl implements CardResponse {

        private byte[] data;
        private byte[] sw1sw2;

        CardResponseImpl(byte[] response) {
            data = new byte[response.length - 2];
            System.arraycopy(response, 0, data, 0, response.length - 2);
            sw1sw2 = new byte[2];
            sw1sw2[0] = response[response.length - 2];
            sw1sw2[1] = response[response.length - 1];
        }

        CardResponseImpl(byte[] data, byte[] sw1sw2) {
            if (data == null) {
                data = new byte[0];
            }
            this.data = data;
            this.sw1sw2 = sw1sw2;
        }

        @Override
        public byte[] getData() {
            return data;
        }

        @Override
        public byte getSW1() {
            return sw1sw2[0];
        }

        @Override
        public byte getSW2() {
            return sw1sw2[1];
        }

        @Override
        public short getSW() {
            return Util.byte2Short(sw1sw2[0], sw1sw2[1]);
        }
    }

    public static void main(String[] args) throws Exception {
        CardEmulator emulator = new CardEmulator("/sdacardtransaction.xml");
    }
}
