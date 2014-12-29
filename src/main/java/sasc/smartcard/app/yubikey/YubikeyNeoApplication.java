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

import java.io.PrintWriter;
import sasc.smartcard.common.SmartCard;
import sasc.iso7816.AID;
import sasc.iso7816.Application;
import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class YubikeyNeoApplication implements Application {

    private AID aid;
    private int versionMajor;
    private int versionMinor;
    private int versionBuild;
    private int programmingSeqNum; //Programming Sequence Number
    private int touchDetectorLevel;
    private byte modeByte;
    private Mode deviceMode;
    private byte flags;
    private int challengeResponseTimeout;
    private int autoEjectTime;
    private SmartCard card = null;

    @Override
    public SmartCard getCard() {
        return card;
    }
    
    public static enum Mode{
        OTP(1), CCID(2), OTP_CCID(3);
        
        Mode(int modeInt){
            
        }
    }
    
    public YubikeyNeoApplication(AID aid){
        this.aid = aid;
    }
    
    @Override
    public AID getAID() {
        return aid;
    }
    
    /**
     * @return the versionMajor
     */
    public int getVersionMajor() {
        return versionMajor;
    }

    /**
     * @param versionMajor the versionMajor to set
     */
    public void setVersionMajor(int versionMajor) {
        this.versionMajor = versionMajor;
    }

    /**
     * @return the versionMinor
     */
    public int getVersionMinor() {
        return versionMinor;
    }

    /**
     * @param versionMinor the versionMinor to set
     */
    public void setVersionMinor(int versionMinor) {
        this.versionMinor = versionMinor;
    }

    /**
     * @return the versionBuild
     */
    public int getVersionBuild() {
        return versionBuild;
    }

    /**
     * @param versionBuild the versionBuild to set
     */
    public void setVersionBuild(int versionBuild) {
        this.versionBuild = versionBuild;
    }

    /**
     * @return the programmingSeqNum
     */
    public int getProgrammingSeqNum() {
        return programmingSeqNum;
    }

    /**
     * @param programmingSeqNum the programmingSeqNum to set
     */
    public void setProgrammingSeqNum(int programmingSeqNum) {
        this.programmingSeqNum = programmingSeqNum;
    }

    /**
     * @return the touchDetectorLevel
     */
    public int getTouchDetectorLevel() {
        return touchDetectorLevel;
    }

    /**
     * @param touchDetectorLevel the touchDetectorLevel to set
     */
    public void setTouchDetectorLevel(int touchDetectorLevel) {
        this.touchDetectorLevel = touchDetectorLevel;
    }
    
    /**
     * @return the modeByte
     */
    public byte getModeByte() {
        return modeByte;
    }

    /**
     * @param modeByte the modeByte to set
     */
    public void setModeByte(byte modeByte) {
        this.modeByte = modeByte;
    }

    /**
     * @return the deviceMode
     */
    public int getDeviceMode() {
        return (modeByte & Yubikey.MODE_MASK);
    }
//
//    /**
//     * @param deviceMode the deviceMode to set
//     */
//    public void setDeviceMode(Mode deviceMode) {
//        this.deviceMode = deviceMode;
//    }

    /**
     * @return the flags
     */
    public byte getFlags() {
        return (byte)(modeByte & ~Yubikey.MODE_MASK);
    }

//    /**
//     * @param flags the flags to set
//     */
//    public void setFlags(byte flags) {
//        this.flags = flags;
//    }

    /**
     * @return the challengeResponseTimeout
     */
    public int getChallengeResponseTimeout() {
        return challengeResponseTimeout;
    }

    /**
     * @param challengeResponseTimeout the challengeResponseTimeout to set
     */
    public void setChallengeResponseTimeout(int challengeResponseTimeout) {
        this.challengeResponseTimeout = challengeResponseTimeout;
    }

    /**
     * @return the autoEjectTime
     */
    public int getAutoEjectTime() {
        return autoEjectTime;
    }

    /**
     * @param autoEjectTime the autoEjectTime to set
     */
    public void setAutoEjectTime(int autoEjectTime) {
        this.autoEjectTime = autoEjectTime;
    }
    
    @Override
    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "Yubikey NEO Admin Application");

        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);
        
        if (aid != null) {
            aid.dump(pw, indent + Log.INDENT_SIZE);
        }
        
        pw.println(indentStr+"Firmware Version           : "+getVersionMajor()+"."+getVersionMinor()+"."+getVersionBuild());
        pw.println(indentStr+"Programming sequence number: "+getProgrammingSeqNum());
        pw.println(indentStr+"Level from touch detector  : "+getTouchDetectorLevel());
        int mode = getDeviceMode();
        String modeDescription = "";
        if(mode == 0){
            modeDescription = " (OTP)";
        }else if(mode == 1){
            modeDescription = " (CCID)";
        }else if(mode == 2){
            modeDescription = " (OTP+CCID)";
        }
        byte flagsByte = getFlags();
        String flagsDescription = "";
        if(flagsByte == (byte)0x80){
            flagsDescription = " (Eject)";
        }
        
        pw.println(indentStr+"Device mode                : "+mode + modeDescription); //getDeviceMode();
        pw.println(indentStr+"Flags                      : 0x"+Util.byte2Hex(flagsByte) + flagsDescription); //getFlags();
        pw.println(indentStr+"Challenge-response timeout : "+getChallengeResponseTimeout() + " seconds");
        pw.println(indentStr+"Auto eject time in seconds : "+getAutoEjectTime());
    }
}
