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
package com.espertech.esper.common.internal.epl.agg.access.plugin;

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAgentFactory;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAgentModeManaged;
import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategyClassNewInstance;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregationAgentForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethod;

public class AggregationAgentForgePlugin implements AggregationAgentForge {

    private final AggregationForgeFactoryAccessPlugin parent;
    private final AggregationMultiFunctionAgentModeManaged mode;
    private final ExprForge optionalFilter;

    public AggregationAgentForgePlugin(AggregationForgeFactoryAccessPlugin parent, AggregationMultiFunctionAgentModeManaged mode, ExprForge optionalFilter) {
        this.parent = parent;
        this.mode = mode;
        this.optionalFilter = optionalFilter;
    }

    public ExprForge getOptionalFilter() {
        return optionalFilter;
    }

    public CodegenExpression make(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        InjectionStrategyClassNewInstance injectionStrategy = (InjectionStrategyClassNewInstance) mode.getInjectionStrategyAggregationAgentFactory();
        CodegenExpressionField factoryField = classScope.addFieldUnshared(true, AggregationMultiFunctionAgentFactory.class, injectionStrategy.getInitializationExpression(classScope));
        return exprDotMethod(factoryField, "newAgent", constantNull());
    }
}
