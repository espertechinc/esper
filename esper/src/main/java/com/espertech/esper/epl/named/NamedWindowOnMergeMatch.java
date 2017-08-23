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
package com.espertech.esper.epl.named;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.OneEventCollection;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.List;

public class NamedWindowOnMergeMatch {
    private ExprEvaluator optionalCond;
    private List<NamedWindowOnMergeAction> actions;

    public NamedWindowOnMergeMatch(ExprNode optionalCond, List<NamedWindowOnMergeAction> actions, EngineImportService engineImportService, String statementName) {
        this.optionalCond = optionalCond != null ? ExprNodeCompiler.allocateEvaluator(optionalCond.getForge(), engineImportService, this.getClass(), false, statementName) : null;
        this.actions = actions;
    }

    public boolean isApplies(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        if (optionalCond == null) {
            return true;
        }

        Object result = optionalCond.evaluate(eventsPerStream, true, context);
        return result != null && (Boolean) result;
    }

    public void apply(EventBean matchingEvent, EventBean[] eventsPerStream, OneEventCollection newData, OneEventCollection oldData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qInfraMergeWhenThenActions(actions.size());
        }

        int count = -1;
        for (NamedWindowOnMergeAction action : actions) {
            count++;

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qInfraMergeWhenThenActionItem(count, action.getName());
                boolean applies = action.isApplies(eventsPerStream, context);
                if (applies) {
                    action.apply(matchingEvent, eventsPerStream, newData, oldData, context);
                }
                InstrumentationHelper.get().aInfraMergeWhenThenActionItem(applies);
                continue;
            }

            if (action.isApplies(eventsPerStream, context)) {
                action.apply(matchingEvent, eventsPerStream, newData, oldData, context);
            }
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aInfraMergeWhenThenActions();
        }
    }
}