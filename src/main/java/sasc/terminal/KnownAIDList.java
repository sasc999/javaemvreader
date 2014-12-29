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
package sasc.terminal;

import sasc.iso7816.AID;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import nanoxml.XMLElement;
import sasc.util.Util;

/**
 * Part of list maintained by "Terminal"
 * 
 * @author sasc
 */
public class KnownAIDList {

    private static final Map<AID, KnownAID> knownAIDsMap = new LinkedHashMap<AID, KnownAID>();
//    private static final Map<String, List<KnownAID>> knownAIDsByTypeMap = new LinkedHashMap<String, List<KnownAID>>();

    /*
     * For an application in the ICC to be supported by an application in the terminal,
     * the Application Selection Indicator indicates whether the associated AID
     * in the terminal must match the AID in the card exactly, including the length of the AID,
     * or only up to the length of the AID in the terminal.
     *
     * There is only one Application Selection Indicator per AID supported by the terminal
     *
     * Format: At the discretion of the terminal. The data is not sent across the interface
     */
    public static enum ApplicationSelectionIndicator {

        EXACT_MATCH, PARTIAL_MATCH;
    }

    public static class KnownAID {

        private ApplicationSelectionIndicator asi;
        private AID aid;
        private boolean supported;
        private String type;
        private String name;
        private String description;
//        private String candidateType;

        KnownAID(String name, AID aid, String type, boolean supported, ApplicationSelectionIndicator asi, String description) {
            this.name = name;
            this.aid = aid;
            this.type = type;
            this.supported = supported;
            this.asi = asi;
            this.description = description;
        }

        public AID getAID() {
            return this.aid;
        }

        public boolean partialMatchAllowed() {
            return ApplicationSelectionIndicator.PARTIAL_MATCH.equals(this.asi);
        }

        public boolean isSupported(){
            return this.supported;
        }
        
        public String getName(){
            return name;
        }
        
        public String getType(){
            return type;
        }
        
        public String getDescription(){
            return description;
        }

        @Override
        public String toString(){
            StringBuilder buf = new StringBuilder();
            buf.append(Util.prettyPrintHexNoWrap(this.aid.getAIDBytes()));
            buf.append(" ");
            buf.append(this.name);
            buf.append(" ");
            buf.append(this.type);
            buf.append(" ");
            buf.append(this.supported);
            buf.append(" ");
            buf.append(this.asi);
            buf.append(" ");
            buf.append(description);
            return buf.toString();
        }
    }

    public static Collection<KnownAID> getAIDsByType(String type) {
        List<KnownAID> aids = new ArrayList<KnownAID>();
        for(KnownAID aid : knownAIDsMap.values()) {
            if(type.equals(aid.getType())) {
                aids.add(aid);
            }
        }
        return Collections.unmodifiableCollection(aids);
    }
    
    public static Collection<KnownAID> getAIDs() {
        return Collections.unmodifiableCollection(knownAIDsMap.values());
    }
    
    public static KnownAID searchAID(byte[] aidBytes){
        return knownAIDsMap.get(new AID(aidBytes));
    }

    static {
        _initFromFile("/aidlist.xml");
    }

    private static void _initFromFile(String filename) {
        try {
            XMLElement aidListElement = new XMLElement();
            aidListElement.parseFromReader(new InputStreamReader(Util.loadResource(KnownAIDList.class, filename), "UTF-8"));

            if (!"AIDList".equalsIgnoreCase(aidListElement.getName())) {
                throw new RuntimeException("Unexpected Root Element: <" + aidListElement.getName() + "> . Expected <AIDList>");
            }
            for (Object aidListChildObject : aidListElement.getChildren()) {
                XMLElement appElement = (XMLElement) aidListChildObject;
                String appElementName = appElement.getName();

                if (!"Application".equalsIgnoreCase(appElementName)) {
                    throw new RuntimeException("Unexpected XML Element: <" + appElementName + "> . Expected <Application>");
                }
                String aidStr = appElement.getStringAttribute("AID");
                AID aid = new AID(aidStr);
                boolean supported = appElement.getBooleanAttribute("Supported", "true", "false", false);
                String asiStr = appElement.getStringAttribute("ASI");
                String type = appElement.getStringAttribute("Type");
                String name = appElement.getStringAttribute("Name");
                String description = appElement.getStringAttribute("Description");
                knownAIDsMap.put(aid, new KnownAID(name, aid, type, supported, ApplicationSelectionIndicator.valueOf(asiStr), description));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    public static void main(String[] args) {
        for (KnownAID can : knownAIDsMap.values()) {
            System.out.println(can);
        }
    }
}
