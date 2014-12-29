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
public class CVMResults {

    private byte firstByte;
    private byte secondByte;
    private byte thirdByte;

    public CVMResults() {
        this((byte) 0x00, (byte) 0x00, (byte) 0x00);  //Initialize with all bits unset
    }

    public CVMResults(byte firstByte, byte secondByte, byte thirdByte) {
        this.firstByte = firstByte;
        this.secondByte = secondByte;
        this.thirdByte = thirdByte;
    }

    public void setCVMResults(byte first, byte second, byte third) {
        this.firstByte = first;
        this.secondByte = second;
        this.thirdByte = third;
    }

    public String getCVMResults() {
        if (thirdByte == 0x00) {
            return "CVM Result: Unknown";
        } else if (thirdByte == 0x01) {
            return "CVM Result: Fail/No CVM Condition Code was satisfied/CVM Code was not recognized or not supported";
        } else {
            return "CVM Result: Success";
        }
    }

    public String getCVMValue() {
        return "CVM Value: " + Util.prettyPrintHex(getBytes());
    }

    public void reset() {
        firstByte = 0x00;
        secondByte = 0x00;
        thirdByte = 0x00;
    }

    public byte[] getBytes() {
        return new byte[]{firstByte, secondByte, thirdByte};
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "CVM Results:");
        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);

        pw.println(indentStr + getCVMValue());
        pw.println(indentStr + getCVMResults());
    }

    public static void main(String[] args) {
        CVMResults cvmr;
        cvmr = new CVMResults((byte) 0x68, (byte) 0x00, (byte) 0x00);
        System.out.println(cvmr.toString());
        /*tsi = new CVMResults((byte)0xE8, (byte)0x00);
         System.out.println(tsi.toString()); //VISA Comfort Hotel 
         tsi = new CVMResults((byte)0xF8, (byte)0x00);
         System.out.println(tsi.toString()); //VISA Inside premium hotel (DE)*/
    }
}
