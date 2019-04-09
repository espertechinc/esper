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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
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

    protected TableInstanceGrouped lockTableRead(ExprEvaluatorContext context) {
        TableAndLockGrouped tableAndLockGrouped = provider.get();
        TableEvalLockUtil.obtainLockUnless(tableAndLockGrouped.getLock(), context);
        return tableAndLockGrouped.getGrouped();
    }

    public AggregationRow getAggregationRow(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        ObjectArrayBackedEventBean row = getRow(eventsPerStream, isNewData, context);
        if (row == null) {
            return null;
        }
        return ExprTableEvalStrategyUtil.getRow(row);
    }

    protected ObjectArrayBackedEventBean getRow(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object groupKey = factory.getGroupKeyEval().evaluate(eventsPerStream, isNewData, context);
        TableAndLockGrouped tableAndLockGrouped = provider.get();
        TableEvalLockUtil.obtainLockUnless(tableAndLockGrouped.getLock(), context);
        if (groupKey instanceof Object[]) {
            groupKey = tableAndLockGrouped.getGrouped().getTable().getPrimaryKeyObjectArrayTransform().from((Object[]) groupKey);
        }
        return tableAndLockGrouped.getGrouped().getRowForGroupKey(groupKey);
    }
}
