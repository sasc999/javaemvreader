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
package sasc.smartcard.app.usim;

import java.io.PrintWriter;
import java.io.StringWriter;
import sasc.iso7816.AID;
import sasc.iso7816.Application;
import sasc.smartcard.common.SmartCard;
import sasc.util.Log;
import sasc.util.Util;

/**
 * (U)SIM/UICC
 * 
 * @author sasc
 */
public class USIM implements Application {

    private AID aid;
    private SmartCard card;
    
    public USIM(AID aid, SmartCard card) {
        this.aid = aid;
        this.card = card;
    }
    
    @Override
    public AID getAID() {
        return aid;
    }

    @Override
    public SmartCard getCard() {
        return card;
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    @Override
    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "(U)SIM Application");

        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);
        
        if (aid != null) {
            aid.dump(pw, indent + Log.INDENT_SIZE);
        }
        
        pw.println(indentStr+"");
        
    }

}
// According to the 3GPP standards the phone / UE, when initializing the card, has to read the GSM files, look up the USIM AID (=application ID) listed in the EF_DIR file and then select the USIM AID, thus selecting and activating the ADF USIM.

// select MF
// select EF.DIR
//  if contains AID USIM -> USIM
// check USIM_DF
// check SIM_DF
// find 3G USIM app
// -> if not found, try 3G RID (from EF_DIR?)
    
// Verify whether CHV1 (PIN1) is needed to access the card
// 0x9804 = Security status not satisfied?

// http://scard.org/press/19971221-01/
// http://code.metager.de/source/xref/hostapd-wpa_supplicant/src/utils/pcsc_funcs.c
// http://osxr.org/android/source/external/wpa_supplicant_6/wpa_supplicant/src/utils/pcsc_funcs.h
// http://osxr.org/android/source/external/wpa_supplicant_6/wpa_supplicant/src/utils/pcsc_funcs.c
// https://android.googlesource.com/platform/frameworks/opt/telephony/+/jb-mr1-dev/src/java/com/android/internal/telephony/IccConstants.java
    
// Select Cyclic File (6E00) 00 A4 00 00 02 6E 00    
// DF Telecom (7F10) 00 A4 00 00 02 7F 10
// SCARD_FILE_GSM_DF 0x7F20
// SCARD_FILE_UMTS_DF 0x7F50
// SCARD_FILE_GSM_EF_IMSI 0x6F07
// SCARD_FILE_GSM_EF_AD 0x6FAD
// SCARD_FILE_EF_DIR 0x2F00
// SCARD_FILE_EF_ICCID 0x2FE2 (ICC Serial Number)
// SCARD_FILE_EF_CK 0x6FE1
//
// SCARD_FILE_EF_CK 0x6FE1
// SCARD_FILE_EF_IK 0x6FE2
// SCARD_CURRENT_ADF 0x7FFF <- current selected (u)sim
    
// USIM_CMD_RUN_UMTS_ALG 0x00, 0x88, 0x00, 0x81, 0x22
// USIM_CMD_STATUS       0x80, 0xF2, 0x00, 0x00, 0x00
// unsigned char cmd[50] = { USIM_CMD_STATUS };
// cmd[3] = 0x01; // P2 -> get AID of selected application.
//
//    GET STATUS:
//  if (scard->sim_type == SCARD_GSM_SIM) {
//   cmd[0] = 0xA0;
//   cmd[2] = 0x00;
//   cmd[3] = 0x00;
//   cmd[4] = 0x00;
//  }
//    

