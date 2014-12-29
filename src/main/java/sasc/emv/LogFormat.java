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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import sasc.iso7816.TLVUtil;
import sasc.util.Log;
import sasc.util.Util;

/**
 * List (in tag and length format) of data objects representing the logged data elements that are passed to the terminal when a transaction log record is read
 * 
 * @author sasc
 */
public class LogFormat {
    private List<TagAndLength> formatList;
    private int recordLength = 0;

    public LogFormat(byte[] formatBytes){
        this.formatList = TLVUtil.parseTagAndLength(formatBytes);
        for(TagAndLength tal : formatList){
            recordLength+=tal.getLength();
        }
    }

    public List<TagAndLength> getTagAndLengthList(){
        return formatList;
    }
    
    public int getRecordLength(){
        return recordLength;
    }
    
    @Override
    public String toString(){
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent){
        pw.println(Util.getSpaces(indent)+"Log Format:");
        String indentStr = Util.getSpaces(indent+Log.INDENT_SIZE);

        for(TagAndLength tagAndLength : formatList){
            int length = tagAndLength.getLength();
            pw.println(indentStr+tagAndLength.getTag().getName() + " ("+length+ " "+(length==1?"byte":"bytes")+")");
        }
    }
}
