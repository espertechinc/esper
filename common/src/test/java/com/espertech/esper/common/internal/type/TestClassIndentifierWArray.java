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
package com.espertech.esper.common.internal.type;

import junit.framework.TestCase;

public class TestClassIndentifierWArray extends TestCase {
    public void testParse() {
        assertParse("x[]", "x", 1, false);
        assertParse("x[Primitive]", "x", 1, true);
        assertParse("x", "x", 0, false);
        assertParse("x.y", "x.y", 0, false);
        assertParse("x[][]", "x", 2, false);
        assertParse("x[primitive][]", "x", 2, true);
    }

    private void assertParse(String classIdentifier, String name, int dimensions, boolean arrayOfPrimitive) {
        ClassIdentifierWArray ident = ClassIdentifierWArray.parseSODA(classIdentifier);
        assertEquals(name, ident.getClassIdentifier());
        assertEquals(dimensions, ident.getArrayDimensions());
        assertEquals(arrayOfPrimitive, ident.isArrayOfPrimitive());
    }
}
