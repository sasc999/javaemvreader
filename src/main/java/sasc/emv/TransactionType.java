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
public class TransactionType {
    
    private byte value;
    
    public TransactionType(byte value){
        this.value = value;
    }
    
    public byte getByte(){
        return value;
    }
    
    public String getDescription(){
        switch(value){
            case 0x00:
                return "Purchase (of Goods or Services) with a type of Payment Card tender";
            case 0x01:
                return "Cash";
            case 0x03:
                return "Check Guarantee with Conversion";
            case 0x04:
                return "Check Verification Only";
            case 0x09:
                return "Purchase with Cash Back";
            case 0x16:
                return "Check Conversion Only";
            case 0x17:
                return "Check Conversion with Cash Back";
            case 0x18:
                return "Check Conversion Only";
            case 0x20:
                return "Purchase Return";
            default:
                return "Unknown";
        }
        
    }

        @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "Transaction Type:");
        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);

        pw.println(indentStr + Util.byte2Hex(value) + " (" + getDescription() + ")");


    }
    
    public static void main(String[] args) {
        System.out.println(new TransactionType((byte)0x01));
    }
}
