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
import sasc.lookup.RID_DB;
import sasc.terminal.KnownAIDList;
import sasc.util.Log;
import sasc.util.Util;

/**
 * Application Identifier (AID)
 *
 * See EMV Book 1 page 153
 * @author sasc
 */
public class AID {

    //Registered Application Provider Identifier
    private byte[] rid;
    //Proprietary Application Identifier Extension
    private byte[] pix;

    public AID(byte[] rid, byte[] pix) {
        if (rid == null || pix == null){
            throw new IllegalArgumentException("Arguments 'rid' and 'pix' cannot be null");
        }
        if (rid.length != 5) {
            throw new IllegalArgumentException("RID length != 5. Length=" + rid.length);
        }
        if (pix.length > 11) {
            throw new IllegalArgumentException("PIX length > 11. Length=" + pix.length);
        }
        this.rid = rid;
        this.pix = pix;
    }

    public AID(byte[] aid) {
        if (aid == null){
            throw new IllegalArgumentException("Argument 'aid' cannot be null");
        }
        if (aid.length < 5) {
            throw new IllegalArgumentException("AID length < 5. Length=" + aid.length);
        }
        if (aid.length > 16) {
            throw new IllegalArgumentException("AID length > 16. Length=" + aid.length);
        }
        rid = new byte[5];
        System.arraycopy(aid, 0, rid, 0, rid.length);
        pix = new byte[aid.length-rid.length];
        System.arraycopy(aid, rid.length, pix, 0, pix.length);

    }

    public AID(String aid) {
        this(Util.fromHexString(aid));
    }

    public byte[] getAIDBytes() {
        byte[] tmp = new byte[rid.length + pix.length];
        System.arraycopy(rid, 0, tmp, 0, rid.length);
        System.arraycopy(pix, 0, tmp, rid.length, pix.length);
        return tmp;
    }

    public byte[] getRIDBytes() {
        return Util.copyByteArray(rid);
    }

    public byte[] getPIXBytes() {
        return Util.copyByteArray(pix);
    }

    public boolean belongsToRID(byte[] rid) {
        return Arrays.equals(this.rid, rid);
    }

    @Override
    public String toString(){
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent){
        KnownAIDList.KnownAID knownAID = KnownAIDList.searchAID(getAIDBytes());
        String aidName = "";
        if(knownAID != null){
            aidName = " ("+knownAID.getName()+")";
        }
        pw.println(Util.getSpaces(indent)+"AID: "+Util.prettyPrintHexNoWrap(getAIDBytes()) + aidName);
        String indentStr = Util.getSpaces(indent+Log.INDENT_SIZE);

        RID ridFromDB = RID_DB.searchRID(rid);
        String description = "";
        if (ridFromDB != null) {
            description = " (" + ridFromDB.getApplicant() + " [" + ridFromDB.getCountry() + "])";
        }

        pw.println(indentStr+"RID: "+Util.prettyPrintHexNoWrap(rid) + description);
        pw.println(indentStr+"PIX: "+Util.prettyPrintHexNoWrap(pix));
    }

    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof AID)){
            return false;
        }
        AID that = (AID)obj;
        if(this == that){
            return true;
        }
        if(Arrays.equals(this.getAIDBytes(), that.getAIDBytes())){
            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Arrays.hashCode(this.rid);
        hash = 97 * hash + Arrays.hashCode(this.pix);
        return hash;
    }
}
