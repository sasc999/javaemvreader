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

import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;

public class RSAKeyGenerator {

    public static class RSAKeyPair {

        private RSAPublicKey publicKey;
        private RSAPrivateKey privateKey;

        RSAKeyPair(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }

        public RSAPublicKey getPublicKey() {
            return publicKey;
        }

        public RSAPrivateKey getPrivateKey() {
            return privateKey;
        }
    }

    public static RSAKeyPair generateRSAKeys(int numBits, int exponent) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        AlgorithmParameterSpec spec = new RSAKeyGenParameterSpec(numBits, BigInteger.valueOf(exponent));
        keyGen.initialize(spec, new SecureRandom()); //For production code: also specify a hardware SecureRandom
        KeyPair keyPair = keyGen.genKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        RSAPrivateKey rsaPrivKey = (RSAPrivateKey) privateKey;
        RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;

        return new RSAKeyPair(rsaPublicKey, rsaPrivKey);

    }

    private static void dump(RSAKeyPair rsaKeyPair) {

        RSAPrivateKey rsaPrivKey = rsaKeyPair.getPrivateKey();
        RSAPublicKey rsaPublicKey = rsaKeyPair.getPublicKey();
        System.out.println(rsaPrivKey);
        System.out.println(rsaPublicKey);

        System.out.println("\nRSA Key Shared Modulus Hex:");
        System.out.println(Util.prettyPrintHex(rsaPrivKey.getModulus()));
        System.out.println("\nRSA Private Key Exponent Hex:");
        System.out.println(Util.prettyPrintHex(rsaPrivKey.getPrivateExponent()));
        System.out.println("\nRSA Public Key Exponent Hex:");
        System.out.println(Util.prettyPrintHex(rsaPublicKey.getPublicExponent()));
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {

        //From EMV docs:
        //The value of the Issuer Public Key Exponent and the ICC Public Key Exponent is determined by the issuer.
        //The Certification Authority, Issuer, and ICC Public Key Exponents shall be equal to 3 or 2^16 + 1.

        //NOTE: using a low public exponent in RSA, like 3, leaves the system vulnerable to certain types of attack.
        //However, in EMV, these attacks do not apply. (TODO: Verify!)
        
        // Generate a 1152-bit RSA key pair with exponent == 3
        dump(generateRSAKeys(1152, 3));
    }
}
