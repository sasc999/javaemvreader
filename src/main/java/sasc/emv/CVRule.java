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
import sasc.util.ISO4217_Numeric;
import sasc.util.Log;
import sasc.util.Util;

/**
 * Describes Card holder Verification Method (CVM) Codes
 * EMV book 3 page 184
 * 
 * @author sasc
 */
public class CVRule {
    
    public static enum Rule {
        FAIL_PROCESSING("Fail CVM processing"), 
        NO_CVM_REQUIRED("No CVM required"),
        PLAINTEXT_PIN_VERIFIED_BY_ICC("Plaintext PIN verification performed by ICC"), 
        PLAINTEXT_PIN_VERIFIED_BY_ICC_AND_SIGNATURE_ON_PAPER("Plaintext PIN verification performed by ICC and signature (paper)"),
        ENCIPHERED_PIN_VERIFIED_ONLINE("Enciphered PIN verified online"),
        ENCIPHERED_PIN_VERIFIED_BY_ICC("Enciphered PIN verification performed by ICC"),
        ENCIPHERED_PIN_VERIFIED_BY_ICC_AND_SIGNATURE_ON_PAPER("Enciphered PIN verification performed by ICC and signature (paper)"),
        SIGNATURE_ON_PAPER("Signature (paper)"),
        RESERVED_FOR_USE_BY_THE_INDIVIDUAL_PAYMENT_SYSTEMS("Reserved for use by individual payment systems"),
        RESERVED_FOR_USE_BY_THE_ISSUER("Reserved for use by the issuer"),
        NOT_AVAILABLE_FOR_USE("Value is not available for use"),
        RFU("Reserved for future use");
        
        private String description;
        
        private Rule(String description){
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public static enum Condition {
        
    }

    byte firstByte;
    byte secondByte;
    byte[] amountFieldXBytes;
    String amountFieldXStr;
    byte[] secondAmountFieldYBytes;
    String secondAmountFieldYStr;
    Rule rule;

    public CVRule(byte firstByte, byte secondByte, byte[] amountFieldX, byte[] secondAmountFieldY) {
        this.firstByte = firstByte;
        this.secondByte = secondByte;
        this.amountFieldXBytes = amountFieldX;
        this.amountFieldXStr = CVRule.formatAmountField(amountFieldX);
        this.secondAmountFieldYBytes = secondAmountFieldY;
        this.secondAmountFieldYStr = CVRule.formatAmountField(secondAmountFieldYBytes);
        
        Log.debug("first byte: " + Util.byte2Hex(firstByte));
        
        if(failCVMProcessing()){
            rule = Rule.FAIL_PROCESSING;
        }else if(noCVMRequired()) {
            rule = Rule.NO_CVM_REQUIRED;
        }else if(plaintextPINVerificationPerformedByICC()) {
            rule = Rule.PLAINTEXT_PIN_VERIFIED_BY_ICC;
        }else if(plaintextPINVerificationPerformedByICCAndSignature_paper_()) {
            rule = Rule.PLAINTEXT_PIN_VERIFIED_BY_ICC_AND_SIGNATURE_ON_PAPER;
        }else if(encipheredPINVerifiedOnline()) {
            rule = Rule.ENCIPHERED_PIN_VERIFIED_ONLINE;
        }else if(encipheredPINVerificationPerformedByICC()) {
            rule = Rule.ENCIPHERED_PIN_VERIFIED_BY_ICC;
        }else if(encipheredPINVerificationPerformedByICCAndSignature_paper_()) {
            rule = Rule.ENCIPHERED_PIN_VERIFIED_BY_ICC_AND_SIGNATURE_ON_PAPER;
        }else if(signature_paper_()) {
            rule = Rule.SIGNATURE_ON_PAPER;
        }else if((firstByte & (byte) 0x30) == 0x20) {
        	Log.debug("for payment sytem");
            rule = Rule.RESERVED_FOR_USE_BY_THE_INDIVIDUAL_PAYMENT_SYSTEMS;
        }else if((firstByte & (byte) 0x30) == 0x30) {
            rule = Rule.RESERVED_FOR_USE_BY_THE_ISSUER;
        }else /*if(firstByte >= 0x06 && firstByte <= 0x1D)*/ {
            rule = Rule.RFU;
        }
    }

    //These 2 are mutually exclusive
    //Most significant bit (0x80) of the first byte is RFU
    public final boolean failCardholderVerificationIfThisCVMIsUnsuccessful() {
        return (firstByte & (byte) 0x40) == 0;
    }

    public final boolean applySucceedingCVRuleIfThisCVMIsUnsuccessful() {
        return (firstByte & (byte) 0x40) == (byte) 0x40;
    }

    public final boolean failCVMProcessing() {
        return (firstByte & (byte) 0x3F) == 0;
    }

    public final boolean plaintextPINVerificationPerformedByICC() {
        return (firstByte & (byte) 0x3F) == (byte) 0x01;
    }

    public final boolean encipheredPINVerifiedOnline() {
        return (firstByte & (byte) 0x3F) == (byte) 0x02;
    }

    public final boolean plaintextPINVerificationPerformedByICCAndSignature_paper_() {
        return (firstByte & (byte) 0x3F) == (byte) 0x03;
    }

    public final boolean encipheredPINVerificationPerformedByICC() {
        return (firstByte & (byte) 0x3F) == (byte) 0x04;
    }

    public final boolean encipheredPINVerificationPerformedByICCAndSignature_paper_() {
        return (firstByte & (byte) 0x3F) == (byte) 0x05;
    }

    //Values in the range 000110 (0x06) - 011101 (0x1D) reserved for future use

    public final boolean signature_paper_() {
        return (firstByte & (byte) 0x3F) == (byte) 0x1E;
    }

    public final boolean noCVMRequired() {
        return (firstByte & (byte) 0x3F) == (byte) 0x1F;
    }
    
    public boolean isPinRelated() {
        return plaintextPINVerificationPerformedByICC() 
                || encipheredPINVerificationPerformedByICC()
                || plaintextPINVerificationPerformedByICCAndSignature_paper_()
                || encipheredPINVerificationPerformedByICCAndSignature_paper_()
                || encipheredPINVerifiedOnline();
    }
    
    public Rule getRule(){
        return rule;
    }

    public String getCVMUnsuccessfulRuleString() {
        if (failCardholderVerificationIfThisCVMIsUnsuccessful()) {
            return "Fail cardholder verification if this CVM is unsuccessful";
        } else {
            return "Apply succeeding CV Rule if this CVM is unsuccessful";
        }
    }

    public String getRuleString(){
        return rule.getDescription();
    }

    private static String formatAmountField(byte[] amount){ //TODO Application Currency Code
        StringBuilder sb = new StringBuilder(String.valueOf(Util.byteArrayToInt(amount)));
        while(sb.length() < 3){
            sb.insert(0, '0');
        }
        sb.insert(sb.length()-2, ".");
        
        int currencyCode = -1; //TODO get from app (if available)
        if(currencyCode != -1){
            String codeAlpha3 = ISO4217_Numeric.getCurrencyForCode(currencyCode).getCode();
            if(codeAlpha3 != null && !codeAlpha3.isEmpty()) {
                sb.append(" ").append(codeAlpha3);
            }
        }
        
        return sb.toString();
    }


    //Second byte (Condition Codes)

    //EMV book3 Table 40 (page 185)

    //CV Rule Byte 2 (Rightmost): Cardholder Verification Method (CVM) Condition Codes
    //Value Meaning
    //'00' Always
    //'01' If unattended cash
    //'02' If not unattended cash and not manual cash and not purchase with cashback
    //'03' If terminal supports the CVM 
    //'04' If manual cash
    //'05' If purchase with cashback
    //'06' If transaction is in the application currency and is under X value (see section 10.5 for a discussion of X)
    //'07' If transaction is in the application currency and is over X value
    //'08' If transaction is in the application currency and is under Y value (see section 10.5 for a discussion of Y)
    //'09' If transaction is in the application currency and is over Y value
    //'0A' - '7F' RFU
    //'80' - 'FF' Reserved for use by individual payment systems

    public String getConditionCodeDescription() {
        switch (secondByte) {
            case 0x00:
                return "Always";
            case 0x01:
                return "If unattended cash";
            case 0x02:
                return "If not unattended cash and not manual cash and not purchase with cashback";
            case 0x03:
                return "If terminal supports the CVM";
            case 0x04:
                return "If manual cash";
            case 0x05:
                return "If purchase with cashback";
            case 0x06:
                return "If transaction is in the application currency and is under " + amountFieldXStr + " value";
            case 0x07:
                return "If transaction is in the application currency and is over " + amountFieldXStr + " value";
            case 0x08:
                return "If transaction is in the application currency and is under " + secondAmountFieldYStr + " value";
            case 0x09:
                return "If transaction is in the application currency and is over " + secondAmountFieldYStr + " value";
            default:
                if (secondByte <= 0x7F) { //0x0A - 0x7F
                    return "RFU";
                } else { //0x80 - 0xFF
                    return "Reserved for use by individual payment systems";
                }

        }
    }
    
    public byte getConditionCode() {
        return secondByte;
    }
    
    public byte getCodeCode() {
        return firstByte;
    }
    
    public boolean getConditionAlways() {
        return secondByte == 0x00;
    }
    
    public boolean getConditionTerminalSupportCVM() {
        return secondByte == 0x03;
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "Cardholder Verification Rule");
        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);

        pw.println(indentStr + "Rule: "+getRuleString());
        pw.println(indentStr + "Condition Code: "+getConditionCodeDescription());

        pw.println(indentStr + getCVMUnsuccessfulRuleString());

    }
}
