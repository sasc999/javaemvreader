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
 *
 * @author sasc
 */
public class IssuerPublicKeyCertificate {

    private IssuerPublicKey issuerPublicKey;
    private int caPublicKeyIndex = -1;
    private boolean isValid = false;
    private byte[] signedBytes;
    private CA ca;
    private int issuerIdentifier = -1;
    private byte certFormat;
    private byte[] certExpirationDate = new byte[2];
    private byte[] certSerialNumber = new byte[3];
    private int hashAlgorithmIndicator;
    private int issuerPublicKeyAlgorithmIndicator;
    private byte[] hash = new byte[20];
    private boolean validationPerformed = false;

    public IssuerPublicKeyCertificate(CA ca) {
        //ca == null is permitted
        this.ca = ca;
        issuerPublicKey = new IssuerPublicKey();
    }

    public void setCAPublicKeyIndex(int index) {
        this.caPublicKeyIndex = index;
    }

    public void setSignedBytes(byte[] signedBytes) {
        this.signedBytes = signedBytes;
    }

    public IssuerPublicKey getIssuerPublicKey() {
        return issuerPublicKey;
    }

    //Perform lazy validation, since we might not have all the data elements initially
    //This method must only be called after ALL application records have been read
    public boolean validate() {
        if (validationPerformed) { //Validation already run
            return isValid();
        }
        validationPerformed = true;
        if(this.ca == null){
            isValid = false;
            return isValid();
        }
        CAPublicKey caPublicKey = ca.getPublicKey(caPublicKeyIndex);

        if (caPublicKey == null) {
            isValid = false;
            return isValid();
//            throw new SmartCardException("No suitable CA Public Key found");
        }
        //Decipher data using RSA
        byte[] recoveredBytes = Util.performRSA(signedBytes, caPublicKey.getExponent(), caPublicKey.getModulus());

        Log.debug("IssuerPKCert recoveredBytes="+Util.prettyPrintHex(recoveredBytes));

        ByteArrayInputStream bis = new ByteArrayInputStream(recoveredBytes);

        if (bis.read() != 0x6a) { //Header
            throw new SmartCardException("Header != 0x6a");
        }

        certFormat = (byte) bis.read();

        if (certFormat != 0x02) {
            throw new SmartCardException("Invalid certificate format");
        }

        byte[] issuerIdentifierPaddedBytes = new byte[4];

        bis.read(issuerIdentifierPaddedBytes, 0, issuerIdentifierPaddedBytes.length);

        //Remove padding (if any) from issuerIdentifier
        String iiStr = Util.byteArrayToHexString(issuerIdentifierPaddedBytes);
        int padStartIndex = iiStr.toUpperCase().indexOf('F');
        if(padStartIndex != -1){
            iiStr = iiStr.substring(0, padStartIndex);
        }
        issuerIdentifier = Util.binaryHexCodedDecimalToInt(iiStr);

        bis.read(certExpirationDate, 0, certExpirationDate.length);

        bis.read(certSerialNumber, 0, certSerialNumber.length);

        hashAlgorithmIndicator = bis.read() & 0xFF;

        issuerPublicKeyAlgorithmIndicator = bis.read() & 0xFF;

        int issuerPublicKeyModLengthTotal = bis.read() & 0xFF;

        int issuerPublicKeyExpLengthTotal = bis.read() & 0xFF;

        int modBytesLength = bis.available() - 21;

        if(issuerPublicKeyModLengthTotal < modBytesLength){
            //The mod bytes block in this cert contains padding.
            //we don't want padding in our key
            modBytesLength = issuerPublicKeyModLengthTotal;
        }

        byte[] modtmp = new byte[modBytesLength];

        bis.read(modtmp, 0, modtmp.length);

        issuerPublicKey.setModulus(modtmp);

        //Now read padding bytes (0xbb), if available
        //The padding bytes are not used
        byte[] padding = new byte[bis.available()-21];
        bis.read(padding, 0, padding.length);

        bis.read(hash, 0, hash.length);

        ByteArrayOutputStream hashStream = new ByteArrayOutputStream();

        hashStream.write(certFormat);
        hashStream.write(issuerIdentifierPaddedBytes, 0, issuerIdentifierPaddedBytes.length);
        hashStream.write(certExpirationDate, 0, certExpirationDate.length);
        hashStream.write(certSerialNumber, 0, certSerialNumber.length);
        hashStream.write((byte)hashAlgorithmIndicator);
        hashStream.write((byte)issuerPublicKeyAlgorithmIndicator);
        hashStream.write((byte)issuerPublicKeyModLengthTotal);
        hashStream.write((byte)issuerPublicKeyExpLengthTotal);
        byte[] ipkModulus = issuerPublicKey.getModulus();
        hashStream.write(ipkModulus, 0, ipkModulus.length);
        byte[] ipkExponent = issuerPublicKey.getExponent();
        hashStream.write(ipkExponent, 0, ipkExponent.length);


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
        pw.println(Util.getSpaces(indent) + "Issuer Public Key Certificate");
        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);

        if (!validationPerformed) {
            validate();
        }

        if (isValid()) {
            pw.println(indentStr + "Issuer Identifier: " + issuerIdentifier);
            if (caPublicKeyIndex != -1) {
                pw.println(indentStr + "CA Public Key Index: " + caPublicKeyIndex);
            }
            pw.println(indentStr + "Certificate Format: " + certFormat);
            pw.println(indentStr + "Certificate Expiration Date (MMYY): " + Util.byteArrayToHexString(certExpirationDate));
            pw.println(indentStr + "Certificate Serial Number: " + Util.byteArrayToHexString(certSerialNumber) + " ("+Util.byteArrayToInt(certSerialNumber)+")");
            pw.println(indentStr + "Hash Algorithm Indicator: " + hashAlgorithmIndicator + " (=SHA-1)");
            pw.println(indentStr + "Issuer Public Key Algorithm Indicator: " + issuerPublicKeyAlgorithmIndicator + " (=RSA)");
            pw.println(indentStr + "Hash: " + Util.byteArrayToHexString(hash));

            issuerPublicKey.dump(pw, indent + Log.INDENT_SIZE);
        } else {
            if(this.ca == null){
                pw.println(indentStr + "NO CA CONFIGURED FOR THIS RID. UNABLE TO VALIDATE CERTIFICATE");
            }else{
                pw.println(indentStr + "CERTIFICATE NOT VALID");
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        byte[] rid = Util.fromHexString("a0 00 00 00 03");
        byte[] mod = Util.fromHexString("BE9E1FA5E9A803852999C4AB432DB28600DCD9DAB76DFAAA47355A0FE37B1508AC6BF38860D3C6C2E5B12A3CAAF2A7005A7241EBAA7771112C74CF9A0634652FBCA0E5980C54A64761EA101A114E0F0B5572ADD57D010B7C9C887E104CA4EE1272DA66D997B9A90B5A6D624AB6C57E73C8F919000EB5F684898EF8C3DBEFB330C62660BED88EA78E909AFF05F6DA627B");
        byte[] chksum = CA.calculateCAPublicKeyCheckSum(rid, Util.intToByteArray(149), mod, new byte[]{0x03});
        System.out.println(Util.prettyPrintHexNoWrap(chksum));
        CA.initFromFile("/certificationauthorities_test.xml");
        CA ca = CA.getCA(rid);
        IssuerPublicKeyCertificate cert = new IssuerPublicKeyCertificate(ca);
        cert.setCAPublicKeyIndex(149);
        String signedBytesStr = "8b 39 01 f6 25 30 48 a8 b2 cb 08 97 4a 42 45 d9" +
                                "0e 1f 0c 4a 2a 69 bc a4 69 61 5a 71 db 21 ee 7b" +
                                "3a a9 42 00 cf ae dc d6 f0 a7 d9 ad 0b f7 92 13" +
                                "b6 a4 18 d7 a4 9d 23 4e 5c 97 15 c9 14 0d 87 94" +
                                "0f 2e 04 d6 97 1f 4a 20 4c 92 7a 45 5d 4f 8f c0" +
                                "d6 40 2a 79 a1 ce 05 aa 3a 52 68 67 32 98 53 f5" +
                                "ac 2f eb 3c 6f 59 ff 6c 45 3a 72 45 e3 9d 73 45" +
                                "14 61 72 57 95 ed 73 09 70 99 96 3b 82 eb f7 20" +
                                "3c 1f 78 a5 29 14 0c 18 2d bb e6 b4 2a e0 0c 02";
        byte[] signedBytes = Util.fromHexString(signedBytesStr);
        cert.setSignedBytes(signedBytes);
        
        String remainderStr = "33 f5 e4 44 7d 4a 32 e5 93 6e 5a 13 39 32 9b b4 e8 dd 8b f0 04 4c e4 42 8e 24 d0 86 6f ae fd 23 48 80 9d 71";
        cert.getIssuerPublicKey().setExponent(new byte[]{0x03});
        cert.getIssuerPublicKey().setRemainder(Util.fromHexString(remainderStr));
        
        System.out.println(cert.toString());
    }
}
