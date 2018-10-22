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
package com.espertech.esper.common.internal.epl.table.core;

import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.util.concurrent.locks.Lock;

public class TableEvalLockUtil {

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param lock                 lock
     * @param exprEvaluatorContext ctx
     */
    public static void obtainLockUnless(Lock lock, ExprEvaluatorContext exprEvaluatorContext) {
        obtainLockUnless(lock, exprEvaluatorContext.getTableExprEvaluatorContext());
    }

    public static void obtainLockUnless(Lock lock, TableExprEvaluatorContext tableExprEvaluatorContext) {
        boolean added = tableExprEvaluatorContext.addAcquiredLock(lock);
        if (added) {
            lock.lock();
        }
    }
}
