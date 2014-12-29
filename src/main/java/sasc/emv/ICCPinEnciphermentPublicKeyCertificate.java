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
public class ICCPinEnciphermentPublicKeyCertificate {

    private final EMVApplication application;
    private IssuerPublicKeyCertificate issuerPublicKeyCert;
    private final ICCPublicKey iccPublicKey;
    private boolean isValid = false;
    private byte[] signedBytes;
    private byte[] pan = new byte[10];
    private byte certFormat;
    private byte[] certExpirationDate = new byte[2];
    private byte[] certSerialNumber = new byte[3];
    private int hashAlgorithmIndicator;
    private int iccPublicKeyAlgorithmIndicator;
    private byte[] hash = new byte[20];
    private boolean validationPerformed = false;

    public ICCPinEnciphermentPublicKeyCertificate(EMVApplication application, IssuerPublicKeyCertificate issuerPublicKeyCert) {
        this.application = application;
        this.issuerPublicKeyCert = issuerPublicKeyCert;
        this.iccPublicKey = new ICCPublicKey();
    }

    public void setSignedBytes(byte[] signedBytes) {
        this.signedBytes = signedBytes;
    }

    public IssuerPublicKeyCertificate getIssuerPublicKeyCertificate() {
        return issuerPublicKeyCert;
    }

    public ICCPublicKey getICCPublicKey() {
        //Don't validate just yet. Perform validation after ALL apprecords have been read
        return iccPublicKey; //never null
    }

    //This method must only be called after ALL application records have been read
    public boolean validate() {
        if (validationPerformed) { //Validation already run
            return isValid();
        }
        validationPerformed = true;

        if (issuerPublicKeyCert == null) {
			issuerPublicKeyCert = application.getIssuerPublicKeyCertificate();
		}

        if (issuerPublicKeyCert == null){
            //No isser public key cert found
            return isValid();
        }

        if(!issuerPublicKeyCert.validate()){ //Init the cert
            isValid = false;
            return isValid();
        }

        IssuerPublicKey issuerPublicKey = issuerPublicKeyCert.getIssuerPublicKey();

        byte[] recoveredBytes = Util.performRSA(signedBytes, issuerPublicKey.getExponent(), issuerPublicKey.getModulus());

        ByteArrayInputStream bis = new ByteArrayInputStream(recoveredBytes);
        
        if (bis.read() != 0x6a) { //Header
            throw new SmartCardException("Header != 0x6a");
        }

        certFormat = (byte) bis.read();

        if (certFormat != 0x04) { //Always 0x04
            throw new SmartCardException("Invalid certificate format");
        }

        bis.read(pan, 0, pan.length);

        bis.read(certExpirationDate, 0, certExpirationDate.length);

        bis.read(certSerialNumber, 0, certSerialNumber.length);

        hashAlgorithmIndicator = bis.read() & 0xFF;

        iccPublicKeyAlgorithmIndicator = bis.read() & 0xFF;

        int iccPublicKeyModLengthTotal = bis.read() & 0xFF;

        int iccPublicKeyExpLengthTotal = bis.read() & 0xFF;

        int modBytesLength = bis.available() - 21;

        if(iccPublicKeyModLengthTotal < modBytesLength) {
            //The mod bytes block in the cert contains padding
            //we don't want padding in our key
            modBytesLength = iccPublicKeyModLengthTotal;
        }

        byte[] modtmp = new byte[modBytesLength];

        bis.read(modtmp, 0, modtmp.length);

        iccPublicKey.setModulus(modtmp);

        //Now read padding bytes (0xbb), if available
        //The padding bytes are not used
        byte[] padding = new byte[bis.available()-21];
        bis.read(padding, 0, padding.length);

        bis.read(hash, 0, hash.length);

        ByteArrayOutputStream hashStream = new ByteArrayOutputStream();

        //Header not included in hash
        hashStream.write(certFormat);
        hashStream.write(pan, 0, pan.length);
        hashStream.write(certExpirationDate, 0, certExpirationDate.length);
        hashStream.write(certSerialNumber, 0, certSerialNumber.length);
        hashStream.write((byte)hashAlgorithmIndicator);
        hashStream.write((byte)iccPublicKeyAlgorithmIndicator);
        hashStream.write((byte)iccPublicKeyModLengthTotal);
        hashStream.write((byte)iccPublicKeyExpLengthTotal);
        byte[] ipkModulus = iccPublicKey.getModulus();
        int numPadBytes = issuerPublicKey.getModulus().length-42-ipkModulus.length;
        Log.debug("issuerMod: "+issuerPublicKey.getModulus().length + " iccMod: "+ipkModulus.length + " padBytes: "+numPadBytes);
        if(numPadBytes > 0){
            //If NIC <= NI – 42, consists of the full
            //ICC Public Key padded to the right
            //with NI – 42 – NIC bytes of value
            //'BB'
            hashStream.write(ipkModulus, 0, ipkModulus.length);
            for(int i=0; i<numPadBytes; i++){
                hashStream.write((byte)0xBB);
            }
        }else{
            //If NIC > NI – 42, consists of the NI –
            //42 most significant bytes of the
            //ICC Public Key
            //and the NIC – NI + 42 least significant bytes of the ICC Public Key
            hashStream.write(ipkModulus, 0, ipkModulus.length);
        }

        byte[] ipkExponent = iccPublicKey.getExponent();
        hashStream.write(ipkExponent, 0, ipkExponent.length);

        byte[] offlineAuthenticationRecords = application.getOfflineDataAuthenticationRecords();
        hashStream.write(offlineAuthenticationRecords, 0, offlineAuthenticationRecords.length);
        //Trailer not included in hash

        Log.debug("HashStream:\n"+Util.prettyPrintHex(hashStream.toByteArray()));

        byte[] sha1Result = null;
        try {
            sha1Result = Util.calculateSHA1(hashStream.toByteArray());
        } catch (NoSuchAlgorithmException ex) {
            throw new SignedDataException("SHA-1 hash algorithm not available", ex);
        }

        if (!Arrays.equals(sha1Result, hash)) {
            throw new SignedDataException("Hash is not valid");
        }

        int trailer = bis.read();

        if (trailer != 0xbc) {//Trailer
            throw new SmartCardException("Trailer != 0xbc");
        }

        if (bis.available() > 0) {
            throw new SmartCardException("Error parsing certificate. Bytes left=" + bis.available());
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
        pw.println(Util.getSpaces(indent) + "ICC PIN Encipherment Public Key Certificate");
        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);

        if(!validationPerformed){
            validate();
        }

        if (isValid()) {
            pw.println(indentStr + "Primary Account Number (PAN): " + Util.byteArrayToHexString(pan));

            pw.println(indentStr + "Certificate Format: " + certFormat);
            pw.println(indentStr + "Certificate Expiration Date (MMYY): " + Util.byteArrayToHexString(certExpirationDate));
            pw.println(indentStr + "Certificate Serial Number: " + Util.byteArrayToHexString(certSerialNumber));
            pw.println(indentStr + "Hash Algorithm Indicator: " + hashAlgorithmIndicator +" (=SHA-1)");
            pw.println(indentStr + "ICC Public Key Algorithm Indicator: " + iccPublicKeyAlgorithmIndicator +" (=RSA)");
            pw.println(indentStr + "Hash: " + Util.byteArrayToHexString(hash));

            iccPublicKey.dump(pw, indent + Log.INDENT_SIZE);
        } else {
            if (this.issuerPublicKeyCert == null) {
                pw.println(indentStr + "NO ISSUER CERTIFICATE FOUND. UNABLE TO VALIDATE CERTIFICATE");
			} else {
				pw.println(indentStr + "CERTIFICATE NOT VALID");
			}
        }
    }

    public static void main(String[] args){

    }
}
