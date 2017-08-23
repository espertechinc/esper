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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.core.context.util.ContextDescriptor;
import com.espertech.esper.core.service.StatementExtensionSvcContext;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.core.viewres.ViewResourceDelegateUnverified;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.schedule.TimeProvider;

import java.lang.annotation.Annotation;

public class ExprValidationContext {
    private final StreamTypeService streamTypeService;
    private final EngineImportService engineImportService;
    private final StatementExtensionSvcContext statementExtensionSvcContext;
    private final ViewResourceDelegateUnverified viewResourceDelegate;
    private final TimeProvider timeProvider;
    private final VariableService variableService;
    private final TableService tableService;
    private final ExprEvaluatorContext exprEvaluatorContext;
    private final EventAdapterService eventAdapterService;
    private final String statementName;
    private final int statementId;
    private final Annotation[] annotations;
    private final ContextDescriptor contextDescriptor;
    private final boolean disablePropertyExpressionEventCollCache;
    private final boolean allowRollupFunctions;
    private final boolean allowBindingConsumption;
    private final boolean isResettingAggregations;
    private final boolean isExpressionNestedAudit;
    private final boolean isExpressionAudit;
    private final String intoTableName;
    private final boolean isFilterExpression;

    public ExprValidationContext(StreamTypeService streamTypeService,
                                 EngineImportService engineImportService,
                                 StatementExtensionSvcContext statementExtensionSvcContext,
                                 ViewResourceDelegateUnverified viewResourceDelegate,
                                 TimeProvider timeProvider,
                                 VariableService variableService,
                                 TableService tableService,
                                 ExprEvaluatorContext exprEvaluatorContext,
                                 EventAdapterService eventAdapterService,
                                 String statementName,
                                 int statementId,
                                 Annotation[] annotations,
                                 ContextDescriptor contextDescriptor,
                                 boolean disablePropertyExpressionEventCollCache,
                                 boolean allowRollupFunctions,
                                 boolean allowBindingConsumption,
                                 boolean isUnidirectionalJoin,
                                 String intoTableName,
                                 boolean isFilterExpression) {
        this.streamTypeService = streamTypeService;
        this.engineImportService = engineImportService;
        this.statementExtensionSvcContext = statementExtensionSvcContext;
        this.viewResourceDelegate = viewResourceDelegate;
        this.timeProvider = timeProvider;
        this.variableService = variableService;
        this.tableService = tableService;
        this.exprEvaluatorContext = exprEvaluatorContext;
        this.eventAdapterService = eventAdapterService;
        this.statementName = statementName;
        this.statementId = statementId;
        this.annotations = annotations;
        this.contextDescriptor = contextDescriptor;
        this.disablePropertyExpressionEventCollCache = disablePropertyExpressionEventCollCache;
        this.allowRollupFunctions = allowRollupFunctions;
        this.allowBindingConsumption = allowBindingConsumption;
        this.isResettingAggregations = isUnidirectionalJoin;
        this.intoTableName = intoTableName;
        this.isFilterExpression = isFilterExpression;

        isExpressionAudit = AuditEnum.EXPRESSION.getAudit(annotations) != null;
        isExpressionNestedAudit = AuditEnum.EXPRESSION_NESTED.getAudit(annotations) != null;
    }

    public ExprValidationContext(StreamTypeServiceImpl types, ExprValidationContext ctx) {
        this(types, ctx.getEngineImportService(), ctx.getStatementExtensionSvcContext(), ctx.getViewResourceDelegate(), ctx.getTimeProvider(), ctx.getVariableService(), ctx.getTableService(), ctx.getExprEvaluatorContext(), ctx.getEventAdapterService(), ctx.getStatementName(), ctx.getStatementId(), ctx.getAnnotations(), ctx.getContextDescriptor(), ctx.isDisablePropertyExpressionEventCollCache(), false, ctx.isAllowBindingConsumption(), ctx.isResettingAggregations(), ctx.getIntoTableName(), false);
    }

    public StreamTypeService getStreamTypeService() {
        return streamTypeService;
    }

    public ViewResourceDelegateUnverified getViewResourceDelegate() {
        return viewResourceDelegate;
    }

    public TimeProvider getTimeProvider() {
        return timeProvider;
    }

    public VariableService getVariableService() {
        return variableService;
    }

    public ExprEvaluatorContext getExprEvaluatorContext() {
        return exprEvaluatorContext;
    }

    public EventAdapterService getEventAdapterService() {
        return eventAdapterService;
    }

    public String getStatementName() {
        return statementName;
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    public boolean isExpressionNestedAudit() {
        return isExpressionNestedAudit;
    }

    public boolean isExpressionAudit() {
        return isExpressionAudit;
    }

    public int getStatementId() {
        return statementId;
    }

    public ContextDescriptor getContextDescriptor() {
        return contextDescriptor;
    }

    public boolean isDisablePropertyExpressionEventCollCache() {
        return disablePropertyExpressionEventCollCache;
    }

    public boolean isAllowRollupFunctions() {
        return allowRollupFunctions;
    }

    public TableService getTableService() {
        return tableService;
    }

    public boolean isAllowBindingConsumption() {
        return allowBindingConsumption;
    }

    public boolean isResettingAggregations() {
        return isResettingAggregations;
    }

    public String getIntoTableName() {
        return intoTableName;
    }

    public boolean isFilterExpression() {
        return isFilterExpression;
    }

    public EngineImportService getEngineImportService() {
        return engineImportService;
    }

    public StatementExtensionSvcContext getStatementExtensionSvcContext() {
        return statementExtensionSvcContext;
    }
}
