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
import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import sasc.util.Log;
import sasc.util.Util;

/**
 * Cardholder Verification Method (CVM) List
 * The CVM list specifies acceptable types of cardholder verification
 * EMV book 3 section 10.5 (page 119)
 *
 * @author sasc
 */
public class CVMList {
    private LinkedList<CVRule> cvRules = new LinkedList<CVRule>();
    private byte[] amountField;
    private byte[] secondAmountField;

    public CVMList(byte[] data){

        if(data.length < 8 ){
            throw new IllegalArgumentException("Length of CVM list is less than 8. Length=" + data.length);
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        amountField = new byte[4];
        secondAmountField = new byte[4];
        bis.read(amountField, 0, amountField.length);
        bis.read(secondAmountField, 0, secondAmountField.length);
        if(bis.available() % 2 != 0 ){
            throw new SmartCardException("CMVRules data is not a multiple of 2. Length=" + data.length);
        }
        while(bis.available() > 0){
            byte[] tmp = new byte[2];
            bis.read(tmp, 0, tmp.length);
            cvRules.add(new CVRule(tmp[0], tmp[1], amountField, secondAmountField));
        }
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }
    
    public List<CVRule> getRules() {
        return Collections.unmodifiableList(cvRules);
    }

    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "Cardholder Verification Method (CVM) List:");

        for(CVRule cvRule : cvRules){
            cvRule.dump(pw, indent + Log.INDENT_SIZE);
        }
    }

    public static void main(String[] args){
        System.out.println(new CVMList(Util.fromHexString("00 00 00 00 00 00 00 00 1e 03 1f 00 5d 00 ")).toString());
        //System.out.println(new CVMList(Util.fromHexString("00 00 00 00 00 00 00 00 44 03 41 03 42 03 5e 03 1f 00")).toString());
    }
}
