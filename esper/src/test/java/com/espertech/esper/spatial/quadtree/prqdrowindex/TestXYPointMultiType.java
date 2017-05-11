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
package com.espertech.esper.spatial.quadtree.prqdrowindex;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.StringJoiner;

public class TestXYPointMultiType extends TestCase {

    public void testCollectInto() {
        Collection<Object> values = new ArrayList<>();

        XYPointMultiType v = new XYPointMultiType(10, 20, "X");
        v.collectInto(values);
        assertEquals("X", join(values));

        values.clear();
        v.addSingleValue("Y");
        v.collectInto(values);
        assertEquals("X,Y", join(values));
    }

    public void testAddSingleValue() {
        XYPointMultiType v = new XYPointMultiType(10, 20, "X");
        assertValues("X", v);

        v.addSingleValue("Y");
        assertValues("X,Y", v);

        v.addSingleValue("Z");
        assertValues("X,Y,Z", v);
    }

    public void testAddMultiType() {
        XYPointMultiType vOne = new XYPointMultiType(10, 20, "X");
        XYPointMultiType vTwo = new XYPointMultiType(10, 20, "Y");
        vOne.addMultiType(vTwo);
        assertValues("X,Y", vOne);
        assertValues("Y", vTwo);

        XYPointMultiType vThree = new XYPointMultiType(10, 20, "1");
        vThree.addSingleValue("2");
        vOne.addMultiType(vThree);
        assertValues("X,Y,1,2", vOne);
        assertValues("1,2", vThree);

        XYPointMultiType vFour = new XYPointMultiType(10, 20, "X");
        vFour.addSingleValue("1");
        vFour.addMultiType(vTwo);
        assertValues("X,1,Y", vFour);

        XYPointMultiType vFive = new XYPointMultiType(10, 20, "A");
        vFive.addSingleValue("B");
        vFive.addMultiType(vThree);
        assertValues("A,B,1,2", vFive);
        vFive.addSingleValue("C");
        assertValues("A,B,1,2,C", vFive);
    }

    public void testInvalidMerge() {
        XYPointMultiType vOne = new XYPointMultiType(10, 20, "X");
        try {
            vOne.addMultiType(new XYPointMultiType(5, 20, "Y"));
            fail();
        }
        catch (IllegalArgumentException ex) {
            // expected
        }
        try {
            vOne.addMultiType(new XYPointMultiType(10, 19, "Y"));
            fail();
        }
        catch (IllegalArgumentException ex) {
            // expected
        }
    }

    private void assertValues(String expected, XYPointMultiType v) {
        String received = v.getMultityped() instanceof Collection ? join((Collection) v.getMultityped()) : v.getMultityped().toString();
        assertEquals(expected, received);
        assertEquals(v.count(), expected.split(",").length);
    }

    private String join(Collection collection) {
        StringJoiner joiner = new StringJoiner(",");
        for (Object value : collection) {
            joiner.add(value.toString());
        }
        return joiner.toString();
    }
}
