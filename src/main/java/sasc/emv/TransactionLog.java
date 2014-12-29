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
import java.util.ArrayList;
import java.util.List;
import sasc.iso7816.TagAndLength;
import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class TransactionLog {

    private LogFormat logFormat;
    private List<Record> logRecords = new ArrayList<Record>();
    private boolean isProcessed = false;
    
    public TransactionLog(LogFormat logFormat) {
        this.logFormat = logFormat;
    }

    public LogFormat getLogFormat() {
        return logFormat;
    }

    public void addRecord(byte[] logRecord) {
        if (logRecord != null) {
            if (logRecord.length == logFormat.getRecordLength()) {
                logRecords.add(new Record(logRecord));
            } else {
                Log.debug("logRecord length (" + logRecord.length + ") does not match logFormat length (" + logFormat.getRecordLength() + ")");
            }
        }
    }
    
    public boolean isEmpty() {
        return logRecords.isEmpty();
    }
    
    public boolean isProcessed() {
        return isProcessed;
    }
    
    public void setProcessed() {
        this.isProcessed = true;
    }
    
    public List<Record> getRecords() {
        return logRecords;
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "Transaction Log:");
        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);

        logFormat.dump(pw, indent + Log.INDENT_SIZE);

        pw.println(indentStr + "Log Record(s):");
        if (!logRecords.isEmpty()) {

            for (Record record : logRecords) {
                //TODO format record
                record.dump(pw, indent + Log.INDENT_SIZE*2);
            }
        } else {
            pw.println(Util.getSpaces(indent+Log.INDENT_SIZE*2) + "No Log Records found");
        }

    }

    public class Record {

        private byte[] recordData;

        private Record(byte[] recordData) {
            this.recordData = recordData;
        }

        @Override
        public String toString() {
            StringWriter sw = new StringWriter();
            dump(new PrintWriter(sw), 0);
            return sw.toString();
        }

        public void dump(PrintWriter pw, int indent) {
            pw.println(Util.getSpaces(indent) + "Record:");
            String indentStr = Util.getSpaces(indent + 3);

            int offset = 0;
            for (TagAndLength tagAndLength : logFormat.getTagAndLengthList()) {
                byte[] value = new byte[tagAndLength.getLength()];
                System.arraycopy(recordData, offset, value, 0, value.length);
                pw.println(indentStr + tagAndLength.getTag().getName() + ": " + Util.prettyPrintHexNoWrap(value));
                offset += value.length;
            }

        }
    }

    public static void main(String[] args) {
        LogFormat logFormat = new LogFormat(Util.fromHexString("9f 02 06 9f 27 01 9f 1a 02 5f 2a 02 9a 03 9c 01"));
        TransactionLog tl = new TransactionLog(logFormat);
        tl.addRecord(Util.fromHexString("00 00 00 00 38 70 40 02 50 09 78 12 04 21 00"));
        tl.addRecord(Util.fromHexString("00 00 00 00 21 90 40 02 50 09 78 12 04 17 00"));
        tl.addRecord(Util.fromHexString("00 00 00 00 39 20 40 02 50 09 78 12 04 13 00"));
        tl.addRecord(Util.fromHexString("00 00 00 01 30 00 40 02 50 09 78 12 04 07 00"));
        tl.addRecord(Util.fromHexString("00 00 00 00 61 24 40 02 50 09 78 12 04 05 00"));
        tl.addRecord(Util.fromHexString("00 00 00 00 06 73 40 02 50 09 78 12 04 03 00"));
        tl.addRecord(Util.fromHexString("00 00 00 00 15 20 40 02 50 09 78 12 03 28 00"));
        tl.addRecord(Util.fromHexString("00 00 00 00 17 50 40 02 50 09 78 12 03 26 00"));
        tl.addRecord(Util.fromHexString("00 00 00 00 09 80 40 02 50 09 78 12 03 26 00"));
        tl.addRecord(Util.fromHexString("00 00 00 00 61 24 40 02 50 09 78 12 03 20 00"));
        tl.addRecord(Util.fromHexString("00 00 00 01 00 00 40 02 50 09 78 12 02 28 00"));

        System.out.println(tl);
    }
}
