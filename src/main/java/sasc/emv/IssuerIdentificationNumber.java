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

import java.util.Arrays;
import sasc.util.Util;

/**
 *
 * A 6 digit number that identifies the major industry and the card issuer 
 * and that forms the first part of the Primary Account Number (PAN)
 * 
 * @author sasc
 */
public class IssuerIdentificationNumber {
    
    byte[] iinBytes;
    
    public IssuerIdentificationNumber(byte[] iinBytes) {
        if(iinBytes == null) {
            throw new NullPointerException("Param iinBytes cannot be null");
        }
        if(iinBytes.length != 3){
            throw new IllegalArgumentException("Param iinBytes must have length 3, but was "+iinBytes.length);
        }
        this.iinBytes = Arrays.copyOf(iinBytes, iinBytes.length);
        
    }
    
    public IssuerIdentificationNumber(int iin) {
        if(iin < 0 || iin > 1000000) {
            throw new IllegalArgumentException("IIN must be between 0 and 999999, but was "+iin);
        }
        byte[] tmp = Util.intToBinaryEncodedDecimalByteArray(iin);
        if(tmp.length != 6){
            iinBytes = Util.resizeArray(tmp, 6);
        } else {
            iinBytes = tmp;
        }
    }
    
    public int getValue(){
        return Util.binaryHexCodedDecimalToInt(iinBytes);
    }
    
    public byte[] getBytes(){
        return Arrays.copyOf(iinBytes, iinBytes.length);
    }
    
    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof IssuerIdentificationNumber)){
            return false;
        }
        IssuerIdentificationNumber that = (IssuerIdentificationNumber)obj;
        if(this == that){
            return true;
        }
        if(Arrays.equals(this.getBytes(), that.getBytes())){
            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Arrays.hashCode(this.iinBytes);
        return hash;
    }
}
