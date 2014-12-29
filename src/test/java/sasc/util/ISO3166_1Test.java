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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author sasc
 */
public class ISO3166_1Test {

    public ISO3166_1Test() {
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

    /**
     * Test of getCountryForCode method, of class ISO3166_1.
     */
    @Test
    public void testGetCountryForCode_int() {
        System.out.println("getCountryForCode");
        int code = 826;
        String expResult = "United Kingdom";
        String result = ISO3166_1.getCountryForCode(code);
        assertEquals(expResult, result);
    }

    /**
     * Test of getCountryForCode method, of class ISO3166_1.
     */
    @Test
    public void testGetCountryForCode_String() {
        System.out.println("getCountryForCode");
        String code = "334";
        String expResult = "Heard Island and McDonald Islands";
        String result = ISO3166_1.getCountryForCode(code);
        assertEquals(expResult, result);
    }
}