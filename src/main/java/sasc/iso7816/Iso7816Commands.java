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
package sasc.iso7816;

import java.io.ByteArrayOutputStream;
import sasc.util.Log;
import sasc.util.Util;

/**
 * See Iso7816-4:2005 Table 4.2
 * 
 * @author sasc
 */
public class Iso7816Commands {
    
    public static final byte ISO_CLA           = (byte)0x00;
    public static final byte ISO_SELECT        = (byte)0xa4;
    public static final byte ISO_READ_RECORD   = (byte)0xb2;
    public static final byte ISO_INTERNAL_AUTH = (byte)0x88;
    public static final byte ISO_EXTERNAL_AUTH = (byte)0x82;
    public static final byte ISO_GET_DATA      = (byte)0xca;
    
    /**
     * Select Master File.
     * Standard iso7816 command
     */
    public static byte[] selectMasterFile() {
        return Util.fromHexString("00 A4 00 00 00");
    }

    public static byte[] selectMasterFileByIdentifier() {
        return Util.fromHexString("00 A4 00 00 02 3F 00 00");
    }

    public static byte[] selectByDFName(byte[] fileBytes, boolean lePresent, byte le) {
        if (fileBytes.length > 16) {
            throw new IllegalArgumentException("Dedicated File name not valid (length > 16). Length = "+fileBytes.length);
        }
        byte[] cmd = new byte[5+fileBytes.length+(lePresent?1:0)];
        cmd[0] = ISO_CLA;
        cmd[1] = ISO_SELECT;
        //04 - Direct selection by DF name (data field=DF name)
        cmd[2] = 0x04;
        //TODO: when P2 = 0C : see 7816-4 spec..
        cmd[3] = 0x00;
        cmd[4] = (byte)fileBytes.length;
        System.arraycopy(fileBytes, 0, cmd, 5, fileBytes.length);
        if(lePresent) {
            cmd[cmd.length-1] = 0x00;
        }
        return cmd;
    }
    
    public static byte[] selectByDFNameNextOccurrence(byte[] fileBytes, boolean lePresent, byte le) {
        if (fileBytes.length > 16) {
            throw new IllegalArgumentException("Dedicated File name not valid (length > 16). Length = "+fileBytes.length);
        }
        byte[] cmd = new byte[5+fileBytes.length+(lePresent?1:0)];
        cmd[0] = ISO_CLA;
        cmd[1] = ISO_SELECT;
        //04 - Direct selection by DF name (data field=DF name)
        cmd[2] = 0x04;
        //02 - Next occurrence
        cmd[3] = 0x02;
        cmd[4] = (byte)fileBytes.length;
        System.arraycopy(fileBytes, 0, cmd, 5, fileBytes.length);
        if(lePresent) {
            cmd[cmd.length-1] = 0x00;
        }
        return cmd;
    }

    public static byte[] readRecord(int recordNum, int sfi) {
        //Valid Record numbers: 1 to 255
        //Valid SFI: 1 to 30
        //SFI=0 : Currently selected EF
        if (recordNum < 1 || recordNum > 255) {
            throw new IllegalArgumentException("Argument 'recordNum' must be in the rage 1 to 255. recordNum=" + recordNum);
        }
        if (sfi < 0 || sfi > 30) {
            throw new IllegalArgumentException("Argument 'sfi' must be in the rage 1 to 30. Use 0 for currently selected EF. sfi=" + sfi);
        }

        byte P1 = (byte) recordNum;

        //00010100 = P2
        //00010    = SFI (= 2 << 3)
        //     100 = "Record number can be found in P1" (=4)
        byte P2 = (byte) ((sfi << 3) | 4);

        Log.debug("Iso7816Commands.readRecord() P1=" + P1 + " P2=" + P2);

        byte[] cmd = new byte[5];
        //00 = No secure messaging
        cmd[0] = ISO_CLA;
        //B2 = READ RECORD
        cmd[1] = ISO_READ_RECORD;
        //P1 = Record number or record identifier of the first record to be read ('00' indicates the current record)
        cmd[2] = P1;
        //P2 = SFI + 4 (Indicates that the record number can be found in P1)
        cmd[3] = P2;
        cmd[4] = 0x00;
        return cmd;
    }

    public static byte[] internalAuthenticate(byte[] authenticationRelatedData) {
        if (authenticationRelatedData == null) {
            throw new IllegalArgumentException("Argument 'authenticationRelatedData' cannot be null");
        }
        byte[] cmd = new byte[5+authenticationRelatedData.length+1];
        cmd[0] = ISO_CLA;
        cmd[1] = ISO_INTERNAL_AUTH;
        cmd[2] = 0x00;
        cmd[3] = 0x00;
        cmd[4] = (byte)authenticationRelatedData.length;
        System.arraycopy(authenticationRelatedData, 0, cmd, 5, authenticationRelatedData.length);
        cmd[cmd.length-1] = 0x00;
        return cmd;
    }

    public static byte[] externalAuthenticate(byte[] cryptogram, byte[] proprietaryBytes) {
        if (cryptogram == null) {
            throw new IllegalArgumentException("Argument 'cryptogram' cannot be null");
        }
        if (cryptogram.length != 8) {
            throw new IllegalArgumentException("Argument 'cryptogram' must have a length of 8. length=" + cryptogram.length);
        }
        if (proprietaryBytes != null && (proprietaryBytes.length < 1 || proprietaryBytes.length > 8)) {
            throw new IllegalArgumentException("Argument 'proprietaryBytes' must have a length in the range 1 to 8. length=" + proprietaryBytes.length);
        }
        int length = cryptogram.length;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(ISO_CLA);
        stream.write(ISO_EXTERNAL_AUTH);
        stream.write(0x00);
        stream.write(0x00);
        if (proprietaryBytes != null) {
            length += proprietaryBytes.length;
        }
        stream.write(length);
        stream.write(cryptogram, 0, cryptogram.length);
        if (proprietaryBytes != null) {
            stream.write(proprietaryBytes, 0, proprietaryBytes.length);
        }
        stream.write(0x00); //Le
        return stream.toByteArray();
    }
    
    public static byte[] getData(byte p1, byte p2, byte le) {
        byte[] cmd = new byte[5];
        cmd[0] = ISO_CLA;
        cmd[1] = ISO_GET_DATA;
        cmd[2] = p1;
        cmd[3] = p2;
        cmd[4] = le;
        return cmd;
    }
}
