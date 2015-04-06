/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.table.strategy;

import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.ObjectArrayBackedEventBean;

import java.util.Map;
import java.util.concurrent.locks.Lock;

public abstract class ExprTableEvalStrategyGroupByBase {

    private final Lock lock;
    protected final Map<Object, ObjectArrayBackedEventBean> aggregationState;

    protected ExprTableEvalStrategyGroupByBase(Lock lock, Map<Object, ObjectArrayBackedEventBean> aggregationState) {
        this.lock = lock;
        this.aggregationState = aggregationState;
    }

    protected ObjectArrayBackedEventBean lockTableReadAndGet(Object group, ExprEvaluatorContext context) {
        ExprTableEvalLockUtil.obtainLockUnless(lock, context);
        return aggregationState.get(group);
    }

    protected void lockTableRead(ExprEvaluatorContext context) {
        ExprTableEvalLockUtil.obtainLockUnless(lock, context);
    }
}
