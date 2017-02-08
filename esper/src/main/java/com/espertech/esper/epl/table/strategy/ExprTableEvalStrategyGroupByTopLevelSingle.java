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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumn;

import java.util.Map;

public class ExprTableEvalStrategyGroupByTopLevelSingle extends ExprTableEvalStrategyGroupByTopLevelBase {

    private final ExprEvaluator groupExpr;

    public ExprTableEvalStrategyGroupByTopLevelSingle(TableAndLockProviderGrouped provider, Map<String, TableMetadataColumn> items, ExprEvaluator groupExpr) {
        super(provider, items);
        this.groupExpr = groupExpr;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object groupKey = groupExpr.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        return super.evaluateInternal(groupKey, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public Object[] evaluateTypableSingle(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object groupKey = groupExpr.evaluate(eventsPerStream, isNewData, context);
        return super.evaluateTypableSingleInternal(groupKey, eventsPerStream, isNewData, context);
    }
}
