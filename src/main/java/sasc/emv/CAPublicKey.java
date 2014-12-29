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

import sasc.iso7816.SmartCardException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class CAPublicKey {
    private byte[] exponent = null;
    private byte[] modulus = null;
    private int index;
    private String description;
    private Date expirationDate;
    private int hashAlgorithmIndicator;
    private int publicKeyAlgorithmIndicator;
    
    //A check value calculated on the concatenation of all parts of the 
    //Certification Authority Public Key:
    //- RID
    //- Certification Authority Public Key Index 
    //- Certification Authority Public Key Modulus
    //- Certification Authority Public Key Exponent
    //using SHA-1
    private byte[] sha1CheckSum = null;

    public CAPublicKey(int index, byte[] exponent, byte[] modulus, byte[] sha1CheckSum, int publicKeyAlgorithmIndicator, int hashAlgorithmIndicator, String description, Date expirationDate){
        this.index = index;
        this.exponent = exponent;
        this.modulus = modulus;
        this.sha1CheckSum = sha1CheckSum;
        this.publicKeyAlgorithmIndicator = publicKeyAlgorithmIndicator;
        this.hashAlgorithmIndicator = hashAlgorithmIndicator;
        this.description = description;
        this.expirationDate = expirationDate;
    }

    public int getIndex(){
        return index;
    }

    public int getKeyLengthInBytes(){
        return modulus.length;
    }

    public byte[] getExponent(){
        return Util.copyByteArray(exponent);
    }

    public byte[] getModulus(){
        return Util.copyByteArray(modulus);
    }

    public byte[] getCertificationAuthorityPublicKeyCheckSum(){
        return Util.copyByteArray(sha1CheckSum);
    }

    public Date getExpirationDate(){
        return (Date)expirationDate.clone();
    }

    public int getHashAlgorithmIndicator(){
        return hashAlgorithmIndicator;
    }

    public int getPublicKeyAlgorithmIndicator(){
        return publicKeyAlgorithmIndicator;
    }

    public String getDescription(){
        return description;
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "CA Public Key");
        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);

        pw.println(indentStr + "Size: "+getKeyLengthInBytes()*8+"-bit");
        pw.println(indentStr + "Exponent:");
        pw.println(indentStr + "   " + Util.prettyPrintHex(Util.byteArrayToHexString(exponent), indent+Log.INDENT_SIZE*2));
        pw.println(indentStr + "Modulus:");
        pw.println(indentStr + "   " + Util.prettyPrintHex(Util.byteArrayToHexString(modulus), indent+Log.INDENT_SIZE*2));
        pw.println(indentStr + "Checksum:");
        pw.println(indentStr + "   " + Util.prettyPrintHex(Util.byteArrayToHexString(sha1CheckSum), indent+Log.INDENT_SIZE*2));
    }

}
