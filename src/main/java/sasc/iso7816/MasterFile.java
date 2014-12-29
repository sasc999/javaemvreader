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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class MasterFile implements File {
    private byte[] data;
    private List<BERTLV> unhandledRecords = new ArrayList<BERTLV>();

    public MasterFile(byte[] data){
        this.data = data;
    }
    
    public void addUnhandledRecord(BERTLV bertlv) {
        unhandledRecords.add(bertlv);
    }

    public List<BERTLV> getUnhandledRecords() {
        return Collections.unmodifiableList(unhandledRecords);
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "Master File");

        pw.println(Util.getSpaces(indent+Log.INDENT_SIZE) + Util.prettyPrintHex(Util.byteArrayToHexString(data), indent+Log.INDENT_SIZE));
        
        pw.println("");
        
        if (!unhandledRecords.isEmpty()) {
            pw.println(Util.getSpaces(indent + Log.INDENT_SIZE) + "UNHANDLED RECORDS (" + unhandledRecords.size() + " found):");

            for (BERTLV tlv : unhandledRecords) {
                pw.println(Util.getSpaces(indent + Log.INDENT_SIZE*2) + tlv.getTag() + " " + tlv);
            }
        }

    }
}
