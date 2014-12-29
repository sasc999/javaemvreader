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
package sasc.smartcard.pcsc;

import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class PCSC {

    //PC/SC part 10 v2.02.07 March 2010
    public static final int FEATURE_VERIFY_PIN_START         = 0x01;
    public static final int FEATURE_VERIFY_PIN_FINISH        = 0x02;
    public static final int FEATURE_MODIFY_PIN_START         = 0x03;
    public static final int FEATURE_MODIFY_PIN_FINISH        = 0x04;
    public static final int FEATURE_GET_KEY_PRESSED          = 0x05;
    public static final int FEATURE_VERIFY_PIN_DIRECT        = 0x06;
    public static final int FEATURE_MODIFY_PIN_DIRECT        = 0x07;
    public static final int FEATURE_MCT_READER_DIRECT        = 0x08;
    public static final int FEATURE_MCT_UNIVERSAL            = 0x09;
    public static final int FEATURE_IFD_PIN_PROPERTIES       = 0x0A;
    public static final int FEATURE_ABORT                    = 0x0B;
    public static final int FEATURE_SET_SPE_MESSAGE          = 0x0C;
    public static final int FEATURE_VERIFY_PIN_DIRECT_APP_ID = 0x0D;
    public static final int FEATURE_MODIFY_PIN_DIRECT_APP_ID = 0x0E;
    public static final int FEATURE_WRITE_DISPLAY            = 0x0F;
    public static final int FEATURE_GET_KEY                  = 0x10;
    public static final int FEATURE_IFD_DISPLAY_PROPERTIES   = 0x11;
    public static final int FEATURE_GET_TLV_PROPERTIES       = 0x12;
    public static final int FEATURE_CCID_ESC_COMMAND         = 0x13;
    public static final int FEATURE_EXECUTE_PACE             = 0x20;
    
    /* 
     * vendor-specific IOCTL codes: Refer to the macro SCARD_CTL_CODE in 
     * winsmcrd.h (DDK) for information on how to define a vendor-specific IOCTL code. 
     * Note that the code must be between 2048 and 4095.  (0x800 -> 0xFFF)
     * 
     */
    
    public static final int CM_IOCTL_GET_FEATURE_REQUEST = SCARD_CTL_CODE(3400);
    /**
     * Code for sending command Escape (PC_to_RDR_Escape) to the Reader
     */
    public static final int IOCTL_SMARTCARD_VENDOR_IFD_EXCHANGE = SCARD_CTL_CODE(1);
    
    public static final int IOCTL_FEATURE_VERIFY_PIN_DIRECT = SCARD_CTL_CODE(FEATURE_VERIFY_PIN_DIRECT + 0x330000);
    public static final int IOCTL_FEATURE_MODIFY_PIN_DIRECT = SCARD_CTL_CODE(FEATURE_MODIFY_PIN_DIRECT + 0x330000);
    public static final int IOCTL_FEATURE_MCT_READER_DIRECT = SCARD_CTL_CODE(FEATURE_MCT_READER_DIRECT + 0x330000);
    public static final int IOCTL_FEATURE_IFD_PIN_PROPERTIES = SCARD_CTL_CODE(FEATURE_IFD_PIN_PROPERTIES + 0x330000);
    public static final int IOCTL_FEATURE_GET_TLV_PROPERTIES = SCARD_CTL_CODE(FEATURE_GET_TLV_PROPERTIES + 0x330000);
    
    // TAGs returned by FEATURE_GET_TLV_PROPERTIES
    public static final int PCSCv2_PART10_PROPERTY_wLcdLayout                = 1;
    public static final int PCSCv2_PART10_PROPERTY_bEntryValidationCondition = 2;
    public static final int PCSCv2_PART10_PROPERTY_bTimeOut2                 = 3;
    public static final int PCSCv2_PART10_PROPERTY_wLcdMaxCharacters         = 4;
    public static final int PCSCv2_PART10_PROPERTY_wLcdMaxLines              = 5;
    public static final int PCSCv2_PART10_PROPERTY_bMinPINSize               = 6;
    public static final int PCSCv2_PART10_PROPERTY_bMaxPINSize               = 7;
    public static final int PCSCv2_PART10_PROPERTY_sFirmwareID               = 8;
    public static final int PCSCv2_PART10_PROPERTY_bPPDUSupport              = 9;
    public static final int PCSCv2_PART10_PROPERTY_dwMaxAPDUDataSize         = 10;
    public static final int PCSCv2_PART10_PROPERTY_wIdVendor                 = 11;
    public static final int PCSCv2_PART10_PROPERTY_wIdProduct                = 12;
    
    public static int SCARD_CTL_CODE(int code) {
        String os_name = System.getProperty("os.name").toLowerCase();
        // System.out.println(os_name);
        if (os_name.matches("(?i).*windows.*")) {
            // cf. WinIOCTL.h
            //
            //for windows:
            //definition of SCARD_CTL_CODE (winsmcrd.h):
            //
            //#define SCARD_CTL_CODE(code)        
            //            CTL_CODE(FILE_DEVICE_SMARTCARD, \
            //            (code), \
            //            METHOD_BUFFERED, \
            //            FILE_ANY_ACCESS) 
            //
            //CTL_CODE is defined in devioctl.h:
            //(http://marc.info/?l=ms-smartcardsdk&m=107295129204258)
            //
            //#define FILE_DEVICE_SMARTCARD           0x00000031
            //#define METHOD_BUFFERED                 0
            //#define FILE_ANY_ACCESS                 0
            //
            //// Macro definition for defining IOCTL and FSCTL function control codes.
            //// Note that function codes 0-2047 are reserved for Microsoft Corporation, 
            //// and 2048-4095 are reserved for customers.
            //
            //#define CTL_CODE( DeviceType, Function, Method, Access ) ( \
            //    ((DeviceType) << 16) | ((Access) << 14) | ((Function) << 2) |
            //    (Method) \
            //     )
            
            if(code == 1){ // code < 2048 is not valid on windows
                code = 2048;
            }

            int winControlCode = (0x31 << 16 | (code) << 2);
            Log.debug("Constructed CTL_CODE="+Util.int2Hex(winControlCode) + " (windows PC/SC format)");
            return winControlCode;
        }
        // cf. reader.h
        int controlCode = 0x42000000 + (code);
        Log.debug("Constructed CTL_CODE="+Util.int2Hex(controlCode) + " (*nix PC/SC format)");
        return controlCode;
    }
}
