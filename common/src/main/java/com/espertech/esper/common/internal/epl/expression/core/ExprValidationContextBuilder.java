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

import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeDescriptor;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.view.access.ViewResourceDelegateExpr;

public final class ExprValidationContextBuilder {
    private final StreamTypeService streamTypeService;
    private final StatementRawInfo statementRawInfo;
    private final StatementCompileTimeServices compileTimeServices;

    private ViewResourceDelegateExpr viewResourceDelegate;
    private ContextCompileTimeDescriptor contextDescriptor;
    private boolean disablePropertyExpressionEventCollCache;
    private boolean allowRollupFunctions;
    private boolean allowBindingConsumption;
    private boolean allowTableAggReset;
    private String intoTableName;
    private boolean isFilterExpression;
    private boolean isResettingAggregations;
    private boolean aggregationFutureNameAlreadySet;
    private ExprValidationMemberName memberName = ExprValidationMemberNameDefault.INSTANCE;

    public ExprValidationContextBuilder(StreamTypeService streamTypeService,
                                        StatementRawInfo statementRawInfo,
                                        StatementCompileTimeServices compileTimeServices) {
        this.streamTypeService = streamTypeService;
        this.statementRawInfo = statementRawInfo;
        this.compileTimeServices = compileTimeServices;
    }

    public ExprValidationContextBuilder withViewResourceDelegate(ViewResourceDelegateExpr viewResourceDelegate) {
        this.viewResourceDelegate = viewResourceDelegate;
        return this;
    }

    public ExprValidationContextBuilder withContextDescriptor(ContextCompileTimeDescriptor contextDescriptor) {
        this.contextDescriptor = contextDescriptor;
        return this;
    }

    public ExprValidationContextBuilder withDisablePropertyExpressionEventCollCache(boolean disablePropertyExpressionEventCollCache) {
        this.disablePropertyExpressionEventCollCache = disablePropertyExpressionEventCollCache;
        return this;
    }

    public ExprValidationContextBuilder withAllowRollupFunctions(boolean allowRollupFunctions) {
        this.allowRollupFunctions = allowRollupFunctions;
        return this;
    }

    public ExprValidationContextBuilder withAllowBindingConsumption(boolean allowBindingConsumption) {
        this.allowBindingConsumption = allowBindingConsumption;
        return this;
    }

    public ExprValidationContextBuilder withAllowTableAggReset(boolean allowTableAggReset) {
        this.allowTableAggReset = allowTableAggReset;
        return this;
    }

    public ExprValidationContextBuilder withIntoTableName(String intoTableName) {
        this.intoTableName = intoTableName;
        return this;
    }

    public ExprValidationContextBuilder withIsFilterExpression(boolean isFilterExpression) {
        this.isFilterExpression = isFilterExpression;
        return this;
    }

    public ExprValidationContextBuilder withMemberName(ExprValidationMemberName memberName) {
        this.memberName = memberName;
        return this;
    }

    public ExprValidationContextBuilder withIsResettingAggregations(boolean isResettingAggregations) {
        this.isResettingAggregations = isResettingAggregations;
        return this;
    }

    public ExprValidationContextBuilder withAggregationFutureNameAlreadySet(boolean aggregationFutureNameAlreadySet) {
        this.aggregationFutureNameAlreadySet = aggregationFutureNameAlreadySet;
        return this;
    }

    public ExprValidationContext build() {
        return new ExprValidationContext(streamTypeService, viewResourceDelegate, contextDescriptor, disablePropertyExpressionEventCollCache, allowRollupFunctions,
                allowBindingConsumption, allowTableAggReset, isResettingAggregations, intoTableName, isFilterExpression, memberName, aggregationFutureNameAlreadySet, statementRawInfo, compileTimeServices);
    }
}
