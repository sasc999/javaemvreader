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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class SignedDynamicApplicationData {

    private byte header;
    private byte signedDataFormat;
    private byte hashAlgorithmIndicator;
    private byte iccDynamicDataLenght;
    private byte[] iccDynamicNumber;
    private byte[] hashResult;
    private byte[] decipheredData;
    private byte trailer;
    private byte[] terminalDynamicData;
    private boolean isValid = false;
    private boolean validationPerformed = false;

    private SignedDynamicApplicationData(byte[] data, byte[] terminalDynamicData) {
        this.decipheredData = data;
        this.terminalDynamicData = terminalDynamicData;
    }

    //DDA/Internal Auth must be performed after all app records have been read.
    public boolean validate() {
        if (validationPerformed) { //Validation already run
            return isValid();
        }
        validationPerformed = true; //'isValid' flag set further down

        ByteArrayInputStream stream = new ByteArrayInputStream(decipheredData);

        header = (byte) stream.read();

        if (header != (byte) 0x6a) {
            throw new SignedDataException("Header != 0x6a");
        }

        signedDataFormat = (byte) stream.read();

        if (signedDataFormat != (byte) 0x05) {
            throw new SignedDataException("Signed Data Format != 0x05");
        }

        hashAlgorithmIndicator = (byte) stream.read(); //We currently only support SHA-1
        iccDynamicDataLenght = (byte) stream.read();

        iccDynamicNumber = new byte[iccDynamicDataLenght];

        stream.read(iccDynamicNumber, 0, iccDynamicDataLenght);

        //Now read padding bytes (0xbb), if available
        //The padding bytes are used in hash validation
        byte[] padding = new byte[stream.available()-21];
        stream.read(padding, 0, padding.length);

        hashResult = new byte[20];

        stream.read(hashResult, 0, 20);

        ByteArrayOutputStream hashStream = new ByteArrayOutputStream();

        //EMV Book 2, page 67, table 15

        //Header not included in hash
        hashStream.write(signedDataFormat);
        hashStream.write(hashAlgorithmIndicator);
        hashStream.write((byte)iccDynamicDataLenght);
        hashStream.write(iccDynamicNumber, 0, iccDynamicNumber.length);
        hashStream.write(padding, 0, padding.length);
        hashStream.write(terminalDynamicData, 0, terminalDynamicData.length);
        //Trailer not included in hash

        byte[] sha1Result = null;
        try {
            sha1Result = Util.calculateSHA1(hashStream.toByteArray());
        } catch (NoSuchAlgorithmException ex) {
            throw new SignedDataException("SHA-1 hash algorithm not available", ex);
        }
        if(!Arrays.equals(sha1Result, hashResult)){
            throw new SignedDataException("Hash is not valid");
        }

        trailer = (byte) stream.read();

        if (trailer != (byte) 0xbc) {
            throw new SignedDataException("Trailer != 0xbc");
        }

        isValid = true;
        return true;
    }

    public boolean isValid() {
        return isValid;
    }

    public static SignedDynamicApplicationData parseSignedData(byte[] data, ICCPublicKey iccPublicKey, byte[] terminalDynamicData) {

        byte[] expBytesICC = iccPublicKey.getExponent();
        byte[] modBytesICC = iccPublicKey.getModulus();
        
        if (data.length != modBytesICC.length) {
            throw new SignedDataException("Data length does not equal key length. Data length=" + data.length + " Key length="+modBytesICC.length);
        }

        byte[] decipheredBytes = Util.performRSA(data, expBytesICC, modBytesICC);

        return new SignedDynamicApplicationData(decipheredBytes, terminalDynamicData);
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "Signed Dynamic Application Data");
        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);

        if(!validationPerformed){
            validate();
        }

        if (isValid()) {
            pw.println(indentStr + "Hash Algorithm Indicator: " + hashAlgorithmIndicator +" (=SHA-1)");
            pw.println(indentStr + "ICC Dynamic Data: " + Util.byteArrayToHexString(iccDynamicNumber));
            pw.println(indentStr + "Hash: " + Util.byteArrayToHexString(hashResult));

        } else {
            pw.println(indentStr + "SIGNED DYNAMIC DATA NOT VALID");
        }
    }

}
