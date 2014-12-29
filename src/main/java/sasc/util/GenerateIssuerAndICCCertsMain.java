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
package sasc.util;

import java.io.ByteArrayOutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 *
 * @author sasc
 */
public class GenerateIssuerAndICCCertsMain {

    public static void main(String[] args) {

        //TEST CA
        String caModStr = "a7 aa 21 90 d8 fd 7c 23 f8 9c 76 d1 35 38 ed a4"
                + "f5 08 ed 4a c5 1a 23 46 22 44 42 5d 08 de 5d 2e"
                + "3c e4 9f 5f 45 36 79 6b 19 66 7e 00 80 a6 ae e6"
                + "72 1f 6f 38 fb a9 a5 38 84 c4 86 74 cb 11 c0 c9"
                + "3e 3a 88 11 38 22 72 a4 4a 09 b8 86 7b 9d ad 4c"
                + "f5 cc 7e 39 d5 d8 cf 51 85 58 2d 65 be bc c3 e5"
                + "54 fc de bb d5 51 05 b1 ba ff 05 fd 5a 9a 88 3a"
                + "0e 67 d8 ee 15 cc 92 2a 8d e0 b6 13 e2 44 56 8a"
                + "fe 1f 4f fd 31 15 16 dd 87 e3 4e b5 53 4d 7c 3d";
        String caPrivExpStr = "6f c6 c1 0b 3b 53 a8 17 fb 12 f9 e0 ce 25 f3 c3"
                + "4e 05 f3 87 2e 11 6c d9 6c 2d 81 93 5b 3e e8 c9"
                + "7d ed bf 94 d8 ce fb 9c bb 99 a9 55 ab 19 c9 ee"
                + "f6 bf 9f 7b 52 71 18 d0 58 83 04 4d dc b6 80 86"
                + "29 7c 5a b6 25 6c 4c 6c 70 e2 72 de b3 36 b5 e4"
                + "f9 61 7f e7 17 5c b7 06 8c 70 a3 b4 95 61 b6 ed"
                + "83 c4 48 d5 40 bd cc 12 7b 53 b7 82 44 19 fa 29"
                + "6c ba d3 98 c8 d2 1d 49 ef 3a 68 32 ef 36 57 d2"
                + "8a 24 88 39 6a 66 a2 31 c0 b0 ec 87 e0 d4 57 d3";

        byte[] caModBytes = Util.fromHexString(Util.removeCRLFTab(caModStr));
        byte[] caPrivExpBytes = Util.fromHexString(Util.removeCRLFTab(caPrivExpStr));


        byte header = (byte) 0x6a;
        byte certFormat = (byte) 0x02;
        int issuerIdentifier = 492564;
        int certExpirationDateMMYY = 1214;
        byte[] certSerialNumberBytes = Util.fromHexString("00 e1 6d");
        byte hashAlgorithmIndicator = (byte) 0x01;
        byte issuerPublicKeyAlgorithmIndicator = (byte) 0x01;

        //TEST Issuer

        String issuerModStr = "ba 53 3e b8 ec c9 f9 b8 b2 a3 5e ed 3b e0 3f 7d"
                + "3a cf e2 46 a3 4c 8e 75 f5 c7 4a 64 e6 5c 97 cb"
                + "4f 2f ab 97 09 cf 7e 12 89 0e af f1 8a 4f cf b4"
                + "fa 98 18 db c3 be 5f dc 65 91 54 46 cb 86 24 ac"
                + "2d 1e 07 72 f2 52 49 02 f9 8b a5 5b 4b 4b 11 00"
                + "1e 4e cf b7 0f 12 19 a3 97 12 98 e7 ed c5 b9 2b"
                + "8d 44 c9 80 e2 f6 8f 90 8f 9d ad 78 5b c8 f7 38"
                + "4c 06 dc dc 35 97 51 d1 d4 31 52 0d f5 ff 2d 43"
                + "47 4a 88 60 3c 9e fc a0 66 6a 1a 42 bd f0 a4 f5";
        String issuerPubExpStr = "03";
        String issuerPrivExpStr = "7c 37 7f 25 f3 31 51 25 cc 6c e9 f3 7d 40 2a 53"
                + "7c 8a 96 d9 c2 33 09 a3 f9 2f 86 ed ee e8 65 32"
                + "34 ca 72 64 b1 34 fe b7 06 09 ca a1 06 df df cd"
                + "fc 65 65 e7 d7 d4 3f e8 43 b6 38 2f 32 59 6d c8"
                + "1e 14 04 f7 4c 36 db 56 2d 99 3b 0c 4a b1 20 44"
                + "3e c9 d1 92 37 bf 14 90 37 4d 2d 89 76 09 eb 8e"
                + "8d 3f 74 c8 a6 24 4d b5 90 cc 83 71 97 62 97 e2"
                + "10 40 d2 93 4c c6 3e d1 61 fa 49 46 56 40 65 fc"
                + "ca 3f b7 9a ba 0b 6b c2 b7 76 d6 05 ba 3d e3 6b";
        byte[] issuerModBytes = Util.fromHexString(Util.removeCRLFTab(issuerModStr));
        byte[] issuerPubExpBytes = Util.fromHexString(Util.removeCRLFTab(issuerPubExpStr));
        byte[] issuerPrivExpBytes = Util.fromHexString(Util.removeCRLFTab(issuerPrivExpStr));
        byte trailer = (byte) 0xbc;


        byte[] iiBytesTmp = Util.fromHexString(String.valueOf(issuerIdentifier));
        byte[] issuerIdentifierPaddedBytes = new byte[4];
        Arrays.fill(issuerIdentifierPaddedBytes, (byte) 0xFF);
        System.arraycopy(iiBytesTmp, 0, issuerIdentifierPaddedBytes, 0, iiBytesTmp.length);

        byte[] certExpirationDateBytes = Util.fromHexString(String.valueOf(certExpirationDateMMYY));

        ByteArrayOutputStream certStream = new ByteArrayOutputStream();
        certStream.write(header);
        certStream.write(certFormat);
        certStream.write(issuerIdentifierPaddedBytes, 0, issuerIdentifierPaddedBytes.length);
        certStream.write(certExpirationDateBytes, 0, certExpirationDateBytes.length);
        certStream.write(certSerialNumberBytes, 0, certSerialNumberBytes.length);
        byte[] algoIndicators = Util.fromHexString("01 01");
        certStream.write(algoIndicators, 0, algoIndicators.length);
        certStream.write(issuerModBytes.length); //Total length
        certStream.write(issuerPubExpBytes.length);
        certStream.write(issuerModBytes, 0, issuerModBytes.length - 36);


        //Calculate hash
        ByteArrayOutputStream hashStream = new ByteArrayOutputStream();

        hashStream.write(certFormat);
        hashStream.write(issuerIdentifierPaddedBytes, 0, issuerIdentifierPaddedBytes.length);
        hashStream.write(certExpirationDateBytes, 0, certExpirationDateBytes.length);
        hashStream.write(certSerialNumberBytes, 0, certSerialNumberBytes.length);
        hashStream.write((byte) hashAlgorithmIndicator);
        hashStream.write((byte) issuerPublicKeyAlgorithmIndicator);
        hashStream.write((byte) issuerModBytes.length);
        hashStream.write((byte) issuerPubExpBytes.length);
        hashStream.write(issuerModBytes, 0, issuerModBytes.length);
        hashStream.write(issuerPubExpBytes, 0, issuerPubExpBytes.length);


        byte[] sha1Result = null;
        try {
            sha1Result = Util.calculateSHA1(hashStream.toByteArray());
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("SHA-1 hash algorithm not available", ex);
        }

        certStream.write(sha1Result, 0, sha1Result.length);
        certStream.write(trailer);

        byte[] unsignedCertBytes = certStream.toByteArray();

        System.out.println("Isser Public Key Certificate (Unsigned):");
        System.out.println(Util.prettyPrintHex(unsignedCertBytes));
        System.out.println("Isser Public Key Certificate (Signed):");
        System.out.println(Util.prettyPrintHex(Util.performRSA(unsignedCertBytes, caPrivExpBytes, caModBytes)));
        System.out.println("Issuer Public Key Exponent:");
        System.out.println(Util.prettyPrintHex(issuerPubExpBytes));
        System.out.println("Issuer Public Key Remainder:");
        byte[] remainderBytes = new byte[36];
        System.arraycopy(issuerModBytes, issuerModBytes.length - 36, remainderBytes, 0, remainderBytes.length);
        System.out.println(Util.prettyPrintHex(remainderBytes));


        //Signed Static Application Data
        byte signedDataFormat = (byte) 0x03;
        byte[] dataAuthenticationCode = Util.fromHexString("0123");
        ByteArrayOutputStream ssadStream = new ByteArrayOutputStream();
        ssadStream.write(header);
        ssadStream.write(signedDataFormat);
        ssadStream.write(hashAlgorithmIndicator);
        ssadStream.write(dataAuthenticationCode, 0, dataAuthenticationCode.length);
        byte[] padding = new byte[144 - ssadStream.size() - 21];
        Arrays.fill(padding, (byte) 0xbb);
        ssadStream.write(padding, 0, padding.length);


        ByteArrayOutputStream ssadHashStream = new ByteArrayOutputStream();

        ssadHashStream.write(signedDataFormat);
        byte[] hashAlgoIndArr = Util.intToByteArray(hashAlgorithmIndicator);
        ssadHashStream.write(hashAlgoIndArr, 0, hashAlgoIndArr.length);
        ssadHashStream.write(dataAuthenticationCode, 0, dataAuthenticationCode.length);
        ssadHashStream.write(padding, 0, padding.length);

        //offlineAuthenticationRecords
        String oarStr = "5f 24 03 12 03 31 5f 25 03 09 02 05 5a 08 54 11"
                + "11 88 88 88 88 82 5f 34 01 01 9f 07 02 ff 00 8e"
                + "12 00 00 00 00 00 00 00 00 42 01 41 03 5e 03 42"
                + "03 1f 00 9f 0d 05 f0 20 24 28 00 9f 0e 05 00 50"
                + "80 00 00 9f 0f 05 f0 28 3c f8 00 5f 28 02 05 78"
                + "5c 00";
        byte[] offlineAuthenticationRecords = Util.fromHexString(oarStr);
        System.out.println(Util.getSafePrintChars(offlineAuthenticationRecords));
        ssadHashStream.write(offlineAuthenticationRecords, 0, offlineAuthenticationRecords.length);

        try {
            sha1Result = Util.calculateSHA1(ssadHashStream.toByteArray());
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("SHA-1 hash algorithm not available", ex);
        }
        ssadStream.write(sha1Result, 0, sha1Result.length);
        ssadStream.write(trailer);

        byte[] unsignedSSAD = ssadStream.toByteArray();
        System.out.println("Signed Static Application Data (unsigned):");
        System.out.println(Util.prettyPrintHex(unsignedSSAD));
        System.out.println("Signed Static Application Data (Signed):");
        System.out.println(Util.prettyPrintHex(Util.performRSA(unsignedSSAD, issuerPrivExpBytes, issuerModBytes)));
    }
}
