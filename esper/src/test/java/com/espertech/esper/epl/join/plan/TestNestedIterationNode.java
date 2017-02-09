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
package com.espertech.esper.epl.join.plan;

import junit.framework.TestCase;

public class TestNestedIterationNode extends TestCase {
    public void testMakeExec() {
        try {
            new NestedIterationNode(new int[]{});
            fail();
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }
}
