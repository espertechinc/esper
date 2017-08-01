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
package com.espertech.esper.epl.join.hint;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.annotation.HintEnum;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.event.EventBeanUtility;
import com.espertech.esper.util.AuditPath;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExcludePlanHint {
    private static final Logger QUERY_PLAN_LOG = LoggerFactory.getLogger(AuditPath.QUERYPLAN_LOG);

    private final String[] streamNames;
    private final List<ExprEvaluator> evaluators;
    private final ExprEvaluatorContext exprEvaluatorContext;
    private final boolean queryPlanLogging;

    public ExcludePlanHint(String[] streamNames, List<ExprEvaluator> evaluators, StatementContext statementContext) {
        this.streamNames = streamNames;
        this.evaluators = evaluators;
        this.exprEvaluatorContext = new ExprEvaluatorContextStatement(statementContext, false);
        this.queryPlanLogging = statementContext.getConfigSnapshot().getEngineDefaults().getLogging().isEnableQueryPlan();
    }

    public static ExcludePlanHint getHint(String[] streamNames, StatementContext statementContext)
            throws ExprValidationException {
        List<String> hints = HintEnum.EXCLUDE_PLAN.getHintAssignedValues(statementContext.getAnnotations());
        if (hints == null) {
            return null;
        }
        List<ExprEvaluator> filters = new ArrayList<ExprEvaluator>();
        for (String hint : hints) {
            if (hint.trim().isEmpty()) {
                continue;
            }
            ExprForge forge = ExcludePlanHintExprUtil.toExpression(hint, statementContext);
            if (JavaClassHelper.getBoxedType(forge.getEvaluationType()) != Boolean.class) {
                throw new ExprValidationException("Expression provided for hint " + HintEnum.EXCLUDE_PLAN.getValue() + " must return a boolean value");
            }
            filters.add(forge.getExprEvaluator());
        }
        return new ExcludePlanHint(streamNames, filters, statementContext);
    }

    public boolean filter(int streamLookup, int streamIndexed, ExcludePlanFilterOperatorType opType, ExprNode... exprNodes) {

        EventBean event = ExcludePlanHintExprUtil.toEvent(streamLookup,
                streamIndexed, streamNames[streamLookup], streamNames[streamIndexed],
                opType.name().toLowerCase(Locale.ENGLISH), exprNodes);
        if (queryPlanLogging && QUERY_PLAN_LOG.isInfoEnabled()) {
            QUERY_PLAN_LOG.info("Exclude-plan-hint combination " + EventBeanUtility.printEvent(event));
        }
        EventBean[] eventsPerStream = new EventBean[]{event};

        for (ExprEvaluator evaluator : evaluators) {
            Boolean pass = (Boolean) evaluator.evaluate(eventsPerStream, true, exprEvaluatorContext);
            if (pass != null && pass) {
                if (queryPlanLogging && QUERY_PLAN_LOG.isInfoEnabled()) {
                    QUERY_PLAN_LOG.info("Exclude-plan-hint combination : true");
                }
                return true;
            }
        }
        if (queryPlanLogging && QUERY_PLAN_LOG.isInfoEnabled()) {
            QUERY_PLAN_LOG.info("Exclude-plan-hint combination : false");
        }
        return false;
    }
}
