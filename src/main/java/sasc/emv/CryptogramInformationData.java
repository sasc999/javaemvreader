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

/**
 * Cryptogram Information Data
 * Indicates the type of cryptogram and the actions to be performed by the terminal
 *
 * Length 1
 * Format binary
 *
 * TODO
 *
 * @author sasc
 */
public class CryptogramInformationData {
    private byte cidByte;
    
    public CryptogramInformationData(byte cid){
        this.cidByte = cid;
    }
    
    public String getTEXT(){
        switch(cidByte & 0xC0) {
            case 0x00:
                return "AAC";
            case 0x40:
                return "TC";
            case 0x80:
                return "ARQC";
            default: // 0xC0
                return "RFU";                
        }
    }
}
