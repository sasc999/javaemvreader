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

import java.lang.reflect.Field;
import javax.smartcardio.CardException;
import sasc.smartcard.pcsc.WinErrorCodes;
import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * @author tl
 */
public class SmartcardioUtils {
    public static String getPCSCErrorDescription(CardException ex){
        Integer i = getPCSCError(ex);
        if(i == null) {
            return "";
        }
        String description = WinErrorCodes.getDescription(i);
        return description != null ? description : "";
    }
    
    //http://pcsclite.alioth.debian.org/api/pcsclite_8h_source.html
    public static Integer getPCSCError(CardException ce){
        if(ce == null){
            return null;
        }
        Throwable cause = ce.getCause();
        while(cause != null){
            if("sun.security.smartcardio.PCSCException".equals(cause.getClass().getName())){
                Log.debug("checking field in class: "+cause.getClass().getName());
                Log.debug("cause: "+cause);
                try{
                    Field f = cause.getClass().getDeclaredField("code");
                    f.setAccessible(true);
                    Integer i = (Integer)f.get(cause);
                    return i;
                }catch(NoSuchFieldException ex){
                    Log.debug(Util.getStackTrace(ex));
                }catch(IllegalAccessException ex){
                    Log.debug(Util.getStackTrace(ex));
                }catch(ClassCastException ex){
                    Log.debug(Util.getStackTrace(ex));
                }
            }
            cause = cause.getCause();
        }
        
        return null;
    }
    
    //MS winscard.h
    
//    http://blogs.msdn.com/b/shivaram/archive/2007/02/26/smart-card-logon-on-windows-vista.aspx
//    #if (NTDDI_VERSION >= NTDDI_WINXP)
//    extern WINSCARDAPI HANDLE WINAPI
//    SCardAccessStartedEvent(void);
//
//    extern WINSCARDAPI void WINAPI
//    SCardReleaseStartedEvent(void);
//    #endif // (NTDDI_VERSION >= NTDDI_WINXP)

    public static boolean isNoCardReadersAvailable(CardException ex){
        Integer i = getPCSCError(ex);
        if(i == null){
            return false;
        }
        if(i == WinErrorCodes.SCARD_E_NO_READERS_AVAILABLE.getCode()){
            return true;
        }
        return false;
    }
}
