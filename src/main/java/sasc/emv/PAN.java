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

import sasc.iso7816.SmartCardException;
import java.io.PrintWriter;
import java.io.StringWriter;
import sasc.lookup.IIN_DB;
import sasc.util.ISO3166_1;
import sasc.util.Log;
import sasc.util.Util;

/**
 * See http://en.wikipedia.org/wiki/ISO/IEC_7812
 * 
 * @author sasc
 */
public class PAN {

    private short mii;
    
    //Issuer Identification Number, aka BIN
    //BIN - Bank Identification Number, commonly (and wrongly) referred to as "Bin Number". 
    //This is the first six digits of the number on many types of card including 
    //credit cards and debit cards. As other issuers besides banks participate in card schemes, 
    //the term IIN (Issuer Identification Number) is replacing BIN.
    private IssuerIdentificationNumber iin;
    private long accountNumber;
    private String panStr;
    private boolean valid;

    public PAN(byte[] panBytes) {
        this(Util.byteArrayToHexString(panBytes));
    }

    public PAN(String inputString) {
        if (inputString.length() < 8) {
            throw new SmartCardException("Invalid PAN length: " + inputString.length());
        }
        this.panStr = inputString.toUpperCase();
        mii = Short.parseShort(panStr.substring(0, 1));
        iin = new IssuerIdentificationNumber(Util.fromHexString(panStr.substring(0, 6)));
        //The PAN is transferred as 'cn' (compressed numeric):
        //Compressed numeric data elements consist of two numeric digits
        //(having values in the range Hex '0'–'9') per byte.
        //These data elements are left justified and padded with
        //trailing hexadecimal 'F's.
        int trailingPadIndex = panStr.indexOf('F');
        if(trailingPadIndex != -1){
            panStr = panStr.substring(0, trailingPadIndex);
        }
        //Check that actual PAN length (without padding, is not longer than 19 digits)
        if (panStr.length() > 19) {
            throw new SmartCardException("Invalid PAN length: " + panStr.length());
        }

        accountNumber = Long.parseLong(panStr.substring(6, panStr.length() - 1));
        valid = PAN.isValidPAN(panStr);

    }

    public short getMajorIndustryIdentifier() {
        return mii;
    }

    public IssuerIdentificationNumber getIssuerIdentifierNumber() {
        return iin;
    }

    public long getAccountNumber() {
        return accountNumber;
    }

    public String getPanAsString() {
        return panStr;
    }

    public char getCheckDigit() {
        return panStr.charAt(panStr.length() - 1);
    }

    public boolean isValid(){
        return valid;
    }

    public static boolean isValidPAN(String number) {
        //Perform LUHN check
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(number.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "Primary Account Number (PAN) - " + panStr);
        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);
        switch (getMajorIndustryIdentifier()) {
            case 0:
                pw.println(indentStr + "Major Industry Identifier = 0 (ISO/TC 68 and other industry assignments)");
                break;
            case 1:
                pw.println(indentStr + "Major Industry Identifier = 1 (Airlines)");
                break;
            case 2:
                pw.println(indentStr + "Major Industry Identifier = 2 (Airlines and other future industry assignments)");
                break;
            case 3:
                pw.println(indentStr + "Major Industry Identifier = 3 (Travel and entertainment and banking/financial)");
                break;
            case 4:
                pw.println(indentStr + "Major Industry Identifier = 4 (Banking and financial)");
                break;
            case 5:
                pw.println(indentStr + "Major Industry Identifier = 5 (Banking and financial)");
                break;
            case 6:
                pw.println(indentStr + "Major Industry Identifier = 6 (Merchandising and banking/financial)");
                break;
            case 7:
                pw.println(indentStr + "Major Industry Identifier = 7 (Petroleum and other future industry assignments)");
                break;
            case 8:
                pw.println(indentStr + "Major Industry Identifier = 8 (Healthcare, telecommunications and other future industry assignments)");
                break;
            case 9:
                pw.println(indentStr + "Major Industry Identifier = 9 (For assignment by national standards bodies)");
                pw.println(indentStr + "Country Code (ISO 3166-1): " + panStr.substring(1, 4) + " (=" + ISO3166_1.getCountryForCode(panStr.substring(1, 4)) + ")");
                break;
        }
        int iinInt = iin.getValue();
        IIN_DB.IIN iinLookup = IIN_DB.searchIIN(iinInt);
        String iinDescription = "";
        if(iinLookup != null){
            iinDescription = " ("+iinLookup.getDescription()+")";
        }
        pw.println(indentStr + "Issuer Identifier Number: " + iinInt + iinDescription);
        pw.println(indentStr + "Account Number: " + accountNumber);
        pw.println(indentStr + "Check Digit: " + getCheckDigit() + " ("+(isValid()?"Valid":"Invalid")+")");
    }

    public static void main(String[] args) {
        IIN_DB.initialize();
        PAN pan = new PAN("5411118888888882");
        System.out.println(pan);
        System.out.println(new PAN("6737200111111111112f"));
    }
}
