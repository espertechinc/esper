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

import com.espertech.esper.common.client.annotation.AuditEnum;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeDescriptor;
import com.espertech.esper.common.internal.epl.enummethod.compile.EnumMethodCallStackHelperImpl;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeResolver;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableCompileTimeResolver;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCompileTime;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.view.access.ViewResourceDelegateExpr;

import java.lang.annotation.Annotation;

public class ExprValidationContext {

    private final StreamTypeService streamTypeService;
    private final ViewResourceDelegateExpr viewResourceDelegate;
    private final ContextCompileTimeDescriptor contextDescriptor;
    private final boolean disablePropertyExpressionEventCollCache;
    private final boolean allowRollupFunctions;
    private final boolean allowBindingConsumption;
    private final boolean isResettingAggregations;
    private final boolean isExpressionNestedAudit;
    private final boolean isExpressionAudit;
    private final String intoTableName;
    private final boolean isFilterExpression;
    private final ExprValidationMemberName memberName;
    private final boolean aggregationFutureNameAlreadySet;
    private final StatementRawInfo statementRawInfo;
    private final StatementCompileTimeServices compileTimeServices;

    public ExprValidationContext(StreamTypeService streamTypeService, ExprValidationContext ctx) {
        this(streamTypeService, ctx.getViewResourceDelegate(), ctx.contextDescriptor,
                ctx.disablePropertyExpressionEventCollCache, ctx.allowRollupFunctions, ctx.allowBindingConsumption,
                ctx.isResettingAggregations, ctx.intoTableName, ctx.isFilterExpression, ctx.memberName,
                ctx.aggregationFutureNameAlreadySet,
                ctx.statementRawInfo, ctx.compileTimeServices);
    }

    protected ExprValidationContext(StreamTypeService streamTypeService,
                                    ViewResourceDelegateExpr viewResourceDelegate,
                                    ContextCompileTimeDescriptor contextDescriptor,
                                    boolean disablePropertyExpressionEventCollCache,
                                    boolean allowRollupFunctions,
                                    boolean allowBindingConsumption,
                                    boolean isUnidirectionalJoin,
                                    String intoTableName,
                                    boolean isFilterExpression,
                                    ExprValidationMemberName memberName,
                                    boolean aggregationFutureNameAlreadySet,
                                    StatementRawInfo statementRawInfo,
                                    StatementCompileTimeServices compileTimeServices) {
        this.streamTypeService = streamTypeService;
        this.viewResourceDelegate = viewResourceDelegate;
        this.contextDescriptor = contextDescriptor;
        this.disablePropertyExpressionEventCollCache = disablePropertyExpressionEventCollCache;
        this.allowRollupFunctions = allowRollupFunctions;
        this.allowBindingConsumption = allowBindingConsumption;
        this.isResettingAggregations = isUnidirectionalJoin;
        this.intoTableName = intoTableName;
        this.isFilterExpression = isFilterExpression;
        this.memberName = memberName;
        this.aggregationFutureNameAlreadySet = aggregationFutureNameAlreadySet;
        this.statementRawInfo = statementRawInfo;
        this.compileTimeServices = compileTimeServices;

        isExpressionAudit = AuditEnum.EXPRESSION.getAudit(statementRawInfo.getAnnotations()) != null;
        isExpressionNestedAudit = AuditEnum.EXPRESSION_NESTED.getAudit(statementRawInfo.getAnnotations()) != null;
    }

    public Annotation[] getAnnotations() {
        return statementRawInfo.getAnnotations();
    }

    public StreamTypeService getStreamTypeService() {
        return streamTypeService;
    }

    public ContextCompileTimeDescriptor getContextDescriptor() {
        return statementRawInfo.getOptionalContextDescriptor();
    }

    public boolean isFilterExpression() {
        return isFilterExpression;
    }

    public ClasspathImportServiceCompileTime getClasspathImportService() {
        return compileTimeServices.getClasspathImportServiceCompileTime();
    }

    public VariableCompileTimeResolver getVariableCompileTimeResolver() {
        return compileTimeServices.getVariableCompileTimeResolver();
    }

    public TableCompileTimeResolver getTableCompileTimeResolver() {
        return compileTimeServices.getTableCompileTimeResolver();
    }

    public String getStatementName() {
        return statementRawInfo.getStatementName();
    }

    public boolean isDisablePropertyExpressionEventCollCache() {
        return disablePropertyExpressionEventCollCache;
    }

    public ViewResourceDelegateExpr getViewResourceDelegate() {
        return viewResourceDelegate;
    }

    public ExprValidationMemberName getMemberNames() {
        return memberName;
    }

    public StatementType getStatementType() {
        return statementRawInfo.getStatementType();
    }

    public boolean isResettingAggregations() {
        return isResettingAggregations;
    }

    public boolean isAllowRollupFunctions() {
        return allowRollupFunctions;
    }

    public StatementCompileTimeServices getStatementCompileTimeService() {
        return compileTimeServices;
    }

    public StatementRawInfo getStatementRawInfo() {
        return statementRawInfo;
    }

    public boolean isAllowBindingConsumption() {
        return allowBindingConsumption;
    }

    public EnumMethodCallStackHelperImpl getEnumMethodCallStackHelper() {
        return compileTimeServices.getEnumMethodCallStackHelper();
    }

    public boolean isAggregationFutureNameAlreadySet() {
        return aggregationFutureNameAlreadySet;
    }

    public boolean isExpressionNestedAudit() {
        return isExpressionNestedAudit;
    }

    public boolean isExpressionAudit() {
        return isExpressionAudit;
    }

    public EventBeanTypedEventFactory getEventBeanTypedEventFactory() {
        return EventBeanTypedEventFactoryCompileTime.INSTANCE;
    }

    public String getModuleName() {
        return statementRawInfo.getModuleName();
    }
}
