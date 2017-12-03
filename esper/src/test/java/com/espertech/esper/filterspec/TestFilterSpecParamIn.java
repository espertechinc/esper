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
import com.espertech.esper.filter.FilterForEvalConstantAnyType;
import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;

public class TestFilterSpecParamIn extends TestCase {
    private FilterSpecParamIn values;

    public void testEquals() {
        values = new FilterSpecParamIn(makeLookupable("a"), FilterOperator.IN_LIST_OF_VALUES, getList(new Object[]{"A", "B"}));
        FilterSpecParamIn values2 = new FilterSpecParamIn(makeLookupable("a"), FilterOperator.IN_LIST_OF_VALUES, getList(new Object[]{"A"}));
        FilterSpecParamIn values3 = new FilterSpecParamIn(makeLookupable("a"), FilterOperator.IN_LIST_OF_VALUES, getList(new Object[]{"A", "B"}));
        FilterSpecParamIn values4 = new FilterSpecParamIn(makeLookupable("a"), FilterOperator.IN_LIST_OF_VALUES, getList(new Object[]{"A", "C"}));

        assertFalse(values.equals(new FilterSpecParamConstant(makeLookupable("a"), FilterOperator.EQUAL, "a")));
        assertFalse(values.equals(values2));
        assertTrue(values.equals(values3));
        assertFalse(values.equals(values4));
    }

    private List<FilterSpecParamInValue> getList(Object[] keys) {
        List<FilterSpecParamInValue> list = new LinkedList<FilterSpecParamInValue>();
        for (int i = 0; i < keys.length; i++) {
            list.add(new FilterForEvalConstantAnyType(keys[i]));
        }
        return list;
    }

    private ExprFilterSpecLookupable makeLookupable(String fieldName) {
        return new ExprFilterSpecLookupable(fieldName, null, null, false);
    }
}
