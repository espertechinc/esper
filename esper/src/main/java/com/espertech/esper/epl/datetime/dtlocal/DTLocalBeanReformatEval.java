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
package com.espertech.esper.epl.datetime.dtlocal;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

class DTLocalBeanReformatEval implements DTLocalEvaluator {
    private final EventPropertyGetter getter;
    private final DTLocalEvaluator inner;

    public DTLocalBeanReformatEval(EventPropertyGetter getter, DTLocalEvaluator inner) {
        this.getter = getter;
        this.inner = inner;
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object timestamp = getter.get((EventBean) target);
        if (timestamp == null) {
            return null;
        }
        return inner.evaluate(timestamp, eventsPerStream, isNewData, exprEvaluatorContext);
    }
}
