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
public class ApplicationPriorityIndicatorTest {

    public ApplicationPriorityIndicatorTest() {
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
     * Test of mayBeselectedWithoutCardholderConfirmation method, of class ApplicationPriorityIndicator.
     */
    @Test
    public void testMayBeselectedWithoutCardholderConfirmation() {
        System.out.println("mayBeselectedWithoutCardholderConfirmation");
        ApplicationPriorityIndicator instance = null;
        instance = new ApplicationPriorityIndicator((byte)0x83);
        assertFalse("mayBeSelectedWithoutCardHolderConfirmation should be false, but was true", instance.mayBeselectedWithoutCardholderConfirmation());
        instance = new ApplicationPriorityIndicator((byte)0x03);
        assertTrue("mayBeSelectedWithoutCardHolderConfirmation should be true, but was false", instance.mayBeselectedWithoutCardholderConfirmation());
    }

    /**
     * Test of getSelectionPriority method, of class ApplicationPriorityIndicator.
     */
    @Test
    public void testGetSelectionPriority() {
        System.out.println("getSelectionPriority");
        ApplicationPriorityIndicator instance = null;
        instance = new ApplicationPriorityIndicator((byte)0x83);
        int expResult = 3;
        int result = instance.getSelectionPriority();
        assertEquals(expResult, result);

        instance = new ApplicationPriorityIndicator((byte)0x8F);
        assertEquals(15, instance.getSelectionPriority());
    }
}