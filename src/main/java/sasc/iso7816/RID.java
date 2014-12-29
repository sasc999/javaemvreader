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

import java.util.Arrays;
import sasc.util.Util;

/**
 * Registered Application Provider Identifier
 *
 * @author sasc
 */
public class RID {

    private byte[] rid;
    private String applicant;
    private String country;

    public RID(byte[] rid, String applicant, String country){
        if (rid == null){
            throw new IllegalArgumentException("Argument 'rid' cannot be null");
        }
        if (rid.length != 5) {
            throw new SmartCardException("RID length != 5. Length=" + rid.length);
        }
        if(applicant == null){
            applicant = "";
        }
        if(country == null){
            country = "";
        }
        this.rid = rid;
        this.applicant = applicant;
        this.country = country;
    }

    public RID(String rid, String applicant, String country) {
        this(Util.fromHexString(rid), applicant, country);
    }

    public byte[] getRIDBytes() {
        return Arrays.copyOf(rid, rid.length);
    }

    public String getApplicant(){
        return applicant;
    }

    public String getCountry(){
        return country;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RID other = (RID) obj;
        if (!Arrays.equals(this.rid, other.rid)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + Arrays.hashCode(this.rid);
        return hash;
    }

    @Override
    public String toString() {
        return Util.prettyPrintHex(rid) + " " + applicant + " " + country;
    }
}
