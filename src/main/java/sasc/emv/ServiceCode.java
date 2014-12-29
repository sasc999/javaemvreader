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
 *
 * @author sasc
 */
public class ServiceCode {

    private char[] serviceCode;

    public ServiceCode(char[] serviceCode) {
        if (serviceCode.length != 3) {
            throw new IllegalArgumentException("ServiceCode must have 3 digits");
        }
        for (int i = 0; i < serviceCode.length; i++) {
            if (!Character.isDigit(serviceCode[i])) {
                throw new IllegalArgumentException("Only digits allowed in ServiceCode: " + serviceCode[i]);
            }
        }
        this.serviceCode = serviceCode;
    }
    
    public ServiceCode(int serviceCode){
        this(String.valueOf(serviceCode).toCharArray());
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "Service Code - " + new String(serviceCode) + ":");
        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);
        pw.println(indentStr + serviceCode[0] + " : Interchange Rule - " + getInterchangeRulesDescription());
        pw.println(indentStr + serviceCode[1] + " : Authorization Processing - " + getAuthorisationProcessingDescription());
        pw.println(indentStr + serviceCode[2] + " : Range of Services - " + getRangeOfServicesDescription());
    }

    //Service code values common in financial cards:
    //The first digit specifies the interchange rules, the second specifies authorisation processing and the third specifies the range of services
    public String getInterchangeRulesDescription() {
        switch (serviceCode[0]) {
            case '1':
                return "International interchange OK";
            case '2':
                return "International interchange, use IC (chip) where feasible";
            case '5':
                return "National interchange only except under bilateral agreement";
            case '6':
                return "National interchange only except under bilateral agreement, use IC (chip) where feasible";
            case '7':
                return "No interchange except under bilateral agreement (closed loop)";
            case '9':
                return "Test";
            default:
                return "RFU";
        }
    }

    public String getAuthorisationProcessingDescription() {
        switch (serviceCode[1]) {
            case '0':
                return "Normal";
            case '2':
                return "Contact issuer via online means";
            case '4':
                return "Contact issuer via online means except under bilateral agreement";
            default:
                return "RFU";
        }
    }

    public String getRangeOfServicesDescription() {
        switch (serviceCode[2]) {
            case '0':
                return "No restrictions, PIN required";
            case '1':
                return "No restrictions";
            case '2':
                return "Goods and services only (no cash)";
            case '3':
                return "ATM only, PIN required";
            case '4':
                return "Cash only";
            case '5':
                return "Goods and services only (no cash), PIN required";
            case '6':
                return "No restrictions, use PIN where feasible";
            case '7':
                return "Goods and services only (no cash), use PIN where feasible";
            default:
                return "RFU";
        }
    }
}