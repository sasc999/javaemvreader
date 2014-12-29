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

import sasc.iso7816.TagAndLength;
import sasc.iso7816.Tag;
import sasc.iso7816.SmartCardException;
import sasc.iso7816.BERTLV;
import sasc.iso7816.AID;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import sasc.smartcard.common.SmartCard;
import sasc.iso7816.Application;
import sasc.iso7816.TLVUtil;
import sasc.lookup.IIN_DB;
import sasc.util.ISO3166_1;
import sasc.util.ISO4217_Numeric;
import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class EMVApplication implements Application {

    private boolean isInitializedOnICC = false;
    private boolean allAppRecordsRead = false;
    private boolean isTransactionLogProcessed = false;
    private ApplicationUsageControl auc = null;
    private ApplicationInterchangeProfile aip = null;
    private ApplicationPriorityIndicator api = null;
    private ApplicationFileLocator afl = new ApplicationFileLocator(new byte[]{}); //Default
    private String label = "";
    private String preferredName = "";
    private String issuerUrl = null;
    private AID aid = null;
    private IssuerPublicKeyCertificate issuerCert = null;
    private ICCPublicKeyCertificate iccCert = null;
    private ICCPinEnciphermentPublicKeyCertificate iccPinEnciphermentCert = null;
    private int applicationCurrencyCode = -1;
    private int applicationCurrencyExponent = -1;
    private int issuerCountryCode = -1;
    private String issuerCountryCodeAlpha3 = null;
    private IssuerIdentificationNumber issuerIdentificationNumber = null;
    private int applicationTransactionCounter = -1;
    private int lastOnlineATCRegister = -1;
    private int pinTryCounter = -1;
    private LogEntry logEntry = null;
    private TransactionLog transactionLog = null;
    private DOL pdol = null;
    private DOL ddol = null;
    private Date applicationExpirationDate = null;
    private Date applicationEffectiveDate = null;
    private int applicationVersionNumber = -1;
    private String cardholderName = null;
    private DOL cdol1 = null;
    private DOL cdol2 = null;
    private SignedStaticApplicationData signedStaticAppData = null;
    private SignedDynamicApplicationData signedDynamicAppData = null;
    private PAN pan = null;
    private int panSequenceNumber = -1; //Identifies and differentiates cards with the same PAN
    private CVMList cvmList = null;
    private StaticDataAuthenticationTagList staticDataAuthTagList = null;
    private byte[] track1DiscretionaryData = null; //Optional data encoded by the issuer.
    private byte[] track2DiscretionaryData = null; //Optional data encoded by the issuer.
    private Track2EquivalentData track2EquivalentData = null;
    private ServiceCode serviceCode = null;
    private LanguagePreference languagePreference = null;
    private int issuerCodeTableIndex = -1;
    private int lowerConsecutiveOfflineLimit = -1;
    private int upperConsecutiveOfflineLimit = -1;
    private byte[] issuerActionCodeDefault = null;
    private byte[] issuerActionCodeDenial = null;
    private byte[] issuerActionCodeOnline = null;
    private IBAN iban;
    private BankIdentifierCode bic;
    private byte[] discretionaryData = null;
    
    //Transaction related data elements (TODO move to terminal object)
    private TransactionStatusInformation transactionStatusInformation = new TransactionStatusInformation();
    
    //The terminal shall store all recognised data objects read, whether mandatory or optional, 
    //for later use in the transaction processing. 
    //Data objects that are not recognised by the terminal 
    //(that is, their tags are unknown by the terminal) shall not be stored, 
    //but records containing such data objects may still participate in their 
    //entirety in offline data authentication, depending upon the coding of the AFL.
    private List<BERTLV> unknownRecords = new ArrayList<BERTLV>();
    private List<BERTLV> unprocessedRecords = new ArrayList<BERTLV>();
    private SmartCard card = null;

    public EMVApplication() {
    }

    public void setAID(AID _aid) {
        if (this.aid != null && !Arrays.equals(this.aid.getAIDBytes(), _aid.getAIDBytes())) {
            throw new SmartCardException("Attempting to assign a different AID value. Current: " + Util.prettyPrintHexNoWrap(this.aid.getAIDBytes()) + " new: " + Util.prettyPrintHexNoWrap(_aid.getAIDBytes()));
        }
        this.aid = _aid;
    }

    public void setCard(SmartCard card){
        this.card = card;
    }
    
    @Override
    public SmartCard getCard() {
        return card;
    }
    
    public void addUnknownRecord(BERTLV bertlv) {
        if (aid != null 
                && Arrays.equals(aid.getAIDBytes(), Util.fromHexString("a0 00 00 00 03 00 00 00"))
                && Arrays.equals(bertlv.getTag().getTagBytes(), Util.fromHexString("9f 65"))) {
            //TODO: this is a hack for GP App with tag 9f65 which is very common, but not handled yet
        }
        unknownRecords.add(bertlv);
    }

    public List<BERTLV> getUnknownRecords() {
        return Collections.unmodifiableList(unknownRecords);
    }
    
    public void addUnprocessedRecord(BERTLV bertlv) {
        if (aid != null 
                && Arrays.equals(aid.getAIDBytes(), Util.fromHexString("a0 00 00 00 03 00 00 00"))
                && Arrays.equals(bertlv.getTag().getTagBytes(), Util.fromHexString("9f 65"))) {
            //TODO: this is a hack for GP App with tag 9f65 which is very common, but not handled yet
        }
        unprocessedRecords.add(bertlv);
    }

    public List<BERTLV> getUnprocessedRecords() {
        return Collections.unmodifiableList(unprocessedRecords);
    }

    public ApplicationUsageControl getApplicationUsageControl() {
        return auc;
    }

    public void setApplicationUsageControl(ApplicationUsageControl auc) {
        this.auc = auc;
    }

    public ApplicationPriorityIndicator getApplicationPriorityIndicator() {
        return api;
    }

    public void setApplicationPriorityIndicator(ApplicationPriorityIndicator api) {
        this.api = api;
    }

    public void setApplicationInterchangeProfile(ApplicationInterchangeProfile aip) {
        this.aip = aip;
    }

    public ApplicationInterchangeProfile getApplicationInterchangeProfile() {
        return aip;
    }

    public void setApplicationFileLocator(ApplicationFileLocator afl) {
        this.afl = afl;
    }

    public ApplicationFileLocator getApplicationFileLocator() {
        return afl;
    }

    @Override
    public AID getAID() {
        return aid;
    }

    public void setPAN(PAN pan) {
        this.pan = pan;
    }

    public PAN getPAN() {
        return pan;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setPreferredName(String preferredName) {
        this.preferredName = preferredName;
    }

    public String getPreferredName() {
        return preferredName;
    }
    
    public void setIssuerUrl(String issuerUrl) {
        this.issuerUrl = issuerUrl;
    }

    public String getIssuerUrl() {
        return issuerUrl;
    }
    
    public void setATC(int atc) {
        this.applicationTransactionCounter = atc;
    }
    
    public int getATC() {
        return applicationTransactionCounter;
    }

    public void setApplicationCurrencyCode(int applicationCurrencyCode) {
        this.applicationCurrencyCode = applicationCurrencyCode;
    }

    public int getApplicationCurrencyCode() {
        return applicationCurrencyCode;
    }

    public void setApplicationCurrencyExponent(int applicationCurrencyExponent) {
        this.applicationCurrencyExponent = applicationCurrencyExponent;
    }

    public int getApplicationCurrencyExponent() {
        return applicationCurrencyExponent;
    }

    public void setIssuerCountryCode(int issuerCountryCode) {
        this.issuerCountryCode = issuerCountryCode;
    }

    public int getIssuerCountryCode() {
        return issuerCountryCode;
    }

    public String getIssuerCountryCodeAlpha3() {
        return issuerCountryCodeAlpha3;
    }

    public void setIssuerCountryCodeAlpha3(String issuerCountryCodeAlpha3) {
        this.issuerCountryCodeAlpha3 = issuerCountryCodeAlpha3;
    }

    public IssuerIdentificationNumber getIssuerIdentificationNumber() {
        return issuerIdentificationNumber;
    }

    public void setIssuerIdentificationNumber(IssuerIdentificationNumber issuerIdentificationNumber) {
        this.issuerIdentificationNumber = issuerIdentificationNumber;
    }

    public void setLastOnlineATC(int lastOnlineATCRecord) {
        this.lastOnlineATCRegister = lastOnlineATCRecord;
    }
    
    public int getLastOnlineATC() {
        return lastOnlineATCRegister;
    }

    public void setPINTryCounter(int counter) {
        this.pinTryCounter = counter;
    }

    public int getPINTryCounter() {
        return pinTryCounter;
    }

    public void setLogFormat(LogFormat logFormat) {
        this.transactionLog = new TransactionLog(logFormat);
    }

    public void addTransactionLogRecord(byte[] logRecordBytes) {
        if (transactionLog != null) {
            transactionLog.addRecord(logRecordBytes);
        }
    }
    
    public TransactionLog getTransactionLog() {
        return transactionLog;
    }

    public LogFormat getLogFormat() {
        if (transactionLog != null) {
            return transactionLog.getLogFormat();
        }
        return null;
    }

    public void setLogEntry(LogEntry logEntry) {
        this.logEntry = logEntry;
    }

    public LogEntry getLogEntry() {
        return logEntry;
    }

    public DOL getPDOL() {
        return pdol;
    }

    public void setPDOL(DOL pdol) {
        this.pdol = pdol;
    }

    public DOL getDDOL() {
        return ddol;
    }

    public void setDDOL(DOL ddol) {
        if (ddol == null) {
            throw new IllegalArgumentException("DDOL cannot be null");
        }
        boolean unpredictableNumberFound = false;
        for (TagAndLength tal : ddol.getTagAndLengthList()) {
            if (tal.getTag().equals(EMVTags.UNPREDICTABLE_NUMBER)) {
                unpredictableNumberFound = true;
            }
        }
        if (!unpredictableNumberFound) {
            throw new IllegalArgumentException("DDOL must contain the Unpredictable Number (tag 0x9F37)");
        }
        this.ddol = ddol;
    }

    public void setCardholderName(String name) {
        this.cardholderName = name;
    }

    public String getCardholderName() {
        return cardholderName;
    }
    
    public IBAN getIBAN() {
        return iban;
    }
    
    public void setIBAN(IBAN iban) {
        this.iban = iban;
    }
    
    public BankIdentifierCode getBIC() {
        return bic;
    }
    
    public void setBIC(BankIdentifierCode bic){
        this.bic = bic;
    }
    
    public byte[] getDiscretionaryData() {
        return Util.copyByteArray(discretionaryData);
    }
    
    public void setDiscretionaryData(byte[] discretionaryData) {
        this.discretionaryData = Util.copyByteArray(discretionaryData);
    }

    public IssuerPublicKeyCertificate getIssuerPublicKeyCertificate() {
        return issuerCert;
    }

    public void setIssuerPublicKeyCertificate(IssuerPublicKeyCertificate issuerCert) {
        this.issuerCert = issuerCert;
    }

    public ICCPublicKeyCertificate getICCPublicKeyCertificate() {
        return iccCert;
    }

    public void setICCPublicKeyCertificate(ICCPublicKeyCertificate iccCert) {
        this.iccCert = iccCert;
    }
    
    public ICCPinEnciphermentPublicKeyCertificate getICCPinEnciphermentPublicKeyCertificate() {
        return iccPinEnciphermentCert;
    }

    public void setICCPinEnciphermentPublicKeyCertificate(ICCPinEnciphermentPublicKeyCertificate iccPinEnciphermentCert) {
        this.iccPinEnciphermentCert = iccPinEnciphermentCert;
    }

    public SignedStaticApplicationData getSignedStaticApplicationData() {
        return signedStaticAppData;
    }

    public void setSignedStaticApplicationData(SignedStaticApplicationData signedStaticAppData) {
        this.signedStaticAppData = signedStaticAppData;
    }

    public SignedDynamicApplicationData getSignedDynamicApplicationData() {
        return signedDynamicAppData;
    }

    public void setSignedDynamicApplicationData(SignedDynamicApplicationData signedDynamicAppData) {
        this.signedDynamicAppData = signedDynamicAppData;
    }

    public void setCDOL1(DOL cdol1) {
        this.cdol1 = cdol1;
    }

    public void setCDOL2(DOL cdol2) {
        this.cdol2 = cdol2;
    }

    public void setExpirationDate(byte[] dateBytes) {
        if (dateBytes.length != 3) {
            throw new SmartCardException("Byte array length must be 3. Length=" + dateBytes.length);
        }
        int YY = Util.binaryCodedDecimalToInt(dateBytes[0]);
        int MM = Util.binaryCodedDecimalToInt(dateBytes[1]);
        int DD = Util.binaryCodedDecimalToInt(dateBytes[2]);
        Calendar cal = Calendar.getInstance();
        cal.set(2000 + YY, MM - 1, DD, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        this.applicationExpirationDate = cal.getTime();
    }

    public Date getExpirationDate() {
        if (applicationExpirationDate==null) {
            return null;
        }
        return (Date) applicationExpirationDate.clone();
    }

    public void setEffectiveDate(byte[] dateBytes) {
        if (dateBytes.length != 3) {
            throw new SmartCardException("Byte array length must be 3. Length=" + dateBytes.length);
        }
        int YY = Util.binaryCodedDecimalToInt(dateBytes[0]);
        int MM = Util.binaryCodedDecimalToInt(dateBytes[1]);
        int DD = Util.binaryCodedDecimalToInt(dateBytes[2]);
        Calendar cal = Calendar.getInstance();
        cal.set(2000 + YY, MM - 1, DD, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        this.applicationEffectiveDate = cal.getTime();
    }

    public Date getEffectiveDate() {
        if(applicationEffectiveDate == null) {
            return null;
        }
        return (Date) applicationEffectiveDate.clone();
    }

    public void setApplicationVersionNumber(int version) {
        this.applicationVersionNumber = version;
    }

    public int getApplicationVersionNumber() {
        return applicationVersionNumber;
    }

    void setTrack1DiscretionaryData(byte[] valueBytes) {
        this.track1DiscretionaryData = valueBytes;
    }

    public byte[] getTrack1DiscretionaryData() {
        return Util.copyByteArray(track1DiscretionaryData);
    }

    void setTrack2DiscretionaryData(byte[] valueBytes) {
        this.track2DiscretionaryData = valueBytes;
    }

    public byte[] getTrack2DiscretionaryData() {
        return Util.copyByteArray(track2DiscretionaryData);
    }

    void setTrack2EquivalentData(Track2EquivalentData track2EquivalentData) {
        this.track2EquivalentData = track2EquivalentData;
    }

    public Track2EquivalentData getTrack2EquivalentData() {
        return track2EquivalentData;
    }

    void setServiceCode(int serviceCode) {
        this.serviceCode = new ServiceCode(serviceCode);
    }

    public ServiceCode getServiceCode() {
        return serviceCode;
    }

    void setCVMList(CVMList cvmList) {
        this.cvmList = cvmList;
    }

    public CVMList getCVMList() {
        return cvmList;
    }

    void setStaticDataAuthenticationTagList(StaticDataAuthenticationTagList staticDataAuthTagList) {
        this.staticDataAuthTagList = staticDataAuthTagList;
    }

    public StaticDataAuthenticationTagList getStaticDataAuthenticationTagList() {
        return staticDataAuthTagList;
    }

    void setPANSequenceNumber(byte value) {
        this.panSequenceNumber = Util.binaryCodedDecimalToInt(value);
    }

    public int getPANSequenceNumber() {
        return this.panSequenceNumber;
    }

    void setLanguagePreference(LanguagePreference languagePreference) {
        if (this.languagePreference != null) {
            throw new RuntimeException("JavaEMVReader currently does not support multiple LanguagePreference. Must create method 'ADD LanguagePreference', not SET");
        }
        this.languagePreference = languagePreference;
    }

    public LanguagePreference getLanguagePreference() {
        return this.languagePreference;
    }

    public void setIssuerCodeTableIndex(int index) {
        issuerCodeTableIndex = index;
    }

    public int getIssuerCodeTableIndex() {
        return issuerCodeTableIndex;
    }

    public String getIssuerCodeTable() {
        return "ISO-8859-" + issuerCodeTableIndex;
    }

    public void setLowerConsecutiveOfflineLimit(int limit) {
        this.lowerConsecutiveOfflineLimit = limit;
    }

    public int getLowerConsecutiveOfflineLimit() {
        return lowerConsecutiveOfflineLimit;
    }

    public void setUpperConsecutiveOfflineLimit(int limit) {
        this.upperConsecutiveOfflineLimit = limit;
    }

    public int getUpperConsecutiveOfflineLimit() {
        return upperConsecutiveOfflineLimit;
    }

    public void setIssuerActionCodeDefault(byte[] data) {
        issuerActionCodeDefault = data;
    }

    public void setIssuerActionCodeDenial(byte[] data) {
        issuerActionCodeDenial = data;
    }

    public void setIssuerActionCodeOnline(byte[] data) {
        issuerActionCodeOnline = data;
    }

    public TransactionStatusInformation getTransactionStatusInformation() {
        return transactionStatusInformation;
    }

    public byte[] getOfflineDataAuthenticationRecords() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (ApplicationElementaryFile aef : this.getApplicationFileLocator().getApplicationElementaryFiles()) {

            //Only those records identified in the AFL as participating in offline data authentication are to be processed.
            if (aef.getNumRecordsInvolvedInOfflineDataAuthentication() == 0) {
                continue;
            }

            for (Record record : aef.getRecords()) {

                if (!record.isInvolvedInOfflineDataAuthentication()) {
                    continue;
                }

                byte[] fileRawData = record.getRawData();
                if (fileRawData == null || fileRawData.length < 2) {
                    //The records read for offline data authentication shall be TLV-coded with tag equal to '70'
                    throw new SignedDataException("File Raw Data was null or invalid length (less than 2): " + fileRawData == null ? "null" : String.valueOf(fileRawData.length));
                }
                //The records read for offline data authentication shall be TLV-coded with tag equal to '70'
                if (fileRawData[0] != (byte) 0x70) {
                    //If the records read for offline data authentication are not TLV-coded with tag equal to '70'
                    //then offline data authentication shall be considered to have been performed and to have failed;
                    //that is, the terminal shall set the 'Offline data authentication was performed' bit in the TSI to 1,
                    //and shall set the appropriate 'SDA failed' or 'DDA failed' or 'CDA failed' bit in the TVR.
                    //TODO
                }

                //The data from each record to be included in the offline data authentication input
                //depends upon the SFI of the file from which the record was read.
                int sfi = aef.getSFI().getValue();
                if (sfi >= 1 && sfi <= 10) {
                    //For files with SFI in the range 1 to 10, the record tag ('70') and the record length
                    //are excluded from the offline data authentication process. All other data in the
                    //data field of the response to the READ RECORD command (excluding SW1 SW2) is included.

                    //Get the 'valueBytes'
                    BERTLV tlv = TLVUtil.getNextTLV(new ByteArrayInputStream(fileRawData));
                    stream.write(tlv.getValueBytes(), 0, tlv.getValueBytes().length);
                } else {
                    //For files with SFI in the range 11 to 30, the record tag ('70') and the record length
                    //are not excluded from the offline data authentication process. Thus all data in the
                    //data field of the response to the READ RECORD command (excluding SW1 SW2) is included
                    stream.write(fileRawData, 0, fileRawData.length);
                }
            }


        }

        //After all records identified by the AFL have been processed, the Static Data Authentication Tag List is processed,
        //if it exists. If the Static Data Authentication Tag List exists, it shall contain only the tag for the
        //Application Interchange Profile. The tag must represent the AIP available in the current application.
        //The value field of the AIP is to be concatenated to the current end of the input string.
        //The tag and length of the AIP are not included in the concatenation.
        StaticDataAuthenticationTagList sdaTagListObject = this.getStaticDataAuthenticationTagList();
        if (sdaTagListObject != null) {
            List<Tag> sdaTagList = sdaTagListObject.getTagList();
            if (sdaTagList != null && !sdaTagList.isEmpty()) {
                if (sdaTagList.size() > 1 || sdaTagList.get(0) != EMVTags.APPLICATION_INTERCHANGE_PROFILE) {
                    throw new SmartCardException("SDA Tag list must contain only the 'Application Interchange Profile' tag: " + sdaTagList);
                } else {
                    byte[] aipBytes = this.getApplicationInterchangeProfile().getBytes();
                    stream.write(aipBytes, 0, aipBytes.length);
                }
            }
        }

        return stream.toByteArray();
    }

    //The initializedOnICC methods are only used to indicate that the
    //GET PROCESSING OPTS has been performed
    public void setInitializedOnICC() {
        isInitializedOnICC = true;
    }

    public boolean isInitializedOnICC() {
        return isInitializedOnICC;
    }

    //The allAppRecordsRead methods are only used to indicate that
    //all the records indicated in the AFL have been read
    public void setAllAppRecordsInAFLRead() {
        allAppRecordsRead = true;
    }

    public boolean isAllAppRecordsInAFLRead() {
        return allAppRecordsRead;
    }
    
    public void setTransactionLogProcessed() {
        isTransactionLogProcessed = true;
    }
    
    public boolean isTransactionLogProcessed() {
        return isTransactionLogProcessed;
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    @Override
    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "EMV Application");

        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);

        if (aid != null) {
            aid.dump(pw, indent + Log.INDENT_SIZE);
        }
        pw.println(indentStr + "Label: " + getLabel());
        pw.println(indentStr + "Preferred Name: " + getPreferredName());
        if (issuerUrl != null){
            pw.println(indentStr + "Issuer URL: " + issuerUrl);
        }
        if (applicationEffectiveDate != null) {
            pw.println(indentStr + "Application Effective Date: " + applicationEffectiveDate);
        }
        if (applicationExpirationDate != null) {
            pw.println(indentStr + "Application Expiration Date: " + applicationExpirationDate);
        }
        if (applicationVersionNumber != -1) {
            pw.println(indentStr + "Application Version Number: " + applicationVersionNumber);
        }
        if (applicationCurrencyCode != -1) {
            String description = "";
            ISO4217_Numeric.Currency currency = ISO4217_Numeric.getCurrencyForCode(applicationCurrencyCode);
            if (currency != null) {
                description = " (" + currency.getCode() + " " + currency.getDisplayName() + ")";
            }
            pw.println(indentStr + "Application Currency Code (ISO 4217): " + applicationCurrencyCode + description);
        }
        if (applicationCurrencyExponent != -1) {
            pw.println(indentStr + "Application Currency Exponent: " + applicationCurrencyExponent + " (Position of the decimal point from the right)");
        }
        if (issuerCountryCode != -1) {
            String description = "";
            String countryStr = ISO3166_1.getCountryForCode(issuerCountryCode);
            if (countryStr != null && countryStr.trim().length() > 0) {
                description = " (" + countryStr + ")";
            }
            pw.println(indentStr + "Issuer Country Code (ISO 3166-1): " + issuerCountryCode + description);
        }
        if (issuerCountryCodeAlpha3 != null && issuerCountryCodeAlpha3.trim().length() > 0) {
            pw.println(indentStr + "Issuer Country Code (Alpha 3) : " + issuerCountryCodeAlpha3.trim());
        }
        if (issuerIdentificationNumber != null) {
            String description = "";
            IIN_DB.IIN iin = IIN_DB.searchIIN(issuerIdentificationNumber.getValue());
            if (iin != null && iin.getDescription().trim().length() > 0) {
                description = " (" + iin.getDescription().trim() + ")";
            }
            pw.println(indentStr + "Issuer Identification Number : " + issuerIdentificationNumber + description);
        }
        if (discretionaryData != null) {
            pw.println(indentStr + "Discretionary Data: " + Util.byteArrayToHexString(discretionaryData) + " (ASCII: " + Util.getSafePrintChars(discretionaryData) + ")");
        }
        if (lowerConsecutiveOfflineLimit != -1) {
            pw.println(indentStr + "Lower Consecutive Offline Limit: " + lowerConsecutiveOfflineLimit);
        }
        if (upperConsecutiveOfflineLimit != -1) {
            pw.println(indentStr + "Upper Consecutive Offline Limit: " + upperConsecutiveOfflineLimit);
        }
        if (applicationTransactionCounter != -1) {
            pw.println(indentStr + "Application Transaction Counter (ATC): " + applicationTransactionCounter);
        }
        if (lastOnlineATCRegister != -1) {
            pw.println(indentStr + "Last Online ATC Register: " + lastOnlineATCRegister);
        }
        if (pinTryCounter >= 0) {
            pw.println(indentStr + "PIN Try Counter: " + pinTryCounter + " (Number of PIN tries remaining)");
        }
        if (cardholderName != null) {
            pw.println(indentStr + "Cardholder Name: " + cardholderName);
        }
        if (pan != null) {
            pan.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (panSequenceNumber != -1) {
            pw.println(indentStr + "PAN Sequence Number: " + panSequenceNumber);
        }
        if (api != null) {
            api.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (aip != null) {
            aip.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (afl != null) {
            afl.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (auc != null) {
            auc.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (transactionLog != null) {
            transactionLog.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (pdol != null) {
            pdol.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (ddol != null) {
            ddol.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (issuerCert != null) {
            issuerCert.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (iccCert != null) {
            iccCert.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (iccPinEnciphermentCert != null) {
            iccPinEnciphermentCert.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (cdol1 != null) {
            cdol1.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (cdol2 != null) {
            cdol2.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (signedStaticAppData != null) {
            signedStaticAppData.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (signedDynamicAppData != null) {
            signedDynamicAppData.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (cvmList != null) {
            cvmList.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (staticDataAuthTagList != null) {
            staticDataAuthTagList.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (track1DiscretionaryData != null) {
            pw.println(indentStr + "Track 1 Discretionary Data:");
            pw.println(indentStr + "   " + Util.byteArrayToHexString(track1DiscretionaryData) + " (ASCII: " + Util.getSafePrintChars(track1DiscretionaryData) + ")");
        }
        if (track2DiscretionaryData != null) {
            pw.println(indentStr + "Track 2 Discretionary Data:");
            pw.println(indentStr + "   " + Util.byteArrayToHexString(track2DiscretionaryData) + " (ASCII: " + Util.getSafePrintChars(track2DiscretionaryData) + ")");
        }
        if (track2EquivalentData != null) {
            track2EquivalentData.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (iban != null) {
            iban.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (bic != null) {
            bic.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (serviceCode != null) {
            serviceCode.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (languagePreference != null) {
            languagePreference.dump(pw, indent + Log.INDENT_SIZE);
        }
        if (issuerCodeTableIndex != -1) {
            pw.println(indentStr + "Issuer Code Table Index: " + issuerCodeTableIndex + " (ISO-8859-" + issuerCodeTableIndex + ")");
        }
        if (issuerActionCodeDefault != null) {
            pw.println(indentStr + "Issuer Action Code - Default:");
            for (byte b : issuerActionCodeDefault) {
                pw.println(indentStr + "   " + Util.byte2BinaryLiteral(b));
            }
        }
        if (issuerActionCodeDenial != null) {
            pw.println(indentStr + "Issuer Action Code - Denial:");
            for (byte b : issuerActionCodeDenial) {
                pw.println(indentStr + "   " + Util.byte2BinaryLiteral(b));
            }
        }
        if (issuerActionCodeOnline != null) {
            pw.println(indentStr + "Issuer Action Code - Online:");
            for (byte b : issuerActionCodeOnline) {
                pw.println(indentStr + "   " + Util.byte2BinaryLiteral(b));
            }
        }
        if (!unprocessedRecords.isEmpty()) {
            pw.println(indentStr + "Other records (" + unprocessedRecords.size() + " found):");

            for (BERTLV tlv : unprocessedRecords) {
                pw.println(Util.getSpaces(indent + Log.INDENT_SIZE * 2) + tlv.getTag() + " " + tlv);
            }
        }
        if (!unknownRecords.isEmpty()) {
            pw.println(indentStr + "UNKNOWN APPLICATION RECORDS (" + unknownRecords.size() + " found):");
        
            for (BERTLV tlv : unknownRecords) {
                pw.println(Util.getSpaces(indent + Log.INDENT_SIZE * 2) + tlv.getTag() + " " + tlv);
            }
        }
        pw.println("");
    }
}
