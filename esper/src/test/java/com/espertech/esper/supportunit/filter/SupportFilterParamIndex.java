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
package com.espertech.esper.supportunit.filter;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.filter.*;
import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;

import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;

public class SupportFilterParamIndex extends FilterParamIndexLookupableBase {
    public SupportFilterParamIndex(ExprFilterSpecLookupable lookupable) {
        super(FilterOperator.EQUAL, lookupable);
    }

    public EventEvaluator get(Object expressionValue) {
        return null;
    }

    public void put(Object expressionValue, EventEvaluator evaluator) {
    }

    public void remove(Object expressionValue) {
    }

    public int sizeExpensive() {
        return 0;
    }

    public boolean isEmpty() {
        return true;
    }

    public ReadWriteLock getReadWriteLock() {
        return null;
    }

    public void matchEvent(EventBean theEvent, Collection<FilterHandle> matches) {
    }
}
