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
package com.espertech.esper.common.internal.epl.table.strategy;

import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.table.core.TableEvalLockUtil;
import com.espertech.esper.common.internal.epl.table.core.TableInstanceGrouped;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;

public abstract class ExprTableEvalStrategyGroupedBase implements ExprTableEvalStrategy {

    private final TableAndLockProviderGrouped provider;
    protected final ExprTableEvalStrategyFactory factory;

    public ExprTableEvalStrategyGroupedBase(TableAndLockProviderGrouped provider, ExprTableEvalStrategyFactory factory) {
        this.provider = provider;
        this.factory = factory;
    }

    protected ObjectArrayBackedEventBean lockTableReadAndGet(Object group, ExprEvaluatorContext context) {
        TableAndLockGrouped tableAndLockGrouped = provider.get();
        TableEvalLockUtil.obtainLockUnless(tableAndLockGrouped.getLock(), context);
        return tableAndLockGrouped.getGrouped().getRowForGroupKey(group);
    }

    protected TableInstanceGrouped lockTableRead(ExprEvaluatorContext context) {
        TableAndLockGrouped tableAndLockGrouped = provider.get();
        TableEvalLockUtil.obtainLockUnless(tableAndLockGrouped.getLock(), context);
        return tableAndLockGrouped.getGrouped();
    }
}
