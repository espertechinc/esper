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
package com.espertech.esper.runtime.internal.support;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEventEvaluator;

public class SupportExprEventEvaluator implements ExprEventEvaluator {
    private final EventPropertyValueGetter getter;

    public SupportExprEventEvaluator(EventPropertyValueGetter getter) {
        this.getter = getter;
    }

    public Object eval(EventBean event, ExprEvaluatorContext ctx) {
        return getter.get(event);
    }
}
