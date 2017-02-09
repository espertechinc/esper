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
package com.espertech.esper.epl.lookup;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTableSingle;
import com.espertech.esper.event.EventBeanUtility;

/**
 * Index lookup strategy that coerces the key values before performing a lookup.
 */
public class SubordIndexedTableLookupStrategySingleCoercingNW extends SubordIndexedTableLookupStrategySingleExprNW {
    private Class coercionType;

    public SubordIndexedTableLookupStrategySingleCoercingNW(ExprEvaluator evaluator, PropertyIndexedEventTableSingle index, Class coercionType, LookupStrategyDesc strategyDesc) {
        super(evaluator, index, strategyDesc);
        this.coercionType = coercionType;
    }

    @Override
    protected Object getKey(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        Object key = super.getKey(eventsPerStream, context);
        return EventBeanUtility.coerce(key, coercionType);
    }
}
