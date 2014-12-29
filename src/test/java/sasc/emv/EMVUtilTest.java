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

import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import sasc.iso7816.TLVUtil;
import sasc.terminal.CardConnection;
import sasc.terminal.CardResponse;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class EMVUtilTest {

    public EMVUtilTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

//    /**
//     * Test of sendCmd method, of class EMVUtil.
//     */
//    @Test
//    public void testSendCmd() throws Exception {
//        System.out.println("sendCmd");
//        CardConnection terminal = null;
//        String command = "";
//        CardResponse expResult = null;
//        CardResponse result = EMVUtil.sendCmd(terminal, command);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of printResponse method, of class EMVUtil.
//     */
//    @Test
//    public void testPrintResponse() {
//        System.out.println("printResponse");
//        CardResponse response = null;
//        EMVUtil.printResponse(response);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of parseFCIDDF method, of class EMVUtil.
//     */
//    @Test
//    public void testParseFCIDDF() {
//        System.out.println("parseFCIDDF");
//        byte[] data = null;
//        EMVCard card = null;
//        DDF expResult = null;
//        DDF result = EMVUtil.parseFCIDDF(data, card);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of parsePSERecord method, of class EMVUtil.
//     */
//    @Test
//    public void testParsePSERecord() {
//        System.out.println("parsePSERecord");
//        byte[] data = null;
//        EMVCard card = null;
//        List expResult = null;
//        List result = EMVUtil.parsePSERecord(data, card);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of parseFCIADF method, of class EMVUtil.
//     */
//    @Test
//    public void testParseFCIADF() {
//        System.out.println("parseFCIADF");
//        byte[] data = null;
//        Application app = null;
//        ApplicationDefinitionFile expResult = null;
//        ApplicationDefinitionFile result = EMVUtil.parseFCIADF(data, app);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of parseProcessingOpts method, of class EMVUtil.
//     */
//    @Test
//    public void testParseProcessingOpts() {
//        System.out.println("parseProcessingOpts");
//        byte[] data = null;
//        Application app = null;
//        EMVUtil.parseProcessingOpts(data, app);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of parseAppRecord method, of class EMVUtil.
//     */
//    @Test
//    public void testParseAppRecord() {
//        System.out.println("parseAppRecord");
//        byte[] data = null;
//        Application app = null;
//        EMVUtil.parseAppRecord(data, app);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
    /**
     * Test of prettyPrintAPDUResponse method, of class EMVUtil.
     */
    @Test
    public void testPrettyPrintAPDUResponse_byteArr() {
        System.out.println("prettyPrintAPDUResponse");
        byte[] data = Util.fromHexString("70 63 61 13 4f 09 a0 00 00 03 15 10 10 05 28 50"
                + "03 50 49 4e 87 01 01 61 15 4f 07 a0 00 00 00 04"
                + "30 60 50 07 4d 41 45 53 54 52 4f 87 01 02 61 1d"
                + "4f 07 a0 00 00 00 04 80 02 50 0f 53 65 63 75 72"
                + "65 43 6f 64 65 20 41 75 74 68 87 01 00 61 16 4f"
                + "07 a0 00 00 03 15 60 20 50 08 43 68 69 70 6b 6e"
                + "69 70 87 01 00");
        String expResult = "\n"
                + "70 63 -- Record Template (EMV Proprietary)\n"
                + "      61 13 -- Application Template\n"
                + "            4f 09 -- Application Identifier (AID) - card\n"
                + "                  a0 00 00 03 15 10 10 05 28 (BINARY)\n"
                + "            50 03 -- Application Label\n"
                + "                  50 49 4e (=PIN)\n"
                + "            87 01 -- Application Priority Indicator\n"
                + "                  01 (BINARY)\n"
                + "      61 15 -- Application Template\n"
                + "            4f 07 -- Application Identifier (AID) - card\n"
                + "                  a0 00 00 00 04 30 60 (BINARY)\n"
                + "            50 07 -- Application Label\n"
                + "                  4d 41 45 53 54 52 4f (=MAESTRO)\n"
                + "            87 01 -- Application Priority Indicator\n"
                + "                  02 (BINARY)\n"
                + "      61 1d -- Application Template\n"
                + "            4f 07 -- Application Identifier (AID) - card\n"
                + "                  a0 00 00 00 04 80 02 (BINARY)\n"
                + "            50 0f -- Application Label\n"
                + "                  53 65 63 75 72 65 43 6f 64 65 20 41 75 74 68 (=SecureCode Auth)\n"
                + "            87 01 -- Application Priority Indicator\n"
                + "                  00 (BINARY)\n"
                + "      61 16 -- Application Template\n"
                + "            4f 07 -- Application Identifier (AID) - card\n"
                + "                  a0 00 00 03 15 60 20 (BINARY)\n"
                + "            50 08 -- Application Label\n"
                + "                  43 68 69 70 6b 6e 69 70 (=Chipknip)\n"
                + "            87 01 -- Application Priority Indicator\n"
                + "                  00 (BINARY)";
        String result = TLVUtil.prettyPrintAPDUResponse(data);
        assertEquals(expResult, result);
    }
//    /**
//     * Test of prettyPrintAPDUResponse method, of class EMVUtil.
//     */
//    @Test
//    public void testPrettyPrintAPDUResponse_byteArr_int() {
//        System.out.println("prettyPrintAPDUResponse");
//        byte[] data = null;
//        int indentLength = 0;
//        String expResult = "";
//        String result = EMVUtil.prettyPrintAPDUResponse(data, indentLength);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of readTagIdBytes method, of class EMVUtil.
//     */
//    @Test
//    public void testReadTagIdBytes() {
//        System.out.println("readTagIdBytes");
//        ByteArrayInputStream stream = null;
//        byte[] expResult = null;
//        byte[] result = EMVUtil.readTagIdBytes(stream);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of readTagLength method, of class EMVUtil.
//     */
//    @Test
//    public void testReadTagLength() {
//        System.out.println("readTagLength");
//        ByteArrayInputStream stream = null;
//        int expResult = 0;
//        int result = EMVUtil.readTagLength(stream);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getNextTLV method, of class EMVUtil.
//     */
//    @Test
//    public void testGetNextTLV() {
//        System.out.println("getNextTLV");
//        ByteArrayInputStream stream = null;
//        BERTLV expResult = null;
//        BERTLV result = EMVUtil.getNextTLV(stream);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of parseTagAndLength method, of class EMVUtil.
//     */
//    @Test
//    public void testParseTagAndLength() {
//        System.out.println("parseTagAndLength");
//        byte[] data = null;
//        List expResult = null;
//        List result = EMVUtil.parseTagAndLength(data);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
}
