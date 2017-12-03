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
import com.espertech.esper.collection.Pair;
import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.filterspec.FilterValueSetParam;
import com.espertech.esper.filterspec.FilterValueSetParamImpl;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.event.SupportEventTypeFactory;
import junit.framework.TestCase;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;

public class TestIndexHelper extends TestCase {
    private EventType eventType;
    private ArrayDeque<FilterValueSetParam> parameters;
    private FilterValueSetParam parameterOne;
    private FilterValueSetParam parameterTwo;
    private FilterValueSetParam parameterThree;
    private FilterServiceGranularLockFactory lockFactory = new FilterServiceGranularLockFactoryReentrant();

    public void setUp() {
        eventType = SupportEventTypeFactory.createBeanType(SupportBean.class);
        parameters = new ArrayDeque<FilterValueSetParam>();

        // Create parameter test list
        parameterOne = new FilterValueSetParamImpl(makeLookupable("intPrimitive"), FilterOperator.GREATER, 10);
        parameters.add(parameterOne);
        parameterTwo = new FilterValueSetParamImpl(makeLookupable("doubleBoxed"), FilterOperator.GREATER, 20d);
        parameters.add(parameterTwo);
        parameterThree = new FilterValueSetParamImpl(makeLookupable("string"), FilterOperator.EQUAL, "sometext");
        parameters.add(parameterThree);
    }

    public void testFindIndex() {
        List<FilterParamIndexBase> indexes = new LinkedList<FilterParamIndexBase>();

        // Create index list wity index that doesn't match
        FilterParamIndexBase indexOne = IndexFactory.createIndex(makeLookupable("boolPrimitive"), lockFactory, FilterOperator.EQUAL);
        indexes.add(indexOne);
        assertTrue(IndexHelper.findIndex(parameters, indexes) == null);

        // Create index list wity index that doesn't match
        indexOne = IndexFactory.createIndex(makeLookupable("doubleBoxed"), lockFactory, FilterOperator.GREATER_OR_EQUAL);
        indexes.clear();
        indexes.add(indexOne);
        assertTrue(IndexHelper.findIndex(parameters, indexes) == null);

        // Add an index that does match a parameter
        FilterParamIndexBase indexTwo = IndexFactory.createIndex(makeLookupable("doubleBoxed"), lockFactory, FilterOperator.GREATER);
        indexes.add(indexTwo);
        Pair<FilterValueSetParam, FilterParamIndexBase> pair = IndexHelper.findIndex(parameters, indexes);
        assertTrue(pair != null);
        assertEquals(parameterTwo, pair.getFirst());
        assertEquals(indexTwo, pair.getSecond());

        // Add another index that does match a parameter, should return first match however which is doubleBoxed
        FilterParamIndexBase indexThree = IndexFactory.createIndex(makeLookupable("intPrimitive"), lockFactory, FilterOperator.GREATER);
        indexes.add(indexThree);
        pair = IndexHelper.findIndex(parameters, indexes);
        assertEquals(parameterOne, pair.getFirst());
        assertEquals(indexThree, pair.getSecond());

        // Try again removing one index
        indexes.remove(indexTwo);
        pair = IndexHelper.findIndex(parameters, indexes);
        assertEquals(parameterOne, pair.getFirst());
        assertEquals(indexThree, pair.getSecond());
    }

    public void testFindParameter() {
        FilterParamIndexBase indexOne = IndexFactory.createIndex(makeLookupable("boolPrimitive"), lockFactory, FilterOperator.EQUAL);
        assertNull(IndexHelper.findParameter(parameters, indexOne));

        FilterParamIndexBase indexTwo = IndexFactory.createIndex(makeLookupable("string"), lockFactory, FilterOperator.EQUAL);
        assertEquals(parameterThree, IndexHelper.findParameter(parameters, indexTwo));

        FilterParamIndexBase indexThree = IndexFactory.createIndex(makeLookupable("intPrimitive"), lockFactory, FilterOperator.GREATER);
        assertEquals(parameterOne, IndexHelper.findParameter(parameters, indexThree));
    }

    private ExprFilterSpecLookupable makeLookupable(String fieldName) {
        return new ExprFilterSpecLookupable(fieldName, eventType.getGetter(fieldName), eventType.getPropertyType(fieldName), false);
    }
}
