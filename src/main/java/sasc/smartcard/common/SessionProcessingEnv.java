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

/**
 *
 * @author sasc
 */
public class SessionProcessingEnv {
    private boolean maskSensitiveInformation = false;
    private boolean bruteForceSFIRecords = false;
    private boolean readMasterFile = false;
    private boolean warmUpCard = false;
    private boolean selectAllRIDs = false;
    private boolean probeAllKnownAIDs = false;
    private boolean discoverTerminalFeatures = false;
    private int initialPauseMillis = 100;
    
    public SessionProcessingEnv(){
        
    }
    
    public boolean getReadMasterFile(){
        return readMasterFile;
    }
    
    public void setReadMasterFile(boolean value){
        readMasterFile = value;
    }
    
    public boolean getWarmUpCard(){
        return warmUpCard;
    }
    
    public void setWarmUpCard(boolean value){
        warmUpCard = value;
    }
    
    /**
     * If all 5 byte RIDs should be selected (possibly for partial selection)
     * @return true if all RID should be tested when scanning the card
     */
    public boolean getSelectAllRIDs(){
        return selectAllRIDs;
    }

    public void setSelectAllRIDs(boolean value){
        selectAllRIDs = value;
    }
    
    public boolean getProbeAllKnownAIDs(){
        return probeAllKnownAIDs;
    }

    public void setProbeAllKnownAIDs(boolean value){
        probeAllKnownAIDs = value;
    }
    
    public int getInitialPauseMillis(){
        return initialPauseMillis;
    }
    
    /**
     * Set the delay in milliseconds between PowerOn 
     * and the first command sent to the card
     * @param millis 
     */
    public void setInitialPauseMillis(int millis){
        this.initialPauseMillis = millis;
    }
    
    public boolean getDiscoverTerminalFeatures(){
        return discoverTerminalFeatures;
    }
    
    public void setDiscoverTerminalFeatures(boolean value) {
        this.discoverTerminalFeatures = value;
    }
}
