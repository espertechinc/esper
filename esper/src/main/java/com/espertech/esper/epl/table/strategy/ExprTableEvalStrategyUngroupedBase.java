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
package com.espertech.esper.epl.table.strategy;

import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.event.ObjectArrayBackedEventBean;

public abstract class ExprTableEvalStrategyUngroupedBase {

    private final TableAndLockProviderUngrouped provider;

    protected ExprTableEvalStrategyUngroupedBase(TableAndLockProviderUngrouped provider) {
        this.provider = provider;
    }

    protected ObjectArrayBackedEventBean lockTableReadAndGet(ExprEvaluatorContext context) {
        TableAndLockUngrouped pair = provider.get();
        ExprTableEvalLockUtil.obtainLockUnless(pair.getLock(), context);
        return pair.getUngrouped().getEventUngrouped();
    }
}
