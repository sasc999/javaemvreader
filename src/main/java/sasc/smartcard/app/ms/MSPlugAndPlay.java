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
package sasc.smartcard.app.ms;

/**
 *
 * @author sasc
 */
public class MSPlugAndPlay {
    //-If MS Plug&Play AID [A000000397 4349445F0100] (->GET DATA command to locate the Windows proprietary tag 0x7F68 (ASN.1 DER encoded).
    //If the smart card supports the GET DATA command, the Windows smart card framework expects the card to return a DER-TLV encoded byte array that is formatted in the following ASN.1 Structure.
    //CardID ::= SEQUENCE {
    //  version Version DEFAULT v1,
    //  vendor VENDOR,
    //                   guids GUIDS }
    //
    //Version ::= INTEGER {v1(0), v2(1), v3(2)}
    //VENDOR ::= IA5STRING(SIZE(0..8))
    //GUID ::= OCTET STRING(SIZE(16))
    //GUIDS ::= SEQUENCE OF GUID
}
