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
package sasc.smartcard.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import sasc.iso7816.AID;
import sasc.util.Util;

/**
 * A place to register handlers for specific ATR/AID patterns.
 * Handlers are invoked in the order they are registered.
 * A handler may choose to handle at ATR or AID exclusively:
 * -If ATR: then processing stops for this card
 * -If AID: then processing stops for this AID
 * 
 * ATR and AID patterns are matched using Regular Expressions
 * 
 * @author sasc
 */
public class Registry {
    private static final Registry INSTANCE = new Registry();
    
    private Map<String, AtrHandler> atrHandlers = Collections.synchronizedMap(new LinkedHashMap<String, AtrHandler>());
    private Map<String, ApplicationHandler> aidHandlers = Collections.synchronizedMap(new LinkedHashMap<String, ApplicationHandler>());
//    private Map<AID, ApplicationHandler> aidHandlers = Collections.synchronizedMap(new LinkedHashMap<AID, ApplicationHandler>());
    
    public static Registry getInstance() {
        return INSTANCE;
    }
    
    public void registerAtrHandler(AtrHandler atrHandler, String atrPattern) {
        atrHandlers.put(atrPattern, atrHandler);
    }
    
    public void registerAtrHandler(AtrHandler atrHandler, List<String> atrPatterns) {
        for(String pattern : atrPatterns) {
            atrHandlers.put(pattern, atrHandler);
        }
    }
    
    public void registerAidHandler(ApplicationHandler aidHandler, String aidPattern) {
        byte[] aidPatternBytes = Util.fromHexString(aidPattern); //Sanitize
        aidHandlers.put(Util.prettyPrintHexNoWrap(aidPatternBytes).toUpperCase(), aidHandler);
    }
    
    public void registerAidHandler(ApplicationHandler aidHandler, AID aid) {
        registerAidHandler(aidHandler, Util.byteArrayToHexString(aid.getAIDBytes()));
    }

    public List<ApplicationHandler> getHandlersForAid(AID aid) {
        return getHandlersForAid(aid.getAIDBytes());
    }

    public List<ApplicationHandler> getHandlersForAid(byte[] aid) {
        List<ApplicationHandler> handlers = new ArrayList<ApplicationHandler>();
        String aidStr = Util.prettyPrintHexNoWrap(aid).toUpperCase();
        for(String aidPatternStr : aidHandlers.keySet()) {
            if(aidStr.matches("^"+aidPatternStr+"$")){
                ApplicationHandler handler = aidHandlers.get(aidPatternStr);
                if(handler != null){
                    handlers.add(handler);
                }
            }
        }
        return handlers;
    }
    
    public List<AtrHandler> getHandlersForAtr(byte[] atr) {
        List<AtrHandler> handlers = new ArrayList<AtrHandler>();
        String atrStr = Util.prettyPrintHexNoWrap(atr).toUpperCase();
        for(String atrPatternStr : atrHandlers.keySet()){
            if(atrStr.matches("^"+atrPatternStr+"$")){
                AtrHandler handler = atrHandlers.get(atrPatternStr);
                if(handler != null){
                    handlers.add(handler);
                }
            }
        }
        return handlers;
    }
}
