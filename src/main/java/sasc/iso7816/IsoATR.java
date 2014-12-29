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
package sasc.iso7816;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class IsoATR {
    private byte[] atrBytes;
    private Protocol protocol = Protocol.T_0;
    private Convention convention;
    private int numHistoricalBytes = 0;


    //TODO
    private byte TB1;
    private byte TC1;
    private byte TD1;
    private byte TD2;
    private byte TA3;
    private byte TB3;
    private byte TCK;

//TS
//T0
//TB1
//TC1
//TD1
//TD2
//TA3
//TB3
//TCK

    public static class ParseException extends Exception{
        public ParseException(String msg){
            super(msg);
        }
        public ParseException(String msg, Throwable cause){
            super(msg, cause);
        }
    }

    public static enum Protocol{
        T_0, T_1;

        @Override
        public String toString(){
            switch(this){
                case T_0:
                    return "T=0";
                case T_1:
                    return "T=1";
            }
            return "";
        }
    }

    public static enum Convention{
        INVERSE, DIRECT
    }

    static IsoATR parse(byte[] atrBytes) throws IsoATR.ParseException{
        return new IsoATR(atrBytes);
    }

    private IsoATR(byte[] atrBytes) throws IsoATR.ParseException{

        //TODO throw ParseException if not ISO compliant

        try{
        
            this.atrBytes = atrBytes;
            if(atrBytes[0] == (byte)0x3B){
                convention = Convention.DIRECT;
            }else if(atrBytes[0] == (byte)0x3F){
                convention = Convention.INVERSE;
            }

            numHistoricalBytes = atrBytes[1] & 0x0F;
        }catch(RuntimeException e){ //Catch all RE
            throw new ParseException("Unable to parse ATR according to ISO", e);
        }




    }

    public byte[] getATRBytes(){
        return Arrays.copyOf(atrBytes, atrBytes.length);
    }

    public Convention getConvention(){
        return convention;
    }

    public Protocol getProtocol(){
        return protocol;
    }

    public byte[] getHistoricalBytes(){
        byte[] tmp = new byte[numHistoricalBytes];
        System.arraycopy(atrBytes, atrBytes.length-numHistoricalBytes, tmp, 0, numHistoricalBytes);
        return tmp;
    }

    @Override
    public String toString(){
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent){
        pw.println(Util.getSpaces(indent)+"ISO Compliant Answer To Reset (ATR)");
        String indentStr = Util.getSpaces(indent+3);

        pw.println(indentStr+"Convention - "+convention);
        pw.println(indentStr+"Protocol - "+protocol);

        if(numHistoricalBytes > 0){
            pw.println(indentStr+"Historical bytes - "+Util.prettyPrintHex(Util.byteArrayToHexString(getHistoricalBytes())));
        }

    }
}