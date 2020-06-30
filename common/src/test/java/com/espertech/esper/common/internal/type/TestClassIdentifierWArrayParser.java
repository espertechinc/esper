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

import com.espertech.esper.common.client.EPException;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collections;

public class TestClassIdentifierWArrayParser extends TestCase {
    public void testParse() {
        assertParse("x[]", "x", 1, false);
        assertParse("x[Primitive]", "x", 1, true);
        assertParse("x", "x", 0, false);
        assertParse("x.y", "x.y", 0, false);
        assertParse("x[][]", "x", 2, false);
        assertParse("x[primitive][]", "x", 2, true);

        assertParse("java.util.List<String>", "java.util.List", 0, false, new ClassDescriptor("String"));
        assertParse("java.util.List< String >", "java.util.List", 0, false, new ClassDescriptor("String"));
        assertParse("List<String, Integer>", "List", 0, false, new ClassDescriptor("String"), new ClassDescriptor("Integer"));
        assertParse("java.util.List<String>[]", "java.util.List", 1, false, new ClassDescriptor("String"));
        assertParse("x<y>[][]", "x", 2, false, new ClassDescriptor("y"));
        assertParse("x<y>[primitive][][]", "x", 3, true, new ClassDescriptor("y"));
        assertParse("x<y[]>", "x", 0, false, new ClassDescriptor("y", Collections.emptyList(), 1, false));
        assertParse("x<y[primitive]>", "x", 0, false, new ClassDescriptor("y", Collections.emptyList(), 1, true));
        assertParse("x<a,b[],c[][]>", "x", 0, false, new ClassDescriptor("a", Collections.emptyList(), 0, false), new ClassDescriptor("b", Collections.emptyList(), 1, false), new ClassDescriptor("c", Collections.emptyList(), 2, false));
        assertParse("x<a<b>>", "x", 0, false, new ClassDescriptor("a", Collections.singletonList(new ClassDescriptor("b")), 0, false));
        assertParse("x<a<b<c>>>", "x", 0, false, new ClassDescriptor("a", Collections.singletonList(new ClassDescriptor("b", Collections.singletonList(new ClassDescriptor("c")), 0, false)), 0, false));

        tryInvalid("x[", "Failed to parse class identifier 'x[': Unexpected token END value '', expecting RIGHT_BRACKET");
        tryInvalid("String[][", "Failed to parse class identifier 'String[][': Unexpected token END value '', expecting RIGHT_BRACKET");
        tryInvalid("", "Failed to parse class identifier '': Empty class identifier");
        tryInvalid("<String", "Failed to parse class identifier '<String': Unexpected token LESSER_THAN value '<', expecting IDENTIFIER");
        tryInvalid("Abc<String", "Failed to parse class identifier 'Abc<String': Unexpected token END value '', expecting GREATER_THAN");
        tryInvalid("Abc<String,", "Failed to parse class identifier 'Abc<String,': Unexpected token END value '', expecting IDENTIFIER");
        tryInvalid("Abc<String,Integer", "Failed to parse class identifier 'Abc<String,Integer': Unexpected token END value '', expecting GREATER_THAN");
        tryInvalid("A<>", "Failed to parse class identifier 'A<>': Unexpected token GREATER_THAN value '>', expecting IDENTIFIER");
    }

    private void tryInvalid(String classIdentifier, String expected) {
        try {
            ClassDescriptor.parseTypeText(classIdentifier);
            fail();
        } catch (EPException ex) {
            assertEquals(ex.getMessage(), expected);
        }
    }

    private void assertParse(String classIdentifier, String name, int dimensions, boolean arrayOfPrimitive, ClassDescriptor... typeParams) {
        ClassDescriptor ident = ClassDescriptor.parseTypeText(classIdentifier);
        ClassDescriptor expected = new ClassDescriptor(name, Arrays.asList(typeParams), dimensions, arrayOfPrimitive);
        assertEquals(expected, ident);
    }
}
