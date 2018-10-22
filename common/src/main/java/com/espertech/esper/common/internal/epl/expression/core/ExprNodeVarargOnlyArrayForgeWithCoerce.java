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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.client.EventBean;

import java.lang.reflect.Array;

class ExprNodeVarargOnlyArrayForgeWithCoerce implements ExprEvaluator {
    private final ExprNodeVarargOnlyArrayForge forge;
    private final ExprEvaluator[] evals;

    public ExprNodeVarargOnlyArrayForgeWithCoerce(ExprNodeVarargOnlyArrayForge forge, ExprEvaluator[] evals) {
        this.forge = forge;
        this.evals = evals;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object array = Array.newInstance(forge.varargClass, evals.length);
        for (int i = 0; i < evals.length; i++) {
            Object value = evals[i].evaluate(eventsPerStream, isNewData, context);
            if (forge.optionalCoercers[i] != null) {
                value = forge.optionalCoercers[i].coerceBoxed((Number) value);
            }
            Array.set(array, i, value);
        }
        return array;
    }
}
