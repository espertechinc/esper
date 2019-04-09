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
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;

public abstract class ExprTableEvalStrategyUngroupedBase implements ExprTableEvalStrategy {

    protected final TableAndLockProviderUngrouped provider;
    protected final ExprTableEvalStrategyFactory factory;

    public ExprTableEvalStrategyUngroupedBase(TableAndLockProviderUngrouped provider, ExprTableEvalStrategyFactory factory) {
        this.provider = provider;
        this.factory = factory;
    }

    protected ObjectArrayBackedEventBean lockTableReadAndGet(ExprEvaluatorContext context) {
        TableAndLockUngrouped pair = provider.get();
        TableEvalLockUtil.obtainLockUnless(pair.getLock(), context);
        return pair.getUngrouped().getEventUngrouped();
    }

    public AggregationRow getAggregationRow(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        ObjectArrayBackedEventBean row = lockTableReadAndGet(context);
        if (row == null) {
            return null;
        }
        return ExprTableEvalStrategyUtil.getRow(row);
    }
}
