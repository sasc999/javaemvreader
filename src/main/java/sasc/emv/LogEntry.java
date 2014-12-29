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

import sasc.iso7816.ShortFileIdentifier;
import java.io.PrintWriter;
import java.io.StringWriter;
import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class LogEntry {
    
    private ShortFileIdentifier sfi;
    private int numRecords;
    
    public LogEntry(byte sfiByte, byte numRecordsByte){
        sfi = new ShortFileIdentifier(sfiByte);
        numRecords = Util.byteToInt(numRecordsByte);
    }
    
    public ShortFileIdentifier getSFI(){
        return sfi;
    }
    
    public int getNumberOfRecords(){
        return numRecords;
    }
    
    @Override
    public String toString(){
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent){
        pw.println(Util.getSpaces(indent)+"Log Entry:");
        String indentStr = Util.getSpaces(indent+Log.INDENT_SIZE);

        sfi.dump(pw, indent+Log.INDENT_SIZE);
        
        pw.println(indentStr+numRecords+" record(s)");

    }
}
