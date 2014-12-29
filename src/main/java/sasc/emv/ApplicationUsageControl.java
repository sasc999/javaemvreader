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
 * Application Usage Control
 * Indicates issuer‘s specified restrictions on the geographic usage and services allowed for the application
 *
 * EMV Book 3 Annex C3 (page 183)
 * 
 * @author sasc
 */
public class ApplicationUsageControl {

    private byte firstByte;
    private byte secondByte;

    public ApplicationUsageControl(byte firstByte, byte secondByte) {
        this.firstByte = firstByte;
        this.secondByte = secondByte;
    }

    public boolean validForDomesticCashTransactions() {
        return (firstByte & 0xFF & (byte) 0x80) > 0;
    }

    public boolean validForInternationalCashTransactions() {
        return (firstByte & (byte) 0x40) > 0;
    }

    public boolean validForDomesticGoods() {
        return (firstByte & (byte) 0x20) > 0;
    }

    public boolean validForInternationalGoods() {
        return (firstByte & (byte) 0x10) > 0;
    }

    public boolean validForDomesticServices() {
        return (firstByte & (byte) 0x08) > 0;
    }

    public boolean validForInternationalServices() {
        return (firstByte & (byte) 0x04) > 0;
    }

    public boolean validAtATMs() {
        return (firstByte & (byte) 0x02) > 0;
    }

    public boolean validAtTerminalsOtherThanATMs() {
        return (firstByte & (byte) 0x01) > 0;
    }

    public boolean domesticCashbackAllowed() {
        return (secondByte & 0xFF & (byte) 0x80) > 0;
    }

    public boolean internationalCashbackAllowed() {
        return (secondByte & (byte) 0x40) > 0;
    }

    //The rest of the bits in the second byte are RFU (Reserved for Future Use)

    public String getValidForDomesticCashTransactionsString(){
        if(validForDomesticCashTransactions()){
            return "Valid for domestic cash transactions";
        }else{
            return "Not valid for domestic cash transactions";
        }
    }
    
    public String getValidForInternationalCashTransactionsString(){
        if(validForInternationalCashTransactions()){
            return "Valid for international cash transactions";
        }else{
            return "Not valid for international cash transactions";
        }
    }

    public String getValidForDomesticGoodsString(){
        if(validForDomesticGoods()){
            return "Valid for domestic goods";
        }else{
            return "Not valid for domestic goods";
        }
    }

    public String getValidForInternationalGoodsString(){
        if(validForInternationalGoods()){
            return "Valid for international goods";
        }else{
            return "Not valid for international goods";
        }
    }

    public String getValidForDomesticServicesString(){
        if(validForDomesticServices()){
            return "Valid for domestic services";
        }else{
            return "Not valid for domestic services";
        }
    }

    public String getValidForInternationalServicesString(){
        if(validForInternationalGoods()){
            return "Valid for international services";
        }else{
            return "Not valid for international services";
        }
    }

    public String getValidAtATMsString(){
        if(validAtATMs()){
            return "Valid at ATMs";
        }else{
            return "Not valid at ATMs";
        }
    }

    public String getValidAtTerminalsOtherThanATMsString(){
        if(validAtTerminalsOtherThanATMs()){
            return "Valid at terminals other than ATMs";
        }else{
            return "Not valid at terminals other than ATMs";
        }
    }

    public String getDomesticCashbackAllowedString(){
        if(domesticCashbackAllowed()){
            return "Domestic cashback allowed";
        }else{
            return "Domestic cashback not allowed";
        }
    }

    public String getInternationalCashbackAllowedString(){
        if(internationalCashbackAllowed()){
            return "International cashback allowed";
        }else{
            return "International cashback not allowed";
        }
    }

    @Override
    public String toString(){
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent){
        pw.println(Util.getSpaces(indent)+"Application Usage Control");

        String indentStr = Util.getSpaces(indent+Log.INDENT_SIZE);

        pw.println(indentStr + getValidForDomesticCashTransactionsString());
        pw.println(indentStr + getValidForInternationalCashTransactionsString());
        pw.println(indentStr + getValidForDomesticGoodsString());
        pw.println(indentStr + getValidForInternationalGoodsString());

        pw.println(indentStr + getValidForDomesticServicesString());
        pw.println(indentStr + getValidForInternationalServicesString());
        pw.println(indentStr + getValidAtATMsString());
        pw.println(indentStr + getValidAtTerminalsOtherThanATMsString());

        pw.println(indentStr + getDomesticCashbackAllowedString());
        pw.println(indentStr + getInternationalCashbackAllowedString());
    }

    public static void main(String[] args){
        ApplicationUsageControl auc = new ApplicationUsageControl((byte)0xab, (byte)0x80);
        System.out.println(auc.toString());

    }

}
