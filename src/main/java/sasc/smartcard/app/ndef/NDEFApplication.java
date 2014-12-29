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
package sasc.smartcard.app.ndef;

import java.io.PrintWriter;
import java.io.StringWriter;
import sasc.iso7816.AID;
import sasc.iso7816.Application;
import sasc.smartcard.common.SmartCard;
import sasc.util.Log;
import sasc.util.Util;

/**
 * TODO
 * 
 * @author sasc
 */
public class NDEFApplication implements Application {
    
    private AID aid;
    private SmartCard card;
    
    public NDEFApplication(AID aid, SmartCard card) {
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
        pw.println(Util.getSpaces(indent) + "NDEF Application");

        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);
        
        if (aid != null) {
            aid.dump(pw, indent + Log.INDENT_SIZE);
        }
    }
}

//Select NDEF app:
//00 A4 04 00 07  D2 76 00 00 85 01 01  00
//
//Select Capability Container
//00 A4 00 0C 02  E1 03  00
//
//Read data
//00 B0 00 00  00 (alt: 00 b0 00 00 0f)
//
//[Parse value to find file where NDEF message is stored]
//00 0F (length, including this field)
//20 (the mappling spec version it is compliant with, MSNibble=major, 
//LSNibble=minor)
//00 7F (maximum data size that can be read from the Type 4 Tag using a 
//single ReadBinary command)
//00 7F (maximum data size that can be sent to the Type 4 Tag using a 
//single UpdateBinary command)
//04 06 (04=NDEF File Control TLV, 05=Proprietary File Control TLV)
//       E1 04 (File Identifier, 2 bytes. Indicates a valid NDEF file. The 
//valid ranges are 0001h to E101h, E104h to 3EFFh, 3F01h to 3FFEh and 
//4000h to FFFEh. The values 0000h, E102h, E103h, 3F00h and 3FFFh are 
//reserved (see [ISO/IEC_7816-4]) and FFFFh is RFU)
//       00 7F (Maximum NDEF file size. size of the file containing the 
//NDEF message. valid range is 0005h to FFFEh. The values 0000h-0004h and 
//FFFFh are RFU.)
//       00 (NDEF file read access condition. 00=no security)
//       00 (NDEF file write access condition. 00=no security)
//
//NDEF file read access condition, 1 byte:
//- 00h indicates read access granted without any security
//- 01h to 7Fh and FFh are RFU
//- 80h to FEh are proprietary
//NDEF file write access condition, 1 byte:
//- 00h indicates write access granted without any security
//- FFh indicates no write access granted at all (read-only)
//- 01h to 7Fh are RFU
//- 80h to FEh are proprietary
//
//[Optional TLV blocks]
//90 00
//
//Select file E104 which contains the NDEF message
//00 A4 00 0C 02 [E1 04]
//
//Read the NDEF message
//00 B0 00 00 00

