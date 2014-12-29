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
import java.util.Arrays;
import sasc.util.Log;
import sasc.util.Util;

/**
 * Application Record
 * 
 * @author sasc
 */
public class Record {

    private byte[] rawDataIncTag;
    private boolean isInvolvedInOfflineDataAuthentication = false;
    private int recordNumber;

    public Record(byte[] rawDataIncTag, int recordNumber, boolean isInvolvedInOfflineDataAuthentication){
        this.rawDataIncTag = rawDataIncTag;
        this.recordNumber = recordNumber;
        this.isInvolvedInOfflineDataAuthentication = isInvolvedInOfflineDataAuthentication;
    }

    public byte[] getRawData(){
        return Arrays.copyOf(rawDataIncTag, rawDataIncTag.length);
    }

    public boolean isInvolvedInOfflineDataAuthentication(){
        return isInvolvedInOfflineDataAuthentication;
    }

    public int getRecordNumber(){
        return recordNumber;
    }

    @Override
    public String toString(){
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent){
        pw.println(Util.getSpaces(indent)+"Record: "+getRecordNumber());
        String indentStr = Util.getSpaces(indent+Log.INDENT_SIZE);
        pw.println(indentStr+"Length: "+rawDataIncTag.length);
        pw.println(indentStr+"Involved In Offline Data Authentication: "+
                isInvolvedInOfflineDataAuthentication());
    }

}
