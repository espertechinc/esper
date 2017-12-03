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
package com.espertech.esper.filter;

import com.espertech.esper.client.EventType;
import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.event.SupportEventTypeFactory;
import junit.framework.TestCase;

public class TestIndexFactory extends TestCase {
    private EventType eventType;
    private FilterServiceGranularLockFactory lockFactory = new FilterServiceGranularLockFactoryReentrant();

    public void setUp() {
        eventType = SupportEventTypeFactory.createBeanType(SupportBean.class);
    }

    public void testCreateIndex() {
        // Create a "greater" index
        FilterParamIndexBase index = IndexFactory.createIndex(makeLookupable("intPrimitive"), lockFactory, FilterOperator.GREATER);

        assertTrue(index != null);
        assertTrue(index instanceof FilterParamIndexCompare);
        assertTrue(getPropName(index).equals("intPrimitive"));
        assertTrue(index.getFilterOperator() == FilterOperator.GREATER);

        // Create an "equals" index
        index = IndexFactory.createIndex(makeLookupable("string"), lockFactory, FilterOperator.EQUAL);

        assertTrue(index != null);
        assertTrue(index instanceof FilterParamIndexEquals);
        assertTrue(getPropName(index).equals("string"));
        assertTrue(index.getFilterOperator() == FilterOperator.EQUAL);

        // Create an "not equals" index
        index = IndexFactory.createIndex(makeLookupable("string"), lockFactory, FilterOperator.NOT_EQUAL);

        assertTrue(index != null);
        assertTrue(index instanceof FilterParamIndexNotEquals);
        assertTrue(getPropName(index).equals("string"));
        assertTrue(index.getFilterOperator() == FilterOperator.NOT_EQUAL);

        // Create a range index
        index = IndexFactory.createIndex(makeLookupable("doubleBoxed"), lockFactory, FilterOperator.RANGE_CLOSED);
        assertTrue(index instanceof FilterParamIndexDoubleRange);
        index = IndexFactory.createIndex(makeLookupable("doubleBoxed"), lockFactory, FilterOperator.NOT_RANGE_CLOSED);
        assertTrue(index instanceof FilterParamIndexDoubleRangeInverted);

        // Create a in-index
        index = IndexFactory.createIndex(makeLookupable("doubleBoxed"), lockFactory, FilterOperator.IN_LIST_OF_VALUES);
        assertTrue(index instanceof FilterParamIndexIn);
        index = IndexFactory.createIndex(makeLookupable("doubleBoxed"), lockFactory, FilterOperator.NOT_IN_LIST_OF_VALUES);
        assertTrue(index instanceof FilterParamIndexNotIn);

        // Create a boolean-expression-index
        index = IndexFactory.createIndex(makeLookupable("boolean"), lockFactory, FilterOperator.BOOLEAN_EXPRESSION);
        assertTrue(index instanceof FilterParamIndexBooleanExpr);
        index = IndexFactory.createIndex(makeLookupable("boolean"), lockFactory, FilterOperator.BOOLEAN_EXPRESSION);
        assertTrue(index instanceof FilterParamIndexBooleanExpr);
    }

    private String getPropName(FilterParamIndexBase index) {
        FilterParamIndexLookupableBase propIndex = (FilterParamIndexLookupableBase) index;
        return propIndex.getLookupable().getExpression();
    }

    private ExprFilterSpecLookupable makeLookupable(String fieldName) {
        return new ExprFilterSpecLookupable(fieldName, eventType.getGetter(fieldName), eventType.getPropertyType(fieldName), false);
    }
}

