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
 * International Bank Account Number (IBAN).
 *
 * Uniquely identifies the account of a customer at a financial institution as
 * defined in ISO 13616
 *
 * Length 0-34 bytes
 *
 * IBAN - International Bank Account Number. Often (incorrectly) referred to as
 * IBAN Number. An unique account identifier for every bank account in Europe,
 * supervised by SWIFT. The IBAN system was originally intended for use within
 * Europe, and its use is largely confined thereto: most notably, the USA does
 * not participate. Among British dependencies, only Gibraltar assigns IBANs.
 * All of Europe does however participate with the exception of the CIS
 * countries: Turkey uses IBANS and so do some African countries. The number
 * consists first of a two-digit country identifier, followed by two check
 * digits, then up to 30 characters representing the local account. The number
 * of characters varies from country to country, but is fixed for each country.
 * When used electronically there are no spaces in IBANs, but the convention on
 * paper is to separate into groups of four with any odd digits in the last
 * group.
 *
 * @author sasc
 */
public class IBAN {

    public String iban;
    public String accountNumber;
    public String bankNumber;
    public String countryCode;

    public IBAN(byte[] iban) {
        if(iban.length > 34) {
            throw new SmartCardException("Invalid IBAN length: " + iban.length);
        }
        this.iban = Util.getSafePrintChars(iban);
        countryCode = this.iban.substring(0, 2);
        if ("de".equalsIgnoreCase(countryCode)) {
            bankNumber = this.iban.substring(2, 10);
            accountNumber = this.iban.substring(10);
        }
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "International Bank Account Number (IBAN) - " + iban);
        pw.println(Util.getSpaces(indent + 3) + "Country - " + countryCode);
        if (bankNumber != null) {
            pw.println(Util.getSpaces(indent + 3) + "Bank - " + bankNumber);
            pw.println(Util.getSpaces(indent + 3) + "Account - " + accountNumber);
        }
    }
}
