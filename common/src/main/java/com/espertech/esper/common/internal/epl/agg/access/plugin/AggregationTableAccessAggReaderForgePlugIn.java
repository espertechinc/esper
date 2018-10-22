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

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionTableReaderFactory;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionTableReaderModeManaged;
import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategyClassNewInstance;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationTableAccessAggReaderForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethod;

public class AggregationTableAccessAggReaderForgePlugIn implements AggregationTableAccessAggReaderForge {
    private final Class resultType;
    private final AggregationMultiFunctionTableReaderModeManaged mode;

    public AggregationTableAccessAggReaderForgePlugIn(Class resultType, AggregationMultiFunctionTableReaderModeManaged mode) {
        this.resultType = resultType;
        this.mode = mode;
    }

    public Class getResultType() {
        return resultType;
    }

    public CodegenExpression codegenCreateReader(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        InjectionStrategyClassNewInstance injectionStrategy = (InjectionStrategyClassNewInstance) mode.getInjectionStrategyTableReaderFactory();
        CodegenExpressionField factoryField = classScope.addFieldUnshared(true, AggregationMultiFunctionTableReaderFactory.class, injectionStrategy.getInitializationExpression(classScope));
        return exprDotMethod(factoryField, "newReader", constantNull());
    }
}
