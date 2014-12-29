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
package sasc.smartcard.app.yubikey;

import java.util.concurrent.atomic.AtomicBoolean;
import sasc.emv.EMVUtil;
import sasc.emv.SW;
import sasc.iso7816.AID;
import sasc.iso7816.Iso7816Commands;
import sasc.iso7816.MasterFile;
import sasc.iso7816.SmartCardException;
import sasc.smartcard.common.ApplicationHandler;
import sasc.smartcard.common.SmartCard;
import sasc.terminal.CardConnection;
import sasc.terminal.CardResponse;
import sasc.terminal.TerminalException;
import sasc.util.Log;
import sasc.util.Util;

/**
 * (From http://www.yubico.com/2012/12/yubikey-neo-composite-device/)
 * Here are the common modes:
 * -m0  HID (OTP) mode
 * -m1 CCID (OpenPGP only , no OTP) ( warning : you cannot use ykpersonalize after this setting!)
 * -m2 HID & CCID Only (OTP & OpenPGP)
 * -m82 HID & CCID (OTP and OpenPGP) EJECT Flag set - allows SmartCard and OTP concurrently.
 *
 * The EJECT_FLAG (0x80) operates as follows:
 * -with mode 1 with the EJECT_FLAG set, when touching the button the NEO will "eject" the smart card, making it unavailable to the host, when touching again it will be "inserted" again.
 * -with mode 2 with the EJECT_FLAG set, when touching the button the NEO will "eject" the smart card, send the OTP from the HID interface and then "insert" the smart-card.
 *
 * The NEO can be configured to have card presence states, emulating having a smartcard reader where a smartcard is inserted and removed. The configurations are:
 * -Always present The NEO reports that a card is permanently present. Touching the Yubikey button will cause the LED to toggle and the state of this flip-flop can be read through the secure element.
 * -Insert- and removal enabled After insertion of the NEO, it reports that a smartcard reader is present, but no smartcard is inserted. By touching the Yubikey button, the NEO reports that a smartcard has been inserted. Touching the button again causes the NEO to report that the card has been removed.
 * -Auto eject enabled The NEO will automatically report that the card has been removed after a configured time of inactivity.
 *
 * @author sasc
 */
public class Yubikey implements ApplicationHandler {

    private static final byte MODE_OTP              = 0x00;    // OTP only
    private static final byte MODE_CCID             = 0x01;    // CCID only, no eject
    private static final byte MODE_OTP_CCID         = 0x02;    // OTP + CCID composite

    private static final byte MODE_FLAG_EJECT       = (byte)0x80;    // CCID device supports eject (CCID) / OTP force eject (OTP_CCID)

    public  static final byte MODE_MASK             = 0x03;    // Mask for mode bits

    private static final byte SLOT_DEVICE_CONFIG    = 0x11;    // Write device configuration record

    private static final byte DEFAULT_CHAL_TIMEOUT  = 15;      // Default challenge timeout in seconds

    private static final byte INS_YK2_REQ           = 0x01;    // General request (cmd in P1). YubiKey API request (as used by the yubico personalization tools)
    private static final byte INS_YK2_OTP           = 0x02;    // Generate OTP (slot in P1) (zero indexed)
    private static final byte INS_YK2_STATUS        = 0x03;    // Read out status record
    private static final byte INS_YK2_NDEF          = 0x04;    // Read out NDEF record (only used by the NDEF applet)
    private static final byte NEO_CONFIG_1          = 0x00;    // Configuration 1
    private static final byte NEO_CONFIG_2          = 0x01;    // Configuration 2
    private static final byte FORMAT_ASCII          = 0x00;    // Output format is ascii
    private static final byte FORMAT_ASCII_NO_FMT   = 0x01;    // Ascii, no formatting characters
    private static final byte FORMAT_SCANCODE       = 0x02;    // Output format is scan codes
    private static final short SW_BUTTON_REQD       = 0x6985;  // ISO7816 Access condition not met

    private static final byte SLOT_CHAL_HMAC1       = 0x30;    // cmd 00 01 30 00 09 + 8 bytes challenge (-> response + SW1SW2)

//    printf("\nVersion:       %d.%d.%d\n", rAPDU.select.status.versionMajor,
//            rAPDU.select.status.versionMinor, rAPDU.select.status.versionBuild);
//    printf("Seq:           %d\n", rAPDU.select.status.pgmSeq);
//    printf("Mode:          %02x\n", rAPDU.select.config.mode & MODE_MASK);
//    printf("Flags:         %02x\n", rAPDU.select.config.mode & (~MODE_MASK));
//    printf("CR timeout:    %d\n", rAPDU.select.config.crTimeout);
//    printf("Eject time:    %d\n", rAPDU.select.config.autoEjectTime);
//    firstSeq = rAPDU.select.status.pgmSeq;


    private CardConnection terminal;

    private AtomicBoolean initialized = new AtomicBoolean(false);
    private YubikeyNeoApplication app;

    private static final byte[] NEO_AID_BYTES = Util.fromHexString("A000000527 200101");
    public static final AID NEO_AID = new AID(NEO_AID_BYTES);

    @Override
    public boolean process(AID aid, SmartCard card, CardConnection cardConnection) throws TerminalException {
        Log.debug("Processing Yubikey NEO App");
        terminal = cardConnection;

        int SW1;
        int SW2;
        byte[] command;
        Log.commandHeader("Select Yubikey NEO application interface");

        command = Iso7816Commands.selectByDFName(aid.getAIDBytes(), true, (byte)0x00);

        CardResponse selectAppResp = EMVUtil.sendCmdNoParse(terminal, command);

        SW1 = (byte) selectAppResp.getSW1();
        SW2 = (byte) selectAppResp.getSW2();
        byte[] data = selectAppResp.getData();

        if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00 && data.length == 10) {

//            typedef struct {
//                unsigned char versionMajor;  // Firmware version information
//                unsigned char versionMinor;
//                unsigned char versionBuild;
//                unsigned char pgmSeq;		 // Programming sequence number. 0 if no valid configuration
//                unsigned short touchLevel;   // Level from touch detector
//            } STATUS;
//
//            typedef struct {
//                unsigned char mode;           // Device mode
//                unsigned char crTimeout;      // Challenge-response timeout in seconds
//                unsigned short autoEjectTime; // Auto eject time in seconds
//            } DEVICE_CONFIG;

//            03 00 02 01 05 07 81 00 00 00

//            03 00 02 //Version
//            01       //Seq
//            05 07    //Level
//            81       //Mode
//            00       //CR
//            00 00    //Auto eject
//              90 00  //SW

            this.app = new YubikeyNeoApplication(aid);

            app.setVersionMajor(Util.byteToInt(data[0]));
            app.setVersionMinor(Util.byteToInt(data[1]));
            app.setVersionBuild(Util.byteToInt(data[2]));
            int firstProgSeq = Util.byteToInt(data[3]);

            // Firmware version information
//            Log.info("Firmware Version           : "+versionMajor+"."+versionMinor+"."+versionBuild);
//            Log.info("Programming sequence number: "+firstProgSeq);
//            Log.info("Level from touch detector  : "+Util.byteArrayToInt(selectAppResp.getData(), 4, 2));
//            Log.info("Device mode                : "+(selectAppResp.getData()[6] & MODE_MASK));
//            Log.info("Flags                      : 0x"+Util.byte2Hex((byte)(selectAppResp.getData()[6] & ~MODE_MASK)));
//            Log.info("Challenge-response timeout in seconds: "+Util.byteToInt(selectAppResp.getData()[7]));
//            Log.info("Auto eject time in seconds : "+Util.byteArrayToInt(selectAppResp.getData(), 8, 2));

            app.setProgrammingSeqNum(Util.byteToInt(data[3])); // Programming sequence number. 0 if no valid configuration
            app.setTouchDetectorLevel(Util.byteArrayToInt(data, 4, 2));
            app.setModeByte(data[6]);
//            app.setDeviceMode(selectAppResp.getData()[6] & MODE_MASK));
//            app.setFlags(Util.byte2Hex((byte)(selectAppResp.getData()[6] & ~MODE_MASK)));
            app.setChallengeResponseTimeout(Util.byteToInt(data[7]));
            app.setAutoEjectTime(Util.byteArrayToInt(data, 8, 2));

            card.addApplication(app);
            initialized.set(true);
        } else if(selectAppResp.getSW() == SW.APPLET_SELECTION_FAILED.getSW()) {
            Log.info("The NEO needs to be 'activated' by pressing the button (possibly twice)");
        }
        return false;
    }
    
    /**
     * May only be set using the contacted interface
     * @param mode
     * @throws TerminalException 
     */
    public void setMode(byte mode) throws TerminalException {

        if(!initialized.get()){
            throw new SmartCardException("Yubikey NEO application not initialized");
        }

        int SW1;
        int SW2;
        String commandStr;
        
        byte ejectFlag = (byte)(mode & 0xF0);
        byte modeFlag = (byte)(mode & 0x0F);

        // Transmit set config command

        Log.commandHeader("Send command Yubikey NEO write config");

        commandStr = "00"
                +Util.byte2Hex(INS_YK2_REQ)
                +Util.byte2Hex(SLOT_DEVICE_CONFIG)
                + "00"
                + "04"
                //struct DEVICE_CONFIG
                +Util.byte2Hex(mode)//+"82" //Util.byte2Hex(MODE_OTP_CCID | MODE_FLAG_EJECT)
                +Util.byte2Hex(DEFAULT_CHAL_TIMEOUT)
                +"00 00"; // Auto eject time in seconds

        CardResponse setConfigResp = EMVUtil.sendCmdNoParse(terminal, Util.fromHexString(commandStr));

        int newProgSeq = Util.byteToInt(setConfigResp.getData()[3]);

        Log.info("Update "+(newProgSeq == (app.getProgrammingSeqNum() + 1)?"successful":"failed"));

//            apdu.apdu2.cla = 0;
//            apdu.apdu2.ins = INS_YK2_REQ;
//            apdu.apdu2.p1 = SLOT_DEVICE_CONFIG;
//            apdu.apdu2.p2 = 0;
//            apdu.apdu2.lc = sizeof(DEVICE_CONFIG);
//            apdu.apdu2.config.mode = MODE_OTP; // | MODE_FLAG_EJECT;
//            apdu.apdu2.config.crTimeout = DEFAULT_CHAL_TIMEOUT;
        
//            apdu.apdu2.config.autoEjectTime = 0;
//            dwRecvLength = sizeof(rAPDU);
//            rc = SCardTransmit(hCard, SCARD_PCI_T1, (BYTE *) &apdu, 5 + apdu.apdu2.lc,	NULL, rAPDU.buf, &dwRecvLength);
//            if (rc == SCARD_S_SUCCESS) {
//                dumpHex("\nSCardTransmit [NEO write config]", rAPDU.buf, dwRecvLength);
//
//                // Parse STATUS record again
//                printf("\nSeq:           %d\n", rAPDU.select.status.pgmSeq);
//                printf("Update %s", (rAPDU.select.status.pgmSeq == (firstSeq + 1)) ? "successful" : "failed");
//            } else {
//                printf("SCardTransmit(2) failed\n");
//            }

    }
    
    public YubikeyNeoApplication getApplication() {
        return app;
    }

    public static void main(String[] args){
        byte[] data = Util.fromHexString("03 00 02 01 05 07 81 00 00 00");

        System.out.println(Util.byteArrayToInt(data, 4, 2));
    }

}
