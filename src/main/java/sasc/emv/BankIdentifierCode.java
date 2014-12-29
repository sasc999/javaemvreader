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
import sasc.iso7816.SmartCardException;
import sasc.util.Util;

/**
 * Bank Identifier Code (BIC) aka SWIFT-BIC, BIC code, SWIFT ID or SWIFT code
 * 
 * Uniquely identifies a bank as defined in ISO 9362.
 * http://en.wikipedia.org/wiki/ISO_9362
 * 
 * BIC - Bank Identification Code. 
 * A standard 8- or 11-digit string used under the auspices of the International 
 * Organization for Standardization and administered by SWIFT. 
 * It uniquely identifies either a bank (8 digits) or a branch (11 digits). 
 * The first four digits are the bank code, the next two the ISSN country identifier number, 
 * and the next three the location identifier. The main office of the bank has 8 digits, 
 * or is followed by XXX: where there is an 11 digit code and the last three are not XXX, 
 * this identifies the particular branch. BICs are often referred to as "SWIFT Codes".
 *
 * A SWIFT code is a unique identification code assigned to a particular bank or branch, in a standard format knows as the
 * "Bank Identifier Code" [BIC code]. 
 * 
 * These codes are used when transferring money, and messages between banks.
 * 
 * The SWIFT code consists of 8 or 11 characters. 
 * 
 * When 8-digits code is given, it refers to the primary office.
 * When 11-digit code is given, it refers to a branch of that office.
 * 
 * Here is a sample Entry with a break down:
 * State City Bank Branch BIC / SWIFT Code New York New York Brown Brothers Harriman & Company Brown Brothers Harriman & Company :BBHCUS3IAQR
 *  
 *       BBHC         |         US          |        3I          |         AQR
 *     Bank Code             Country              Location               Branch
 * 
 * First 4 characters - bank code (only letters)
 * Next  2 characters - ISO 3166-1 alpha-2 country code (only letters)
 * Next  2 characters - location code (letters and digits) 
 *                      [passive participant have "1" as the second character]
 * Last  3 characters - branch code, optional 
 *                      ['XXX' for primary office] [letters and digits] 
 *
 * @author sasc
 */
public class BankIdentifierCode {

    private byte[] bic;
    private String bankCode;
    private String countryCode;
    private String locationCode;
    private String branchCode;
    
    public BankIdentifierCode(byte[] value) {
        if(value.length != 8 && value.length != 11) {
            throw new SmartCardException("Invalid BIC length: " + value.length);
        }
        this.bic = value;
        this.bankCode = Util.getSafePrintChars(bic, 0, 4);
        this.countryCode = Util.getSafePrintChars(bic, 4, 2);
        this.locationCode = Util.getSafePrintChars(bic, 6, 2);
        
        if(value.length == 11){
            this.branchCode = Util.getSafePrintChars(bic, 8, 3);
        }
    }
    
    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "Bank Identifier Code (BIC/Swift Code) - " + Util.getSafePrintChars(bic));
        pw.println(Util.getSpaces(indent + 3) + "Bank Code - " + bankCode);
        pw.println(Util.getSpaces(indent + 3) + "Country Code - " + countryCode);
        pw.println(Util.getSpaces(indent + 3) + "Location Code - " + locationCode + (locationCode.charAt(1) == '1'?" (Passive Participant)":" "));
        if(branchCode != null) {
            pw.println(Util.getSpaces(indent + 3) + "Branch Code - " + branchCode + ("XXX".equals(branchCode)?" (Primary Office)":" "));
        } else {
            pw.println(Util.getSpaces(indent + 3) + "Primary Office");
        }
    }
    
    public static void main(String[] args) throws Exception {
        System.out.println(new BankIdentifierCode("BBHCUS3IAQR".getBytes("US-ASCII")));
        System.out.println(new BankIdentifierCode("BBHCUS3IXXX".getBytes("US-ASCII")));
        System.out.println(new BankIdentifierCode("BBHCUS41".getBytes("US-ASCII")));
    }
}
