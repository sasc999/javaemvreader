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
import sasc.iso7816.File;
import sasc.iso7816.SmartCardException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class ApplicationElementaryFile implements File {

    private ShortFileIdentifier sfi;
    private int startRecordNumber;
    private int endRecordNumber;
    private int numRecordsInvolvedInOfflineDataAuthentication;
    private Map<Integer, Record> records = new LinkedHashMap<Integer, Record>(); //LinkedHashMap to keep the correct insertion order

    public ApplicationElementaryFile(byte[] data){
        if(data.length != 4){
            throw new SmartCardException("Applicaton Elementary File length must be equal to 4. Data length="+data.length);
        }

        int sfiNumber = data[0] >>> 3; 
        sfi = new ShortFileIdentifier(sfiNumber);
        startRecordNumber = data[1] & 0xFF;
        if(startRecordNumber == 0){
            throw new SmartCardException("Applicaton Elementary File: Start Record number cannot be 0");
        }
        endRecordNumber = data[2] & 0xFF;
        if(endRecordNumber < startRecordNumber){
            throw new SmartCardException("Applicaton Elementary File: End Record number ("+endRecordNumber+") < Start Record number ("+startRecordNumber+")");
        }
        numRecordsInvolvedInOfflineDataAuthentication = data[3] & 0xFF;
    }

    public ShortFileIdentifier getSFI(){
        return sfi;
    }

    public int getStartRecordNumber(){
        return startRecordNumber;
    }

    public int getEndRecordNumber(){
        return endRecordNumber;
    }

    public int getNumRecordsInvolvedInOfflineDataAuthentication(){
        return numRecordsInvolvedInOfflineDataAuthentication;
    }

    public void setRecord(int recordNum, Record record){
        Integer recordNumber = Integer.valueOf(recordNum);
        if(records.containsKey(recordNumber)){
            throw new IllegalArgumentException("Record number "+recordNum+ " already added: "+record);
        }
        records.put(new Integer(recordNum), record);
    }
    
    public Record getRecord(int recordNum){
        return records.get(Integer.valueOf(recordNum));
    }

    public Collection<Record> getRecords(){
        return Collections.unmodifiableCollection(records.values());
    }

    @Override
    public String toString(){
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent){
        pw.println(Util.getSpaces(indent)+"Application Elementary File");
        String indentStr = Util.getSpaces(indent+Log.INDENT_SIZE);
        if(sfi != null){
            sfi.dump(pw, indent+Log.INDENT_SIZE);
        }
        pw.println(indentStr+"Start Record: "+getStartRecordNumber());
        pw.println(indentStr+"End Record: "+getEndRecordNumber());
        pw.println(indentStr+"Number of Records Involved In Offline Data Authentication: "+
                getNumRecordsInvolvedInOfflineDataAuthentication());

        for(Record record : records.values()){
            record.dump(pw, indent+Log.INDENT_SIZE);
        }
    }

}
