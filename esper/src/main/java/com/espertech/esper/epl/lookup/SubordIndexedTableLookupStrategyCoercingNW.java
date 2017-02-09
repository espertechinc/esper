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
import com.espertech.esper.epl.join.table.PropertyIndexedEventTable;
import com.espertech.esper.util.JavaClassHelper;

/**
 * Index lookup strategy that coerces the key values before performing a lookup.
 */
public class SubordIndexedTableLookupStrategyCoercingNW extends SubordIndexedTableLookupStrategyExprNW {
    private Class[] coercionTypes;

    public SubordIndexedTableLookupStrategyCoercingNW(ExprEvaluator[] evaluators, PropertyIndexedEventTable index, Class[] coercionTypes, LookupStrategyDesc strategyDesc) {
        super(evaluators, index, strategyDesc);
        this.coercionTypes = coercionTypes;
    }

    protected Object[] getKeys(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        Object[] keys = super.getKeys(eventsPerStream, context);
        for (int i = 0; i < keys.length; i++) {
            Object value = keys[i];

            Class coercionType = coercionTypes[i];
            if ((value != null) && (!value.getClass().equals(coercionType))) {
                if (value instanceof Number) {
                    value = JavaClassHelper.coerceBoxed((Number) value, coercionTypes[i]);
                }
                keys[i] = value;
            }
        }
        return keys;
    }
}
