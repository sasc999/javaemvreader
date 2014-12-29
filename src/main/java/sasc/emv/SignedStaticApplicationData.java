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

import sasc.util.Log;
import sasc.iso7816.SmartCardException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import sasc.util.Util;

/**
 * Signed Static Application Data
 * Digital signature on critical application parameters for SDA
 *
 * This object contains the hash that has been computed over 'critical' application records (Elementary Files)
 *
 * @author sasc
 */
public class SignedStaticApplicationData {

    private EMVApplication application;
    private boolean isValid = false;
    private byte[] signedBytes;
    private byte signedDataFormat;
    private byte[] dataAuthenticationCode = new byte[2];
    private int hashAlgorithmIndicator;
    private byte[] hash = new byte[20];
    private boolean validationPerformed = false;

    public SignedStaticApplicationData(EMVApplication app) {
        this.application = app;
    }

    public void setSignedBytes(byte[] signedBytes) {
        this.signedBytes = signedBytes;
    }

    public IssuerPublicKeyCertificate getIssuerPublicKeyCertificate() {
        return application.getIssuerPublicKeyCertificate();
    }
    
    public byte[] getDataAuthenticationCode(){
        return Arrays.copyOf(dataAuthenticationCode, dataAuthenticationCode.length);
    }

    public boolean validate() {
        if (validationPerformed) { //Validation already run
            return isValid();
        }
        validationPerformed = true; //'isValid' flag set further down

        if(!application.getIssuerPublicKeyCertificate().validate()){ //Make sure the cert has been initialized
            isValid = false;
            return isValid();
        }

        IssuerPublicKey issuerPublicKey = application.getIssuerPublicKeyCertificate().getIssuerPublicKey();

        //If the Signed Static Application Data has a length different from
        //the length of the Issuer Public Key Modulus, SDA has failed.
        if (signedBytes.length != issuerPublicKey.getModulus().length) {
            throw new SmartCardException("Invalid Signed Data: Signed data length (" + signedBytes.length + ") != Issuer Public Key Modulus length(" + issuerPublicKey.getModulus().length + ")");
        }

        byte[] decipheredBytes = Util.performRSA(signedBytes, issuerPublicKey.getExponent(), issuerPublicKey.getModulus());

        ByteArrayInputStream stream = new ByteArrayInputStream(decipheredBytes);

        if (stream.read() != 0x6a) { //Header
            throw new SmartCardException("Header != 0x6a");
        }

        signedDataFormat = (byte) stream.read();

        if (signedDataFormat != 0x03) { //Always 0x03
            throw new SmartCardException("Invalid Signed Data format");
        }

        hashAlgorithmIndicator = stream.read() & 0xFF;

        stream.read(dataAuthenticationCode, 0, dataAuthenticationCode.length);

        //Now read the padding bytes (0xbb)
        //The padding bytes are not used
        byte[] padding = new byte[stream.available() - 21];
        stream.read(padding, 0, padding.length);

//        Log.debug("SSAD PADDING: "+Util.byteArrayToHexString(padding));

        stream.read(hash, 0, hash.length);

        //EMV book 2 page 60
        ByteArrayOutputStream hashStream = new ByteArrayOutputStream();

        hashStream.write(signedDataFormat);
        byte[] hashAlgoIndArr = Util.intToByteArray(hashAlgorithmIndicator);
        hashStream.write(hashAlgoIndArr, 0, hashAlgoIndArr.length);
        hashStream.write(dataAuthenticationCode, 0, dataAuthenticationCode.length);
        hashStream.write(padding, 0, padding.length);

        byte[] offlineAuthenticationRecords = application.getOfflineDataAuthenticationRecords();

        Log.debug("OfflineDataAuthenticationRecords: "+Util.prettyPrintHex(offlineAuthenticationRecords));

        hashStream.write(offlineAuthenticationRecords, 0, offlineAuthenticationRecords.length);

        byte[] sha1Result = null;
        try {
            sha1Result = Util.calculateSHA1(hashStream.toByteArray());
        } catch (NoSuchAlgorithmException ex) {
            throw new SignedDataException("SHA-1 hash algorithm not available", ex);
        }

        if (!Arrays.equals(sha1Result, hash)) {
            throw new SignedDataException("Hash is not valid");
        }



        int trailer = stream.read();

        if (trailer != 0xbc) {//Trailer always 0xbc
            throw new SmartCardException("Trailer != 0xbc");
        }

        if (stream.available() > 0) {
            throw new SmartCardException("Error parsing Signed Static Application Data. Bytes left=" + stream.available());
        }

        isValid = true;
        return true;
    }

    public boolean isValid() {
        return isValid;
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "Signed Static Application Data");
        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);

        if (!validationPerformed) {
            validate();
        }

        if (isValid()) {
            pw.println(indentStr + "Hash Algorithm Indicator: " + hashAlgorithmIndicator + " (=SHA-1)");
            pw.println(indentStr + "Data Authentication Code: " + Util.byteArrayToHexString(dataAuthenticationCode));
            pw.println(indentStr + "Hash: " + Util.byteArrayToHexString(hash));

        } else {
            if(!application.getIssuerPublicKeyCertificate().validate()){
                pw.println(indentStr + "ISSUER CERTIFICATE NOT VALID. UNABLE TO VALIDATE DATA");
            }else{
                pw.println(indentStr + "SIGNED DATA NOT VALID");
            }
        }
    }
}
