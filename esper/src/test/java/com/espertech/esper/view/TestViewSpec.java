/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.view;

import com.espertech.esper.epl.spec.ViewSpec;
import com.espertech.esper.supportunit.view.SupportViewSpecFactory;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class TestViewSpec extends TestCase {
    public void testEquals() throws Exception {
        final Class[] c_0 = new Class[]{String.class};
        final String[] s_0_0 = new String[]{"\"symbol\""};
        final String[] s_0_1 = new String[]{"\"price\""};

        final Class[] c_1 = new Class[]{String.class, Long.class};
        final String[] s_1_0 = new String[]{"\"symbol\"", "1"};
        final String[] s_1_1 = new String[]{"\"price\"", "1"};
        final String[] s_1_2 = new String[]{"\"price\"", "2"};
        final String[] s_1_3 = new String[]{"\"price\"", "1"};

        final Class[] c_2 = new Class[]{Boolean.class, String.class, Long.class};
        final String[] s_2_0 = new String[]{"true", "\"symbol\"", "1"};
        final String[] s_2_1 = new String[]{"true", "\"price\"", "1"};
        final String[] s_2_2 = new String[]{"true", "\"price\"", "2"};
        final String[] s_2_3 = new String[]{"false", "\"price\"", "1"};

        Map<Integer, ViewSpec> specs = new HashMap<Integer, ViewSpec>();
        specs.put(1, SupportViewSpecFactory.makeSpec("ext", "sort", null, null));
        specs.put(2, SupportViewSpecFactory.makeSpec("std", "sum", null, null));
        specs.put(3, SupportViewSpecFactory.makeSpec("ext", "sort", null, null));
        specs.put(4, SupportViewSpecFactory.makeSpec("ext", "sort", c_0, s_0_0));
        specs.put(5, SupportViewSpecFactory.makeSpec("ext", "sort", c_0, s_0_0));
        specs.put(6, SupportViewSpecFactory.makeSpec("ext", "sort", c_0, s_0_1));
        specs.put(7, SupportViewSpecFactory.makeSpec("ext", "sort", c_1, s_1_0));
        specs.put(8, SupportViewSpecFactory.makeSpec("ext", "sort", c_1, s_1_1));
        specs.put(9, SupportViewSpecFactory.makeSpec("ext", "sort", c_1, s_1_2));
        specs.put(10, SupportViewSpecFactory.makeSpec("ext", "sort", c_1, s_1_3));
        specs.put(11, SupportViewSpecFactory.makeSpec("ext", "sort", c_2, s_2_0));
        specs.put(12, SupportViewSpecFactory.makeSpec("ext", "sort", c_2, s_2_1));
        specs.put(13, SupportViewSpecFactory.makeSpec("ext", "sort", c_2, s_2_2));
        specs.put(14, SupportViewSpecFactory.makeSpec("ext", "sort", c_2, s_2_3));

        Map<Integer, Integer> matches = new HashMap<Integer, Integer>();
        matches.put(1, 3);
        matches.put(3, 1);
        matches.put(4, 5);
        matches.put(5, 4);
        matches.put(8, 10);
        matches.put(10, 8);

        // Compare each against each
        for (Map.Entry<Integer, ViewSpec> entryOut : specs.entrySet()) {
            for (Map.Entry<Integer, ViewSpec> entryIn : specs.entrySet()) {
                boolean result = entryOut.getValue().equals(entryIn.getValue());

                if (entryOut == entryIn) {
                    assertTrue(result);
                    continue;
                }


                String message = "Comparing " + entryIn.getKey() + "=" + entryIn.getValue() + "   and   " + entryOut.getKey() + "=" + entryOut.getValue();
                if ((matches.containsKey(entryOut.getKey())) &&
                        (matches.get(entryOut.getKey()) == entryIn.getKey())) {
                    assertTrue(message, result);
                } else {
                    assertFalse(message, result);
                }
            }
        }
    }

}
