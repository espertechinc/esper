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
package com.espertech.esper.common.internal.filterspec;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.ExprEvaluatorContextWTableAccess;
import com.espertech.esper.common.internal.context.util.StatementContextFilterEvalEnv;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.variable.core.VariableManagementService;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceRuntime;

import java.lang.annotation.Annotation;

public class FilterBooleanExpressionFactoryImpl implements FilterBooleanExpressionFactory {
    public final static FilterBooleanExpressionFactoryImpl INSTANCE = new FilterBooleanExpressionFactoryImpl();

    public ExprNodeAdapterBase make(FilterSpecParamExprNode node, EventBean[] events, ExprEvaluatorContext exprEvaluatorContext, int agentInstanceId, StatementContextFilterEvalEnv filterEvalEnv) {

        // handle table evaluator context
        if (node.isHasTableAccess()) {
            exprEvaluatorContext = new ExprEvaluatorContextWTableAccess(exprEvaluatorContext, filterEvalEnv.getTableExprEvaluatorContext());
        }

        ExprNodeAdapterBase adapter;
        if (events == null) {

            // if a subquery is present in a filter stream acquire the agent instance lock
            if (node.isHasFilterStreamSubquery()) {
                adapter = getLockableSingle(node, exprEvaluatorContext, filterEvalEnv.getVariableManagementService(), filterEvalEnv.getClasspathImportServiceRuntime(), filterEvalEnv.getAnnotations(), agentInstanceId);
            } else if (!node.isHasVariable()) {
                // no-variable no-prior event evaluation
                adapter = new ExprNodeAdapterSSPlain(node, exprEvaluatorContext);
            } else {
                // with-variable no-prior event evaluation
                adapter = new ExprNodeAdapterSSVariables(node, exprEvaluatorContext, filterEvalEnv.getVariableManagementService());
            }
        } else {
            // pattern cases
            VariableManagementService variableServiceToUse = node.isHasVariable() ? filterEvalEnv.getVariableManagementService() : null;
            if (node.isHasFilterStreamSubquery()) {
                adapter = getLockableMultiStream(node, exprEvaluatorContext, variableServiceToUse, filterEvalEnv.getClasspathImportServiceRuntime(), events, filterEvalEnv.getAnnotations(), agentInstanceId);
            } else {
                if (node.isUseLargeThreadingProfile()) {
                    adapter = new ExprNodeAdapterMSNoTL(node, exprEvaluatorContext, events, variableServiceToUse);
                } else {
                    adapter = new ExprNodeAdapterMSPlain(node, exprEvaluatorContext, events, variableServiceToUse);
                }
            }
        }

        if (!node.isHasTableAccess()) {
            return adapter;
        }

        // handle table
        return new ExprNodeAdapterWTableAccess(node, exprEvaluatorContext, adapter, filterEvalEnv.getTableExprEvaluatorContext());
    }

    protected ExprNodeAdapterBase getLockableSingle(FilterSpecParamExprNode factory, ExprEvaluatorContext exprEvaluatorContext, VariableManagementService variableService, ClasspathImportServiceRuntime classpathImportService, Annotation[] annotations, int agentInstanceId) {
        return new ExprNodeAdapterSSStmtLock(factory, exprEvaluatorContext, variableService);
    }

    protected ExprNodeAdapterBase getLockableMultiStream(FilterSpecParamExprNode factory, ExprEvaluatorContext exprEvaluatorContext, VariableManagementService variableServiceToUse, ClasspathImportServiceRuntime classpathImportService, EventBean[] events, Annotation[] annotations, int agentInstanceId) {
        return new ExprNodeAdapterMSStmtLock(factory, exprEvaluatorContext, events, variableServiceToUse);
    }
}
