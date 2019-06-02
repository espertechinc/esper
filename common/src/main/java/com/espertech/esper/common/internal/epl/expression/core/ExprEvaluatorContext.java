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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.expr.EventBeanService;
import com.espertech.esper.common.internal.context.util.StatementAgentInstanceLock;
import com.espertech.esper.common.internal.epl.enummethod.cache.ExpressionResultCacheService;
import com.espertech.esper.common.internal.epl.script.core.AgentInstanceScriptContext;
import com.espertech.esper.common.internal.epl.table.core.TableExprEvaluatorContext;
import com.espertech.esper.common.internal.metrics.audit.AuditProvider;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;
import com.espertech.esper.common.internal.schedule.TimeProvider;
import com.espertech.esper.common.internal.settings.ExceptionHandlingService;

/**
 * Returns the context for expression evaluation.
 */
public interface ExprEvaluatorContext {
    String getStatementName();

    Object getUserObjectCompileTime();

    String getRuntimeURI();

    int getStatementId();

    EventBean getContextProperties();

    int getAgentInstanceId();

    EventBeanService getEventBeanService();

    TimeProvider getTimeProvider();

    StatementAgentInstanceLock getAgentInstanceLock();

    ExpressionResultCacheService getExpressionResultCacheService();

    TableExprEvaluatorContext getTableExprEvaluatorContext();

    AgentInstanceScriptContext getAllocateAgentInstanceScriptContext();

    String getDeploymentId();

    AuditProvider getAuditProvider();

    InstrumentationCommon getInstrumentationProvider();

    ExceptionHandlingService getExceptionHandlingService();
}