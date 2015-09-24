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
import com.espertech.esper.epl.table.mgmt.TableStateInstanceGrouped;
import com.espertech.esper.event.ObjectArrayBackedEventBean;

import java.util.concurrent.locks.Lock;

public abstract class ExprTableEvalStrategyGroupByBase {

    private final Lock lock;
    protected final TableStateInstanceGrouped grouped;

    protected ExprTableEvalStrategyGroupByBase(Lock lock, TableStateInstanceGrouped grouped) {
        this.lock = lock;
        this.grouped = grouped;
    }

    protected ObjectArrayBackedEventBean lockTableReadAndGet(Object group, ExprEvaluatorContext context) {
        ExprTableEvalLockUtil.obtainLockUnless(lock, context);
        return grouped.getRowForGroupKey(group);
    }

    protected void lockTableRead(ExprEvaluatorContext context) {
        ExprTableEvalLockUtil.obtainLockUnless(lock, context);
    }
}
