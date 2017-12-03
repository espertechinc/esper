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
package com.espertech.esper.filterspec;

import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import junit.framework.TestCase;

public class TestFilterSpecParamConstant extends TestCase {
    public void testConstruct() {
        new FilterSpecParamConstant(make("a"), FilterOperator.GREATER, 5);

        try {
            new FilterSpecParamConstant(make("a"), FilterOperator.RANGE_CLOSED, 5);
            assertTrue(false);
        } catch (IllegalArgumentException ex) {
            // Expected exception
        }
    }

    public void testEquals() {
        FilterSpecParam c1 = new FilterSpecParamConstant(make("a"), FilterOperator.GREATER, 5);
        FilterSpecParam c2 = new FilterSpecParamConstant(make("a"), FilterOperator.GREATER, 6);
        FilterSpecParam c3 = new FilterSpecParamConstant(make("b"), FilterOperator.GREATER, 5);
        FilterSpecParam c4 = new FilterSpecParamConstant(make("a"), FilterOperator.EQUAL, 5);
        FilterSpecParam c5 = new FilterSpecParamConstant(make("a"), FilterOperator.GREATER, 5);

        assertFalse(c1.equals(c2));
        assertFalse(c1.equals(c3));
        assertFalse(c1.equals(c4));
        assertTrue(c1.equals(c5));
    }

    private ExprFilterSpecLookupable make(String name) {
        return new ExprFilterSpecLookupable(name, null, null, false);
    }
}
