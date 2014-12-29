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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import sasc.iso7816.Tag;
import sasc.iso7816.TagAndLength;
import sasc.iso7816.TagImpl;
import sasc.iso7816.TagValueType;
import sasc.util.ISO4217_Numeric;
import sasc.util.Log;
import sasc.util.Util;

/**
 * Representation of a Point of Sale (POS)
 * 
 * There is only 1 Terminal
 * 
 * @author sasc
 */
public class EMVTerminal {

    private final static Properties defaultTerminalProperties = new Properties();
    private final static Properties runtimeTerminalProperties = new Properties();
    private final static TerminalVerificationResults terminalVerificationResults = new TerminalVerificationResults();
    private final static CVMResults cvmResults = new CVMResults();
    
    private static CallbackHandler pinCallbackHandler;
    
    private static boolean doVerifyPinIfRequired = false;
    private static boolean isOnline = true;
    
    static {
        
        try {
            //Default properties
            defaultTerminalProperties.load(EMVTerminal.class.getResourceAsStream("/terminal.properties"));
            for (String key : defaultTerminalProperties.stringPropertyNames()) {
                //Sanitize
                String sanitizedKey = Util.byteArrayToHexString(Util.fromHexString(key)).toLowerCase();
                String sanitizedValue = Util.byteArrayToHexString(Util.fromHexString(defaultTerminalProperties.getProperty(key))).toLowerCase();
                defaultTerminalProperties.setProperty(sanitizedKey, sanitizedValue);
            }
            //Runtime/overridden properties
            String runtimeTerminalPropertiesFile = System.getProperty("/terminal.properties");
            if (runtimeTerminalPropertiesFile != null) {
                runtimeTerminalProperties.load(new FileInputStream(runtimeTerminalPropertiesFile));
                for(String key : runtimeTerminalProperties.stringPropertyNames()) {
                    //Sanitize
                    String sanitizedKey   = Util.byteArrayToHexString(Util.fromHexString(key)).toLowerCase();
                    String sanitizedValue = Util.byteArrayToHexString(Util.fromHexString(runtimeTerminalProperties.getProperty(key))).toLowerCase();
                    if(defaultTerminalProperties.contains(sanitizedKey) && sanitizedValue.length() != defaultTerminalProperties.getProperty(key).length()) {
                        //Attempt to set different length for a default value
                        throw new RuntimeException("Attempted to set a value with unsupported length for key: " + sanitizedKey + " (value: "+sanitizedValue+")");
                    }
                    runtimeTerminalProperties.setProperty(sanitizedKey, sanitizedValue);
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }

    //PDOL (Processing options Data Object List)
    //DDOL (*Default* Dynamic Data Authentication Data Object List)
    //     (Default to be used for constructing the INTERNAL AUTHENTICATE command if the DDOL in the card is not present)
    //TDOL (*Default* Transaction Certificate Data Object List)
    //     (Default to be used for generating the TC Hash Value if the TDOL in the card is not present)
    
    //PDOL example (Visa Electron, contactless)
//    9f 38 18 -- Processing Options Data Object List (PDOL)
//         9f 66 04 -- Terminal Transaction Qualifiers
//         9f 02 06 -- Amount, Authorised (Numeric)
//         9f 03 06 -- Amount, Other (Numeric)
//         9f 1a 02 -- Terminal Country Code
//         95 05 -- Terminal Verification Results (TVR)
//         5f 2a 02 -- Transaction Currency Code
//         9a 03 -- Transaction Date
//         9c 01 -- Transaction Type
//         9f 37 04 -- Unpredictable Number
    public static byte[] getTerminalResidentData(TagAndLength tal, EMVApplication app) {
        //Check if the value is specified in the runtime properties file
        String propertyValueStr = runtimeTerminalProperties.getProperty(Util.byteArrayToHexString(tal.getTag().getTagBytes()).toLowerCase());

        if(propertyValueStr != null) {
            byte[] propertyValue = Util.fromHexString(propertyValueStr);

            if (propertyValue.length == tal.getLength()) {
                return propertyValue;
            }
        }
        
        if (tal.getTag().equals(EMVTags.TERMINAL_COUNTRY_CODE) && tal.getLength() == 2) {
            return findCountryCode(app);
        } else if (tal.getTag().equals(EMVTags.TRANSACTION_CURRENCY_CODE) && tal.getLength() == 2) {
            return findCurrencyCode(app);
        }
        
        //Now check for default values
        propertyValueStr = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(tal.getTag().getTagBytes()).toLowerCase());

        if(propertyValueStr != null) {
            byte[] propertyValue = Util.fromHexString(propertyValueStr);

            if (propertyValue.length == tal.getLength()) {
                return propertyValue;
            }
        }

        if (tal.getTag().equals(EMVTags.UNPREDICTABLE_NUMBER)) {
            return Util.generateRandomBytes(tal.getLength());
        } else if (tal.getTag().equals(EMVTags.TERMINAL_TRANSACTION_QUALIFIERS) && tal.getLength() == 4) {
            //This seems only to be used in contactless mode. Construct accordingly
            TerminalTransactionQualifiers ttq = new TerminalTransactionQualifiers();
            ttq.setContactlessEMVmodeSupported(true);
            ttq.setReaderIsOfflineOnly(true);
            return ttq.getBytes();
        } else if (tal.getTag().equals(EMVTags.TERMINAL_VERIFICATION_RESULTS) && tal.getLength() == 5) {
            //All bits set to '0'
            return terminalVerificationResults.toByteArray();
        } else if (tal.getTag().equals(EMVTags.TRANSACTION_DATE) && tal.getLength() == 3) {
            return Util.getCurrentDateAsNumericEncodedByteArray();
        } else if (tal.getTag().equals(EMVTags.TRANSACTION_TYPE) && tal.getLength() == 1) {
            //transactionTypes = {     0:  "Payment",     1:  "Withdrawal", } 
            //http://www.codeproject.com/Articles/100084/Introduction-to-ISO-8583
            return new byte[]{0x00};
        } else {
            Log.debug("Terminal Resident Data not found for " + tal);
        }
        byte[] defaultResponse = new byte[tal.getLength()];
        Arrays.fill(defaultResponse, (byte) 0x00);
        return defaultResponse;
    }

    public static TerminalVerificationResults getTerminalVerificationResults() {
        return terminalVerificationResults;
    }
    
    public static void resetTVR(){
        terminalVerificationResults.reset();
    }
    
    public static CVMResults getCVMResults()
    {
    	return cvmResults;
    }
    
    public static void resetCVMResults()
    {
    	cvmResults.reset();
    }
    
    public static void setProperty(String tagHex, String valueHex) {
        setProperty(new TagImpl(tagHex, TagValueType.BINARY, "", ""), Util.fromHexString(valueHex));
    }
    
    public static void setProperty(Tag tag, byte[] value){
        runtimeTerminalProperties.setProperty(Util.byteArrayToHexString(tag.getTagBytes()).toLowerCase(Locale.US), Util.byteArrayToHexString(value));
    }
    
    public static boolean isCDASupported(EMVApplication app) {
    	String tc = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.TERMINAL_CAPABILITIES.getTagBytes()));
    	if(tc == null || tc.length() < 6) {
    		return false;
        }
    	
    	byte[] t = Util.fromHexString(tc);

    	return (t[2] & 0x08) == 0x08;
    }
    
    public static boolean isDDASupported(EMVApplication app) {
    	String tc = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.TERMINAL_CAPABILITIES.getTagBytes()));
    	if(tc == null || tc.length() < 6) {
    		return false;
        }
        
    	byte[] t = Util.fromHexString(tc);

    	return (t[2] & 0x40) == 0x40;
    }
    
    public static boolean isSDASupported(EMVApplication app) {
    	String tc = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.TERMINAL_CAPABILITIES.getTagBytes()));
    	if(tc == null || tc.length() < 6) {
    		return false;
        }
    	
    	byte[] t = Util.fromHexString(tc);

    	return (t[2] & 0x80) == 0x80;
    }
    
    public static boolean isATM() {
        return false;
    }
    
    public static Date getCurrentDate() {
        return new Date();
    }
    
    public static int getSupportedApplicationVersionNumber(EMVApplication app) {
        //For now, just return the version number maintained in the card
    	String value = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.APP_VERSION_NUMBER_TERMINAL.getTagBytes()));
    	if(value == null || value.length() < 4) {
    		return 0;
        }
    	byte[] t = Util.fromHexString(value);

        return Util.byteToInt(t[0], t[1]);
    }
    
    public static boolean isCashTrx() {
    	String value = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.ADDITIONAL_TERMINAL_CAPABILITIES.getTagBytes()));
    	if(value == null || value.length() < 10) {
    		return false;
        }
    	byte[] t = Util.fromHexString(value);
    	
    	return (t[0] & 0x80) == 0x80;
    }
    
    public static boolean isGoodsTrx() {
    	String value = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.ADDITIONAL_TERMINAL_CAPABILITIES.getTagBytes()));
    	if(value == null || value.length() < 10) {
    		return false;
        }
    	byte[] t = Util.fromHexString(value);
    	
    	return (t[0] & 0x40) == 0x40;
    }
    
    public static boolean isServicesTrx() {
    	String value = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.ADDITIONAL_TERMINAL_CAPABILITIES.getTagBytes()));
    	if(value == null || value.length() < 10) {
    		return false;
        }
    	byte[] t = Util.fromHexString(value);
    	
    	return (t[0] & 0x20) == 0x20;
    }
    
    public static boolean isCashbackTrx() {
    	String value = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.ADDITIONAL_TERMINAL_CAPABILITIES.getTagBytes()));
    	if(value == null || value.length() < 10) {
    		return false;
        }
    	byte[] t = Util.fromHexString(value);
    	
    	return (t[0] & 0x10) == 0x10;
    }
    
    public static boolean isInquiryTrx() {
    	String value = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.ADDITIONAL_TERMINAL_CAPABILITIES.getTagBytes()));
    	if(value == null || value.length() < 10) {
    		return false;
        }
    	byte[] t = Util.fromHexString(value);
    	
    	return (t[0] & 0x08) == 0x08;
    }
    
    public static boolean isTransferTrx() {
    	String value = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.ADDITIONAL_TERMINAL_CAPABILITIES.getTagBytes()));
    	if(value == null || value.length() < 10) {
    		return false;
        }
    	byte[] t = Util.fromHexString(value);
    	
    	return (t[0] & 0x04) == 0x04;
    }
    
    public static boolean isPaymentTrx() {
    	String value = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.ADDITIONAL_TERMINAL_CAPABILITIES.getTagBytes()));
    	if(value == null || value.length() < 10) {
    		return false;
        }
    	byte[] t = Util.fromHexString(value);
    	
    	return (t[0] & 0x02) == 0x02;
    }
    
    public static boolean isAdminTrx() {
    	String value = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.ADDITIONAL_TERMINAL_CAPABILITIES.getTagBytes()));
    	if(value == null || value.length() < 10) {
    		return false;
        }
    	byte[] t = Util.fromHexString(value);
    	
    	return (t[0] & 0x01) == 0x01;
    }
    
    public static boolean isCVMRecognized(EMVApplication app, CVRule rule) {
        switch(rule.getRule()) {
            case RESERVED_FOR_USE_BY_THE_INDIVIDUAL_PAYMENT_SYSTEMS:
                //app.getAID().getRIDBytes();
                //TODO check if RID specific rule is supported
                //if(supported) {
                //    return true;
                //}
            case RESERVED_FOR_USE_BY_THE_ISSUER:
                
                if(app.getIssuerIdentificationNumber() != null){
                    //TODO check if issuer specific rule is supported
                    //if(supported){
                    //  return true;
                    //}
                }
            case NOT_AVAILABLE_FOR_USE:
            case RFU:
                return false;
			default:
				break;
        }
        return true;
    }
    
    public static boolean isCVMSupported(CVRule rule) {
        switch(rule.getRule()) {
            //TODO support enciphered PIN
            case ENCIPHERED_PIN_VERIFIED_BY_ICC:
            case ENCIPHERED_PIN_VERIFIED_BY_ICC_AND_SIGNATURE_ON_PAPER:
                return false;
            case PLAINTEXT_PIN_VERIFIED_BY_ICC_AND_SIGNATURE_ON_PAPER:
            	return hasPinInputOfflineCapability() && hasSignatureOnPaper();
            case PLAINTEXT_PIN_VERIFIED_BY_ICC:
                return hasPinInputOfflineCapability();
            case SIGNATURE_ON_PAPER:
                return hasSignatureOnPaper();
            case ENCIPHERED_PIN_VERIFIED_ONLINE:
                return allowVerifyEncipheredPinOnline();
            case FAIL_PROCESSING:
            	return true;
            case NO_CVM_REQUIRED:
                return isNoCVMRequired();
            default:
                break;
        }
        return false;
    }
    
    public static boolean isOnline() {
        return isOnline;
    }
    
    public static void setIsOnline(boolean value){
        isOnline = value;
    }
    
    public static boolean isCVMConditionSatisfied(CVRule rule) {
        if(rule.getConditionAlways()) {
            return true;
        }
        if(rule.getConditionCode() <= 0x05){
            //TODO
            return true;
        }else if(rule.getConditionCode() < 0x0A) {
            //TODO
            //Check for presence Application Currency Code or Amount, Authorised in app records?
            return true;
        } else { //RFU and proprietary
            return false;
        }
    }
    
    public static boolean allowVerifyEncipheredPinOnline() {
        if(!isOnline()) {
            return false;
        }
        String tc = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.TERMINAL_CAPABILITIES.getTagBytes()));
    	if(tc == null || tc.length() < 6) {
    		return false;
        }
    	
    	byte[] t = Util.fromHexString(tc);

    	return (t[1] & 0x40) == 0x40;
    }
    
    public static boolean hasSignatureOnPaper() {
    	String tc = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.TERMINAL_CAPABILITIES.getTagBytes()));
    	if(tc == null || tc.length() < 6) {
    		return false;
        }
    	
    	byte[] t = Util.fromHexString(tc);

    	return (t[1] & 0x20) == 0x20;
    }
    
    public static boolean isNoCVMRequired() {
    	String tc = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.TERMINAL_CAPABILITIES.getTagBytes()));
    	if(tc == null || tc.length() < 6) {
    		return false;
        }
    	
    	byte[] t = Util.fromHexString(tc);

    	return (t[1] & 0x08) == 0x08;
    }
    
    public static void setDoVerifyPinIfRequired(boolean value) {
        doVerifyPinIfRequired = value;
    }
    
    public static boolean getDoVerifyPinIfRequired() {
        return doVerifyPinIfRequired;
    }
    
    /**
     * 
     * @return true if a Pin CallbackHandler has be set
     */
    public static boolean hasPinInputOfflineCapability() {
    	String tc = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.TERMINAL_CAPABILITIES.getTagBytes()));
    	if(tc == null || tc.length() < 6) {
    		return false;
        }
    	
    	byte []t = Util.fromHexString(tc);
    	boolean isEnabledInProperties= (t[1] & 0x80) == 0x80;
    	
        return doVerifyPinIfRequired && pinCallbackHandler != null && isEnabledInProperties;
    }
    
    public static void setPinCallbackHandler(CallbackHandler callbackHandler) {
        pinCallbackHandler = callbackHandler;
    }
    
    public static PasswordCallback getPinInput() {
        CallbackHandler callBackHandler = pinCallbackHandler;
        if(callBackHandler == null){
            return null;
        }
        PasswordCallback passwordCallback = new PasswordCallback("Enter PIN", false);
        try{
            callBackHandler.handle(new Callback[]{passwordCallback});
        }catch(IOException ex){
            Log.info(Util.getStackTrace(ex));
        }catch(UnsupportedCallbackException ex){
            Log.info(Util.getStackTrace(ex));
        }
        return passwordCallback;
    }
    
    public static boolean getPerformTerminalRiskManagement() {
        return true;
    }
    
    public static byte[] getTerminalActionCode(Tag tag, EMVApplication app) {
        //Return Terminal Action Code value based on tal
    	String value = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(tag.getTagBytes()));
    	
    	if(value != null && value.length() == 10) {
    		return Util.fromHexString(value);
        }
            
    	byte[] defaultResponse = new byte[5];
        Arrays.fill(defaultResponse, (byte) 0x00);
        return defaultResponse;
    }
    
    public static int getTerminalType() {
        //Return Terminal Action Code value base on tal
    	String value = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.TERMINAL_TYPE.getTagBytes()));
        if(value == null) {
            return 0;
        }
    	return Integer.parseInt(value);
    }

    public static byte[] constructDOLResponse(DOL dol, EMVApplication app) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (TagAndLength tagAndLength : dol.getTagAndLengthList()) {
            byte[] data = getTerminalResidentData(tagAndLength, app);
            stream.write(data, 0, data.length);
        }
        return stream.toByteArray();
    }

    //The ICC may contain the DDOL, but there shall be a default DDOL in the terminal, 
    //specified by the payment system, for use in case the DDOL is not present in the ICC.
    public static byte[] getDefaultDDOLResponse(EMVApplication app) {
        //It is mandatory that the DDOL contains the Unpredictable Number generated by the terminal (tag '9F37', 4 bytes binary).
        byte[] unpredictableNumber = Util.generateRandomBytes(4);
        
        //TODO add other DDOL data specified by the payment system
        //if(app.getAID().equals(SOMEAID))
        
        return unpredictableNumber;
    }

    //Ex Banco BRADESCO (f0 00 00 00 03 00 01) failes GPO with wrong COUNTRY_CODE !
    public static byte[] findCountryCode(EMVApplication app) {
        if(app != null){
            if(app.getIssuerCountryCode() != -1){
                byte[] countryCode = Util.intToBinaryEncodedDecimalByteArray(app.getIssuerCountryCode());
                return Util.resizeArray(countryCode, 2);
            }
        }

        Log.debug("No Issuer Country Code found in app. Using default Terminal Country Code");

        String countryCode = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.TERMINAL_COUNTRY_CODE.getTagBytes()));
        if(countryCode != null){
            return Util.fromHexString(countryCode);
        }
        
        return new byte[]{0x08, 0x26};
    }
    
    public static int getIntTerminalCountryCode() {
        //For now, just return the version number maintained in the card
    	String value = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.TERMINAL_COUNTRY_CODE.getTagBytes()));
    	if(value == null || value.length() < 4) {
    		return 0;
        }

        return Util.binaryHexCodedDecimalToInt(value);
    }
    
    public static byte[] findCurrencyCode(EMVApplication app){
        if(app != null){
            if(app.getApplicationCurrencyCode() != -1){
                byte[] currencyCode = Util.intToBinaryEncodedDecimalByteArray(app.getApplicationCurrencyCode());
                return Util.resizeArray(currencyCode, 2);
            }
            Locale preferredLocale = null;
            if(app.getLanguagePreference() != null){
                preferredLocale = app.getLanguagePreference().getPreferredLocale();
            }
            if(preferredLocale == null 
                    && app.getCard() != null 
                    && app.getCard().getPSE() != null
                    && app.getCard().getPSE().getLanguagePreference() != null){
                preferredLocale = app.getCard().getPSE().getLanguagePreference().getPreferredLocale();
            }
            if(preferredLocale != null){
                if(preferredLocale.getLanguage().equals(Locale.getDefault().getLanguage())) {
                    //Guesstimate; we presume default locale is the preferred
                    preferredLocale = Locale.getDefault();
                }
                List<Integer> numericCodes = ISO4217_Numeric.getNumericCodeForLocale(preferredLocale);
                if (numericCodes != null && numericCodes.size() > 0) {
                    //Just use the first found. It might not be correct, eg Brazil (BRZ) vs Portugal (EUR)
                    return Util.resizeArray(Util.intToBinaryEncodedDecimalByteArray(numericCodes.get(0)), 2); 
                }
            }
            
        }
        String currencyCode = defaultTerminalProperties.getProperty(Util.byteArrayToHexString(EMVTags.TRANSACTION_CURRENCY_CODE.getTagBytes()));
        if(currencyCode != null){
            return Util.fromHexString(currencyCode);
        }
        return new byte[]{0x08, 0x26};
    }

    public static void main(String[] args) {
        for(String key : defaultTerminalProperties.stringPropertyNames()){
            System.out.println(key + "=" + defaultTerminalProperties.getProperty(key));
            /*if(key.equalsIgnoreCase("9f09"))
            {
            	String value = defaultTerminalProperties.getProperty(key);
            	
            	byte []t = Util.fromHexString(value);
            	
            	int m = Util.byteToInt(t[0], t[1]);
            	System.out.println("AVN=" + m);
            }*/
            
            /*if(key.equalsIgnoreCase("9f33"))
            {
            	String value = defaultTerminalProperties.getProperty(key);
            	byte []t = Util.fromHexString(value);
            	System.out.println("t=" + Util.byte2Hex(t[2]));
            	System.out.println("t1=" + (byte)(t[2]&0x80));
            	if((t[2] & 0x80) == 0x80)
            		System.out.println("SDA supported");
            	else
            		System.out.println("SDA not supported");
            }*/
            
            if(key.equalsIgnoreCase("9f40"))
            {
            	if(isCashbackTrx())
            		System.out.println("Cashback supported");
            	else 
            		System.out.println("Cashback not supported");
            	
            	if(isCashTrx())
            		System.out.println("Cash supported");
            	else 
            		System.out.println("Cash not supported");
            	
            	if(isGoodsTrx())
            		System.out.println("Goods supported");
            	else 
            		System.out.println("Goods not supported");
            	
            	if(isServicesTrx())
            		System.out.println("Services supported");
            	else 
            		System.out.println("Services not supported");
            }
        }
        
        /*{
            TagAndLength tagAndLength = new TagAndLength(EMVTags.AMOUNT_AUTHORISED_NUMERIC, 6);
            DOL dol = new DOL(DOL.Type.PDOL, tagAndLength.getBytes());
            System.out.println(Util.prettyPrintHexNoWrap(constructDOLResponse(dol, null)));
        }
        {
            TagAndLength tagAndLength = new TagAndLength(EMVTags.TERMINAL_COUNTRY_CODE, 2);
            DOL dol = new DOL(DOL.Type.PDOL, tagAndLength.getBytes());
            System.out.println(Util.prettyPrintHexNoWrap(constructDOLResponse(dol, null)));
        }

        {
            //Test country code 076 (Brazil)
            TagAndLength tagAndLength = new TagAndLength(EMVTags.TERMINAL_COUNTRY_CODE, 2);
            DOL dol = new DOL(DOL.Type.PDOL, tagAndLength.getBytes());
            EMVApplication app = new EMVApplication();
            app.setIssuerCountryCode(76); //Brazil
            byte[] dolResponse = constructDOLResponse(dol, app);
            System.out.println(Util.prettyPrintHexNoWrap(dolResponse));
            if (!Arrays.equals(new byte[]{0x00, (byte) 0x76}, dolResponse)) {
                throw new AssertionError("Country code was wrong");
            }
        }

        {
            //Test currency code 986 (Brazilian Real)
            TagAndLength tagAndLength = new TagAndLength(EMVTags.TRANSACTION_CURRENCY_CODE, 2);
            DOL dol = new DOL(DOL.Type.PDOL, tagAndLength.getBytes());
            EMVApplication app = new EMVApplication();
            app.setApplicationCurrencyCode(986);
            byte[] dolResponse = constructDOLResponse(dol, app);
            System.out.println(Util.prettyPrintHexNoWrap(dolResponse));
            if (!Arrays.equals(new byte[]{0x09, (byte) 0x86}, dolResponse)) {
                throw new AssertionError("Currency code was wrong");
            }
        }

        {
            //Test currency code 986 (Brazilian Real) from Locale
            TagAndLength tagAndLength = new TagAndLength(EMVTags.TRANSACTION_CURRENCY_CODE, 2);
            DOL dol = new DOL(DOL.Type.PDOL, tagAndLength.getBytes());
            EMVApplication app = new EMVApplication();
            app.setLanguagePreference(new LanguagePreference(Util.fromHexString("70 74 65 6e 65 73 69 74"))); // (=ptenesit)
            byte[] dolResponse = constructDOLResponse(dol, app);
            System.out.println(Util.prettyPrintHexNoWrap(dolResponse));
//            if(!Arrays.equals(new byte[]{0x09, (byte)0x86}, dolResponse)){
//                throw new AssertionError("Currency code was wrong");
//            }
        }*/
    }
}
