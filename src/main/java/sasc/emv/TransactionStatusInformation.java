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
package sasc.emv;

import java.io.PrintWriter;
import java.io.StringWriter;
import sasc.util.Log;
import sasc.util.Util;

/**
 * Transaction Status Information (TSI)
 *
 * @author sasc
 */
public class TransactionStatusInformation {
    private byte firstByte;
    private byte secondByte;

    public TransactionStatusInformation(){
        this((byte)0x00, (byte)0x00);  //Initialize with all bits unset
    }

    public TransactionStatusInformation(byte firstByte, byte secondByte) {
        this.firstByte = firstByte;
        this.secondByte = secondByte;
    }

    public boolean offlineDataAuthenticationWasPerformed() {
        return Util.isBitSet(firstByte, 8);
    }

    public void setOfflineDataAuthenticationWasPerformed(boolean value) {
        Util.setBit(firstByte, 8, value);
    }

    public boolean cardholderVerificationWasPerformed() {
        return Util.isBitSet(firstByte, 7);
    }

    public void setCardholderVerificationWasPerformed(boolean value) {
        Util.setBit(firstByte, 7, value);
    }

    public boolean cardRiskManagementWasPerformed() {
        return Util.isBitSet(firstByte, 6);
    }

    public void setCardRiskManagementWasPerformed(boolean value) {
        Util.setBit(firstByte, 6, value);
    }

    public boolean issuerAuthenticationWasPerformed() {
        return Util.isBitSet(firstByte, 5);
    }

    public void setIssuerAuthenticationWasPerformed(boolean value) {
        Util.setBit(firstByte, 5, value);
    }

    public boolean terminalRiskManagementWasPerformed() {
        return Util.isBitSet(firstByte, 4);
    }

    public void setTerminalRiskManagementWasPerformed(boolean value) {
        Util.setBit(firstByte, 4, value);
    }

    public boolean scriptProcessingWasPerformed() {
        return Util.isBitSet(firstByte, 3);
    }

    public void setScriptProcessingWasPerformed(boolean value) {
        Util.setBit(firstByte, 3, value);
    }

    //The rest of the bits + secondByte are RFU (Reserved for Future Use)

    public String getOfflineDataAuthenticationWasPerformedString(){
        if(offlineDataAuthenticationWasPerformed()){
            return "Offline data authentication was performed";
        }else{
            return "Offline data authentication was not performed";
        }
    }

    public String getCardholderVerificationWasPerformedString(){
        if(cardholderVerificationWasPerformed()){
            return "Cardholder verification was performed";
        }else{
            return "Cardholder verification was not performed";
        }
    }

    public String getCardRiskManagementWasPerformedString(){
        if(cardRiskManagementWasPerformed()){
            return "Card risk management was performed";
        }else{
            return "Card risk management was not performed";
        }
    }

    public String getIssuerAuthenticationWasPerformedString(){
        if(issuerAuthenticationWasPerformed()){
            return "Issuer authentication was performed";
        }else{
            return "Issuer authentication was not performed";
        }
    }

    public String getTerminalRiskManagementWasPerformedString(){
        if(terminalRiskManagementWasPerformed()){
            return "Terminal risk management was performed";
        }else{
            return "Terminal risk management was not performed";
        }
    }

    public String getScriptProcessingWasPerformedString(){
        if(scriptProcessingWasPerformed()){
            return "Script processing was performed";
        }else{
            return "Script processing was not performed";
        }
    }

    public void reset() {
        firstByte = 0x00;
        secondByte = 0x00;
    }
    
    public byte[] getBytes(){
        return new byte[]{firstByte, secondByte};
    }

    @Override
    public String toString(){
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent){
        pw.println(Util.getSpaces(indent)+"Transaction Status Information:");
        String indentStr = Util.getSpaces(indent+Log.INDENT_SIZE);

        pw.println(indentStr+getOfflineDataAuthenticationWasPerformedString());
        pw.println(indentStr+getCardholderVerificationWasPerformedString());
        pw.println(indentStr+getCardRiskManagementWasPerformedString());
        pw.println(indentStr+getIssuerAuthenticationWasPerformedString());
        pw.println(indentStr+getTerminalRiskManagementWasPerformedString());
        pw.println(indentStr+getScriptProcessingWasPerformedString());
    }

    public static void main(String[] args){
        TransactionStatusInformation tsi;
        tsi = new TransactionStatusInformation((byte)0x68, (byte)0x00);
        System.out.println(tsi.toString());
        tsi = new TransactionStatusInformation((byte)0xE8, (byte)0x00);
        System.out.println(tsi.toString()); //VISA Comfort Hotel 
        tsi = new TransactionStatusInformation((byte)0xF8, (byte)0x00);
        System.out.println(tsi.toString()); //VISA Inside premium hotel (DE)
    }

}
