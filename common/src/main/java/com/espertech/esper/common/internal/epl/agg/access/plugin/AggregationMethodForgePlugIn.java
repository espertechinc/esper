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

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAggregationMethodFactory;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAggregationMethodModeManaged;
import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategyClassNewInstance;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationMethodForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethod;

public class AggregationMethodForgePlugIn implements AggregationMethodForge {
    private final Class resultType;
    private final AggregationMultiFunctionAggregationMethodModeManaged mode;

    public AggregationMethodForgePlugIn(Class resultType, AggregationMultiFunctionAggregationMethodModeManaged mode) {
        this.resultType = resultType;
        this.mode = mode;
    }

    public Class getResultType() {
        return resultType;
    }

    public CodegenExpression codegenCreateReader(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        InjectionStrategyClassNewInstance injectionStrategy = (InjectionStrategyClassNewInstance) mode.getInjectionStrategyAggregationMethodFactory();
        CodegenExpressionField factoryField = classScope.addFieldUnshared(true, AggregationMultiFunctionAggregationMethodFactory.class, injectionStrategy.getInitializationExpression(classScope));
        return exprDotMethod(factoryField, "newMethod", constantNull());
    }
}
