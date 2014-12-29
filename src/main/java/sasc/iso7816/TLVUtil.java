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
package sasc.iso7816;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import sasc.emv.EMVApplication;
import sasc.emv.EMVTags;
import static sasc.iso7816.TagValueType.BINARY;
import static sasc.iso7816.TagValueType.DOL;
import static sasc.iso7816.TagValueType.MIXED;
import static sasc.iso7816.TagValueType.NUMERIC;
import static sasc.iso7816.TagValueType.TEXT;
import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class TLVUtil {
    
    private static Tag searchTagById(byte[] tagIdBytes) {
        return EMVTags.getNotNull(tagIdBytes); //TODO take app (IIN or RID) into consideration
    }
    
    private static Tag searchTagById(ByteArrayInputStream stream){
        return searchTagById(TLVUtil.readTagIdBytes(stream));
    }
    
    //This is just a list of Tag And Lengths (eg DOLs)
    public static String getFormattedTagAndLength(byte[] data, int indentLength) {
        StringBuilder buf = new StringBuilder();
        String indent = Util.getSpaces(indentLength);
        ByteArrayInputStream stream = new ByteArrayInputStream(data);

        boolean firstLine = true;
        while (stream.available() > 0) {
            if (firstLine) {
                firstLine = false;
            } else {
                buf.append("\n");
            }
            buf.append(indent);

            Tag tag = searchTagById(stream);
            int length = TLVUtil.readTagLength(stream);

            buf.append(Util.prettyPrintHex(tag.getTagBytes()));
            buf.append(" ");
            buf.append(Util.byteArrayToHexString(Util.intToByteArray(length)));
            buf.append(" -- ");
            buf.append(tag.getName());
        }
        return buf.toString();
    }

    public static byte[] readTagIdBytes(ByteArrayInputStream stream) {
        ByteArrayOutputStream tagBAOS = new ByteArrayOutputStream();
        byte tagFirstOctet = (byte) stream.read();
        tagBAOS.write(tagFirstOctet);

        //Find TAG bytes
        byte MASK = (byte) 0x1F;
        if ((tagFirstOctet & MASK) == MASK) { // EMV book 3, Page 178 or Annex B1 (EMV4.3)
            //Tag field is longer than 1 byte
            do {
                int nextOctet = stream.read();
                if(nextOctet < 0){
                    break;
                }
                byte tlvIdNextOctet = (byte) nextOctet;

                tagBAOS.write(tlvIdNextOctet);

                if (!Util.isBitSet(tlvIdNextOctet, 8) || (Util.isBitSet(tlvIdNextOctet, 8) && (tlvIdNextOctet & 0x7f) == 0) ) {
                    break;
                }
            } while (true);
        }
        return tagBAOS.toByteArray();
    }

    public static int readTagLength(ByteArrayInputStream stream) {
        //Find LENGTH bytes
        int length;
        int tmpLength = stream.read();

        if(tmpLength < 0) {
            throw new TLVException("Negative length: "+tmpLength);
        }

        if (tmpLength <= 127) { // 0111 1111
            // short length form
            length = tmpLength;
        } else if (tmpLength == 128) { // 1000 0000
            // length identifies indefinite form, will be set later
            // indefinite form is not specified in ISO7816-4, but we include it here for completeness
            length = tmpLength;
        } else {
            // long length form
            int numberOfLengthOctets = tmpLength & 127; // turn off 8th bit
            tmpLength = 0;
            for (int i = 0; i < numberOfLengthOctets; i++) {
                int nextLengthOctet = stream.read();
                if(nextLengthOctet < 0){
                    throw new TLVException("EOS when reading length bytes");
                }
                tmpLength <<= 8;
                tmpLength |= nextLengthOctet;
            }
            length = tmpLength;
        }
        return length;
    }
    
    public static BERTLV getNextTLV(ByteArrayInputStream stream) {
        if (stream.available() < 2) {
            throw new TLVException("Error parsing data. Available bytes < 2 . Length=" + stream.available());
        }


        //ISO/IEC 7816 uses neither '00' nor 'FF' as tag value.
        //Before, between, or after TLV-coded data objects,
        //'00' or 'FF' bytes without any meaning may occur
        //(for example, due to erased or modified TLV-coded data objects).

        stream.mark(0);
        int peekInt = stream.read();
        byte peekByte = (byte) peekInt;
        //peekInt == 0xffffffff indicates EOS
        while (peekInt != -1 && (peekByte == (byte) 0xFF || peekByte == (byte) 0x00)) {
            stream.mark(0); //Current position
            peekInt = stream.read();
            peekByte = (byte) peekInt;
        }
        stream.reset(); //Reset back to the last known position without 0x00 or 0xFF

        if (stream.available() < 2) {
            throw new TLVException("Error parsing data. Available bytes < 2 . Length=" + stream.available());
        }

        byte[] tagIdBytes = TLVUtil.readTagIdBytes(stream);

        //We need to get the raw length bytes.
        //Use quick and dirty workaround
        stream.mark(0);
        int posBefore = stream.available();
        //Now parse the lengthbyte(s)
        //This method will read all length bytes. We can then find out how many bytes was read.
        int length = TLVUtil.readTagLength(stream); //Decoded
        //Now find the raw (encoded) length bytes
        int posAfter = stream.available();
        stream.reset();
        byte[] lengthBytes = new byte[posBefore - posAfter];

        if(lengthBytes.length < 1 || lengthBytes.length > 4){
            throw new TLVException("Number of length bytes must be from 1 to 4. Found "+lengthBytes.length);
        }

        stream.read(lengthBytes, 0, lengthBytes.length);

        int rawLength = Util.byteArrayToInt(lengthBytes);

        byte[] valueBytes;

        Tag tag = searchTagById(tagIdBytes);

        // Find VALUE bytes
        if (rawLength == 128) { // 1000 0000
            // indefinite form
            stream.mark(0);
            int prevOctet = 1;
            int curOctet;
            int len = 0;
            while (true) {
                len++;
                curOctet = stream.read();
                if (curOctet < 0) {
                    throw new TLVException("Error parsing data. TLV "
                            + "length byte indicated indefinite length, but EOS "
                            + "was reached before 0x0000 was found" + stream.available());
                }
                if (prevOctet == 0 && curOctet == 0) {
                    break;
                }
                prevOctet = curOctet;
            }
            len -= 2;
            valueBytes = new byte[len];
            stream.reset();
            stream.read(valueBytes, 0, len);
            length = len;
        } else {
            if(stream.available() < length){
                throw new TLVException("Length byte(s) indicated "+length+" value bytes, but only "+stream.available()+ " " +(stream.available()>1?"are":"is")+" available");
            }
            // definite form
            valueBytes = new byte[length];
            stream.read(valueBytes, 0, length);
        }

        //Remove any trailing 0x00 and 0xFF
        stream.mark(0);
        peekInt = stream.read();
        peekByte = (byte) peekInt;
        while (peekInt != -1 && (peekByte == (byte) 0xFF || peekByte == (byte) 0x00)) {
            stream.mark(0);
            peekInt = stream.read();
            peekByte = (byte) peekInt;
        }
        stream.reset(); //Reset back to the last known position without 0x00 or 0xFF


        BERTLV tlv = new BERTLV(tag, length, lengthBytes, valueBytes);
        return tlv;
    }

    private static String getTagValueAsString(Tag tag, byte[] value) {
        StringBuilder buf = new StringBuilder();

        switch (tag.getTagValueType()) {
            case TEXT:
                buf.append("=");
                buf.append(new String(value));
                break;
            case NUMERIC:
                buf.append("NUMERIC");
                break;
            case BINARY:
                buf.append("BINARY");
                break;
            case MIXED:
                buf.append("=");
                buf.append(Util.getSafePrintChars(value));
                break;
            case DOL:
                buf.append("");
                break;
        }

        return buf.toString();
    }

    public static List<TagAndLength> parseTagAndLength(byte[] data) {
        ByteArrayInputStream stream = new ByteArrayInputStream(data);
        List<TagAndLength> tagAndLengthList = new ArrayList<TagAndLength>();

        while (stream.available() > 0) {
            if (stream.available() < 2) {
                throw new SmartCardException("Data length < 2 : " + stream.available());
            }
            byte[] tagIdBytes = TLVUtil.readTagIdBytes(stream);
            int tagValueLength = TLVUtil.readTagLength(stream);

            Tag tag = searchTagById(tagIdBytes);

            tagAndLengthList.add(new TagAndLength(tag, tagValueLength));
        }
        return tagAndLengthList;
    }

    public static String prettyPrintAPDUResponse(byte[] data) {
        return prettyPrintAPDUResponse(data, 0);
    }

    public static String prettyPrintAPDUResponse(byte[] data, int startPos, int length) {
        byte[] tmp = new byte[length-startPos];
        System.arraycopy(data, startPos, tmp, 0, length);
        return prettyPrintAPDUResponse(tmp, 0);
    }

    public static String prettyPrintAPDUResponse(byte[] data, int indentLength) {
        StringBuilder buf = new StringBuilder();

        ByteArrayInputStream stream = new ByteArrayInputStream(data);

        while (stream.available() > 0) {
            buf.append("\n");

            buf.append(Util.getSpaces(indentLength));

            BERTLV tlv = TLVUtil.getNextTLV(stream);

            Log.debug(tlv.toString());

            byte[] tagBytes = tlv.getTagBytes();
            byte[] lengthBytes = tlv.getRawEncodedLengthBytes();
            byte[] valueBytes = tlv.getValueBytes();

            Tag tag = tlv.getTag();

            buf.append(Util.prettyPrintHex(tagBytes));
            buf.append(" ");
            buf.append(Util.prettyPrintHex(lengthBytes));
            buf.append(" -- ");
            buf.append(tag.getName());

            int extraIndent = (lengthBytes.length * 3) + (tagBytes.length * 3);

            if (tag.isConstructed()) {
                //indentLength += extraIndent; //TODO check this
                //Recursion
                buf.append(prettyPrintAPDUResponse(valueBytes, indentLength + extraIndent));
            } else {
                buf.append("\n");
                if (tag.getTagValueType() == TagValueType.DOL) {
                    buf.append(TLVUtil.getFormattedTagAndLength(valueBytes, indentLength + extraIndent));
                } else {
                    buf.append(Util.getSpaces(indentLength + extraIndent));
                    buf.append(Util.prettyPrintHex(Util.byteArrayToHexString(valueBytes), indentLength + extraIndent));
                    buf.append(" (");
                    buf.append(TLVUtil.getTagValueAsString(tag, valueBytes));
                    buf.append(")");
                }
            }
        }
        return buf.toString();
    }
}
