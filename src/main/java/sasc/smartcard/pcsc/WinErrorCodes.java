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

import java.util.HashMap;
import java.util.Map;

/**
 * http://msdn.microsoft.com/en-us/library/cc242851.aspx
 *
 * @author sasc
 */
public enum WinErrorCodes {

    //Not a PCSC error
    WINDOWS_ERROR_CODE_22_ERROR_BAD_COMMAND(0x00000016, "Error: Bad Command"),

    SCARD_S_SUCCESS(0x00000000, "No error has occurred"),
    SCARD_F_INTERNAL_ERROR(0x80100001, "An internal consistency check failed"),
    SCARD_E_CANCELLED(0x80100002, "The action was canceled by a Cancel request."),
    SCARD_E_INVALID_HANDLE(0x80100003, "The supplied handle was invalid."),
    SCARD_E_INVALID_PARAMETER(0x80100004, "One or more of the supplied parameters could not be properly interpreted."),
    SCARD_E_INVALID_TARGET(0x80100005, "Registry startup information is missing or invalid."),
    SCARD_E_NO_MEMORY(0x80100006, "Not enough memory available to complete this command."),
    SCARD_F_WAITED_TOO_LONG(0x80100007, "An internal consistency timer has expired."),
    SCARD_E_INSUFFICIENT_BUFFER(0x80100008, "The data buffer to receive returned data is too small for the returned data."),
    SCARD_E_UNKNOWN_READER(0x80100009, "The specified reader name is not recognized."),
    SCARD_E_TIMEOUT(0x8010000A, "The user-specified time-out value has expired."),
    SCARD_E_SHARING_VIOLATION(0x8010000B, "The smart card cannot be accessed because of other connections outstanding."),
    SCARD_E_NO_SMARTCARD(0x8010000C, "The operation requires a smart card, but no smart card is currently in the device."),
    SCARD_E_UNKNOWN_CARD(0x8010000D, "The specified smart card name is not recognized."),
    SCARD_E_CANT_DISPOSE(0x8010000E, "The system could not dispose of the media in the requested manner."),
    SCARD_E_PROTO_MISMATCH(0x8010000F, "The requested protocols are incompatible with the protocol currently in use with the smart card."),
    SCARD_E_NOT_READY(0x80100010, "The reader or smart card is not ready to accept commands."),
    SCARD_E_INVALID_VALUE(0x80100011, "One or more of the supplied parameters values could not be properly interpreted."),
    SCARD_E_SYSTEM_CANCELLED(0x80100012, "The action was canceled by the system, presumably to log off or shut down."),
    SCARD_F_COMM_ERROR(0x80100013, "An internal communications error has been detected."),
    SCARD_F_UNKNOWN_ERROR(0x80100014, "An internal error has been detected, but the source is unknown."),
    SCARD_E_INVALID_ATR(0x80100015, "An ATR obtained from the registry is not a valid ATR string."),
    SCARD_E_NOT_TRANSACTED(0x80100016, "An attempt was made to end a non-existent transaction."),
    SCARD_E_READER_UNAVAILABLE(0x80100017, "The specified reader is not currently available for use."),
    SCARD_P_SHUTDOWN(0x80100018, "The operation has been stopped to allow the server application to exit."),
    SCARD_E_PCI_TOO_SMALL(0x80100019, "The PCI Receive buffer was too small."),
    SCARD_E_ICC_INSTALLATION(0x80100020, "No primary provider can be found for the smart card."),
    SCARD_E_ICC_CREATEORDER(0x80100021, "The requested order of object creation is not supported."),
    SCARD_E_UNSUPPORTED_FEATURE(0x80100022, "This smart card does not support the requested feature."),
    SCARD_E_DIR_NOT_FOUND(0x80100023, "The specified directory does not exist in the smart card."),
    SCARD_E_FILE_NOT_FOUND(0x80100024, "The specified file does not exist in the smart card."),
    SCARD_E_NO_DIR(0x80100025, "The supplied path does not represent a smart card directory."),
    SCARD_E_READER_UNSUPPORTED(0x8010001A, "The reader device driver does not meet minimal requirements for support."),
    SCARD_E_DUPLICATE_READER(0x8010001B, "The reader device driver did not produce a unique reader name."),
    SCARD_E_CARD_UNSUPPORTED(0x8010001C, "The smart card does not meet minimal requirements for support."),
    SCARD_E_NO_SERVICE(0x8010001D, "Smart Cards for Windows is not running."),
    SCARD_E_SERVICE_STOPPED(0x8010001E, "Smart Cards for Windows has shut down."),
    SCARD_E_UNEXPECTED(0x8010001F, "An unexpected card error has occurred."),
    SCARD_E_NO_FILE(0x80100026, "The supplied path does not represent a smart card file."),
    SCARD_E_NO_ACCESS(0x80100027, "Access is denied to this file."),
    SCARD_E_WRITE_TOO_MANY(0x80100028, "The smart card does not have enough memory to store the information."),
    SCARD_E_BAD_SEEK(0x80100029, "There was an error trying to set the smart card file object pointer."),
    SCARD_E_INVALID_CHV(0x8010002A, "The supplied PIN is incorrect."),
    SCARD_E_UNKNOWN_RES_MSG(0x8010002B, "An unrecognized error code was returned from a layered component."),
    SCARD_E_NO_SUCH_CERTIFICATE(0x8010002C, "The requested certificate does not exist."),
    SCARD_E_CERTIFICATE_UNAVAILABLE(0x8010002D, "The requested certificate could not be obtained."),
    SCARD_E_NO_READERS_AVAILABLE(0x8010002E, "Cannot find a smart card reader."),
    SCARD_E_COMM_DATA_LOST(0x8010002F, "A communications error with the smart card has been detected. Retry the operation."),
    SCARD_E_NO_KEY_CONTAINER(0x80100030, "The requested key container does not exist."),
    SCARD_E_SERVER_TOO_BUSY(0x80100031, "Smart Cards for Windows is too busy to complete this operation."),
    SCARD_E_PIN_CACHE_EXPIRED(0x80100032, "The smart card PIN cache has expired."),
    SCARD_E_NO_PIN_CACHE(0x80100033, "The smart card PIN cannot be cached."),
    SCARD_E_READ_ONLY_CARD(0x80100034, "The smart card is read-only and cannot be written to."),
    SCARD_W_UNSUPPORTED_CARD(0x80100065, "The reader cannot communicate with the smart card due to ATR configuration conflicts."),
    SCARD_W_UNRESPONSIVE_CARD(0x80100066, "The smart card is not responding to a reset."),
    SCARD_W_UNPOWERED_CARD(0x80100067, "Power has been removed from the smart card, so that further communication is impossible."),
    SCARD_W_RESET_CARD(0x80100068, "The smart card has been reset, so any shared state information is invalid."),
    SCARD_W_REMOVED_CARD(0x80100069, "The smart card has been removed, so that further communication is impossible."),
    SCARD_W_SECURITY_VIOLATION(0x8010006A, "Access was denied because of a security violation."),
    SCARD_W_WRONG_CHV(0x8010006B, "The card cannot be accessed because the wrong PIN was presented."),
    SCARD_W_CHV_BLOCKED(0x8010006C, "The card cannot be accessed because the maximum number of PIN entry attempts has been reached."),
    SCARD_W_EOF(0x8010006D, "The end of the smart card file has been reached."),
    SCARD_W_CANCELLED_BY_USER(0x8010006E, "The action was canceled by the user."),
    SCARD_W_CARD_NOT_AUTHENTICATED(0x8010006F, "No PIN was presented to the smart card."),
    SCARD_W_CACHE_ITEM_NOT_FOUND(0x80100070, "The requested item could not be found in the cache."),
    SCARD_W_CACHE_ITEM_STALE(0x80100071, "The requested cache item is too old and was deleted from the cache."),
    SCARD_W_CACHE_ITEM_TOO_BIG(0x80100072, "The new cache item exceeds the maximum per-item size defined for the cache.");

    private final int code;
    private final String description;
    private static final Map<Integer, String> code2String = new HashMap<Integer, String>();

    WinErrorCodes(int code, String description) {
        this.code = code;
        this.description = description;
    }

    static {
        for (WinErrorCodes errorCode : WinErrorCodes.values()) {
            code2String.put(errorCode.code, errorCode.description);
        }
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString(){
        return getDescription();
    }

    public static String getDescription(int code) {
        return code2String.get(code);
    }

//SCARD_E_CANCELLED = 0x80100002 The action was canceled by a Cancel request.
//
//SCARD_E_INVALID_HANDLE = 0x80100003 The supplied handle was invalid.
//
//SCARD_E_INVALID_PARAMETER = 0x80100004 One or more of the supplied parameters could not be properly interpreted.
//
//SCARD_E_INVALID_TARGET = 0x80100005 Registry startup information is missing or invalid.
//
//SCARD_E_NO_MEMORY = 0x80100006 Not enough memory available to complete this command.
//
//SCARD_F_WAITED_TOO_LONG 0x80100007 An internal consistency timer has expired.
//
//SCARD_E_INSUFFICIENT_BUFFER 0x80100008 The data buffer to receive returned data is too small for the returned data.
//
//SCARD_E_UNKNOWN_READER = 0x80100009 The specified reader name is not recognized.
//
//SCARD_E_TIMEOUT = 0x8010000A The user-specified time-out value has expired.
//
//SCARD_E_SHARING_VIOLATION = 0x8010000B The smart card cannot be accessed because of other connections outstanding.
//
//SCARD_E_NO_SMARTCARD = 0x8010000C The operation requires a smart card, but no smart card is currently in the device.
//
//SCARD_E_UNKNOWN_CARD = 0x8010000D The specified smart card name is not recognized.
//
//SCARD_E_CANT_DISPOSE = 0x8010000E The system could not dispose of the media in the requested manner.
//
//SCARD_E_PROTO_MISMATCH = 0x8010000F The requested protocols are incompatible with the protocol currently in use with the smart card.
//
//SCARD_E_NOT_READY = 0x80100010 The reader or smart card is not ready to accept commands.
//
//SCARD_E_INVALID_VALUE = 0x80100011 One or more of the supplied parameters values could not be properly interpreted.
//
//SCARD_E_SYSTEM_CANCELLED0x80100012 The action was canceled by the system, presumably to log off or shut down.
//
//SCARD_F_COMM_ERROR 0x80100013 An internal communications error has been detected.
//
//SCARD_F_UNKNOWN_ERROR 0x80100014 An internal error has been detected, but the source is unknown.
//
//SCARD_E_INVALID_ATR 0x80100015 An ATR obtained from the registry is not a valid ATR string.
//
//SCARD_E_NOT_TRANSACTED 0x80100016 An attempt was made to end a non-existent transaction.
//
//SCARD_E_READER_UNAVAILABLE 0x80100017 The specified reader is not currently available for use.
//
//SCARD_P_SHUTDOWN 0x80100018 The operation has been stopped to allow the server application to exit.
//
//SCARD_E_PCI_TOO_SMALL 0x80100019 The PCI Receive buffer was too small.
//
//SCARD_E_ICC_INSTALLATION 0x80100020 No primary provider can be found for the smart card.
//
//SCARD_E_ICC_CREATEORDER 0x80100021 The requested order of object creation is not supported.
//
//SCARD_E_UNSUPPORTED_FEATURE 0x80100022 This smart card does not support the requested feature.
//
//SCARD_E_DIR_NOT_FOUND 0x80100023 The specified directory does not exist in the smart card.
//
//SCARD_E_FILE_NOT_FOUND 0x80100024 The specified file does not exist in the smart card.
//
//SCARD_E_NO_DIR 0x80100025 The supplied path does not represent a smart card directory.
//
//SCARD_E_READER_UNSUPPORTED 0x8010001A The reader device driver does not meet minimal requirements for support.
//
//SCARD_E_DUPLICATE_READER 0x8010001B The reader device driver did not produce a unique reader name.
//
//SCARD_E_CARD_UNSUPPORTED 0x8010001C The smart card does not meet minimal requirements for support.
//
//SCARD_E_NO_SERVICE 0x8010001D Smart Cards for Windows is not running.
//
//SCARD_E_SERVICE_STOPPED 0x8010001E Smart Cards for Windows has shut down.
//
//SCARD_E_UNEXPECTED 0x8010001F An unexpected card error has occurred.
//
//SCARD_E_NO_FILE 0x80100026 The supplied path does not represent a smart card file.
//
//SCARD_E_NO_ACCESS 0x80100027 Access is denied to this file.
//
//SCARD_E_WRITE_TOO_MANY 0x80100028 The smart card does not have enough memory to store the information.
//
//SCARD_E_BAD_SEEK 0x80100029 There was an error trying to set the smart card file object pointer.
//
//SCARD_E_INVALID_CHV 0x8010002A The supplied PIN is incorrect.
//
//SCARD_E_UNKNOWN_RES_MSG 0x8010002B An unrecognized error code was returned from a layered component.
//
//SCARD_E_NO_SUCH_CERTIFICATE 0x8010002C The requested certificate does not exist.
//
//SCARD_E_CERTIFICATE_UNAVAILABLE 0x8010002D The requested certificate could not be obtained.
//
//SCARD_E_NO_READERS_AVAILABLE 0x8010002E Cannot find a smart card reader.
//
//SCARD_E_COMM_DATA_LOST 0x8010002F A communications error with the smart card has been detected. Retry the operation.
//
//SCARD_E_NO_KEY_CONTAINER 0x80100030 The requested key container does not exist.
//
//SCARD_E_SERVER_TOO_BUSY 0x80100031 Smart Cards for Windows is too busy to complete this operation.
//
//SCARD_E_PIN_CACHE_EXPIRED 0x80100032 The smart card PIN cache has expired.
//
//SCARD_E_NO_PIN_CACHE 0x80100033 The smart card PIN cannot be cached.
//
//SCARD_E_READ_ONLY_CARD 0x80100034 The smart card is read-only and cannot be written to.
//
//SCARD_W_UNSUPPORTED_CARD 0x80100065 The reader cannot communicate with the smart card due to ATR configuration conflicts.
//
//SCARD_W_UNRESPONSIVE_CARD 0x80100066 The smart card is not responding to a reset.
//
//SCARD_W_UNPOWERED_CARD 0x80100067 Power has been removed from the smart card, so that further communication is impossible.
//
//SCARD_W_RESET_CARD 0x80100068 The smart card has been reset, so any shared state information is invalid.
//
//SCARD_W_REMOVED_CARD 0x80100069 The smart card has been removed, so that further communication is impossible.
//
//SCARD_W_SECURITY_VIOLATION 0x8010006A Access was denied because of a security violation.
//
//SCARD_W_WRONG_CHV 0x8010006B The card cannot be accessed because the wrong PIN was presented.
//
//SCARD_W_CHV_BLOCKED 0x8010006C The card cannot be accessed because the maximum number of PIN entry attempts has been reached.
//
//SCARD_W_EOF 0x8010006D The end of the smart card file has been reached.
//
//SCARD_W_CANCELLED_BY_USER 0x8010006E The action was canceled by the user.
//
//SCARD_W_CARD_NOT_AUTHENTICATED 0x8010006F No PIN was presented to the smart card.
//
//SCARD_W_CACHE_ITEM_NOT_FOUND 0x80100070 The requested item could not be found in the cache.
//
//SCARD_W_CACHE_ITEM_STALE 0x80100071 The requested cache item is too old and was deleted from the cache.
//
//SCARD_W_CACHE_ITEM_TOO_BIG 0x80100072 The new cache item exceeds the maximum per-item size defined for the cache.
//
}
