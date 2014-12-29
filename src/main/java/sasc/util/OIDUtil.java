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

/**
 *
 * @author sasc
 */
public class OIDUtil {
    
    public OIDUtil() {
        throw new UnsupportedOperationException("Not allowed to instantiate");
    }
    
    public static String decodeOID(byte[] enc){
        StringBuilder sb = new StringBuilder();
       
        //First OID Component (standard)
        //0: ITU-T
        //1: ISO
        //2: joint-iso-itu-t
        
        //Second OID Component (part in a multi part standard)
        //0: standard
        //1: registration-authority
        //2: member-body
        //3: identified-organization


        long firstSubidentifier = 0;

        int i=0;
        while(Util.isBitSet(enc[i], 8)){
            firstSubidentifier = (firstSubidentifier << 7) | (enc[i] & 0x7f);
            i++;
        }
        firstSubidentifier = (firstSubidentifier << 7) | (enc[i] & 0x7f);
        i++;

        if(firstSubidentifier >= 80){
            long firstOIDComp = 2;
            long secondOIDComp = firstSubidentifier - 80;
            sb.append(firstOIDComp).append(".").append(secondOIDComp);
        }else{
            long secondOIDComp = firstSubidentifier % 40;
            long firstOIDComp = (firstSubidentifier - secondOIDComp)/40;
            sb.append(firstOIDComp).append(".").append(secondOIDComp);
        }
              
        for(; i<enc.length; i++){
            sb.append(".");
            long subIdentifier = 0;
            
            while(Util.isBitSet(enc[i], 8)){
                subIdentifier = (subIdentifier << 7) | (enc[i] & 0x7f);
                i++;
            }
            subIdentifier = (subIdentifier << 7) | (enc[i] & 0x7f);
            sb.append(subIdentifier);
            
        }
        
        String oid = sb.toString();
        String desc = getOIDDescription(oid);
        return oid + ((desc!=null && !desc.isEmpty())?" ("+desc +")":"");
    }
    
    //Simple OID registry
    //See: http://www.oid-info.com/
    public static String getOIDDescription(String oid){

//        1.2.840 - one of 2 US country OIDs 
//        1.2.840.114283 - Global Platform
        
//        1.3.6.1 - the Internet OID 
//        1.3.6.1.4.1 - IANA-assigned company OIDs, used for private MIBs and such things 
//        1.3.6.1.4.1.42 - Sun Microsystems
//        1.3.6.1.4.1.42.2 - Sun Products
//        1.3.6.1.4.1.42.2.110 - java[XML]software
//        1.3.6.1.4.1.42.2.110.1.2 - (Unknown - Java Card?)

        if(oid.startsWith("1.2.840.114283.1")){
            return "Global Platform - Card Recognition Data";
        }
        if(oid.startsWith("1.2.840.114283.2")){
            return "Global Platform v"+oid.substring(17);
        }
        if(oid.startsWith("1.2.840.114283.3")){
            return "Global Platform - Card Identification Scheme";
        }
        if(oid.startsWith("1.2.840.114283.4")){
            return "Global Platform SCP "+oid.substring(17, 18) + " implementation option 0x"+Util.int2Hex(Integer.parseInt(oid.substring(19)));
        }
        if(oid.startsWith("1.2.840.114283")){
            return "Global Platform";
        }
        if(oid.startsWith("1.2.840")){
            return "USA";
        }
        if(oid.startsWith("1.3.6.1.4.1.42.2.110.1.2")){
            return "Sun Microsystems - Java Card ?";
        }
        if(oid.startsWith("1.3.6.1.4.1.42.2")){
            return "Sun Microsystems - Products";
        }
//        if(oid.startsWith("1.3.656.840."))
        //JCOP includes GP refinements according to Visa GP 2.1.1 specification. 
        //This tag is populated accordingly (Visa specific). 
        //The last number tells you what configuration it is (3: SSD + PKI, 2: PKI, 1: just symmetric crypto). 
        //Unfortunately this standard is not open.
        

        return "";
    }
    
    public static void main(String[] args) {        
        System.out.println("1.2.840.114283.1 : " + decodeOID(Util.fromHexString("2a 86 48 86 fc 6b 01")));
        System.out.println("1.2.840.114283.2.2.1.1 : " + decodeOID(Util.fromHexString("2a 86 48 86 fc 6b 02 02 01 01")));
        System.out.println("1.2.840.114283.4.XXXX : " + decodeOID(Util.fromHexString("2a 86 48 86 fc 6b 04 02 15"))); //JCOP 31
        System.out.println("1.2.840.114283.4.XXXX : " + decodeOID(Util.fromHexString("2a 86 48 86 fc 6b 04 01 05"))); //JCOP 31
        
        System.out.println("Sun Microsystems : " + decodeOID(Util.fromHexString("2b 06 01 04 01 2a 02 6e 01 02")));
        System.out.println("Unknown : " + decodeOID(Util.fromHexString("2b 85 10 86 48 64 02 01 03")));
        System.out.println("{2 100 3} : " + decodeOID(Util.fromHexString("813403")));
    }
}
