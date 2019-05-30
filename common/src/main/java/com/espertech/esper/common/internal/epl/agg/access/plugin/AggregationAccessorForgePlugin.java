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

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAccessor;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAccessorModeManaged;
import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategyClassNewInstance;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.epl.agg.core.AggregationAccessorForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregationAccessorForgeGetCodegenContext;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.*;

public class AggregationAccessorForgePlugin implements AggregationAccessorForge {

    private final AggregationForgeFactoryAccessPlugin parent;
    private final AggregationMultiFunctionAccessorModeManaged mode;
    private CodegenExpressionField accessorField;

    public AggregationAccessorForgePlugin(AggregationForgeFactoryAccessPlugin parent, AggregationMultiFunctionAccessorModeManaged mode) {
        this.parent = parent;
        this.mode = mode;
    }

    public void getValueCodegen(AggregationAccessorForgeGetCodegenContext context) {
        makeBlock("getValue", context.getColumn(), context.getMethod(), context.getClassScope());
    }

    public void getEnumerableEventsCodegen(AggregationAccessorForgeGetCodegenContext context) {
        makeBlock("getEnumerableEvents", context.getColumn(), context.getMethod(), context.getClassScope());
    }

    public void getEnumerableEventCodegen(AggregationAccessorForgeGetCodegenContext context) {
        makeBlock("getEnumerableEvent", context.getColumn(), context.getMethod(), context.getClassScope());
    }

    public void getEnumerableScalarCodegen(AggregationAccessorForgeGetCodegenContext context) {
        makeBlock("getEnumerableScalar", context.getColumn(), context.getMethod(), context.getClassScope());
    }

    private void makeBlock(String getterMethod, int column, CodegenMethod method, CodegenClassScope classScope) {
        if (accessorField == null) {
            InjectionStrategyClassNewInstance injectionStrategy = (InjectionStrategyClassNewInstance) mode.getInjectionStrategyAggregationAccessorFactory();
            accessorField = classScope.addFieldUnshared(true, AggregationMultiFunctionAccessor.class, exprDotMethod(injectionStrategy.getInitializationExpression(classScope), "newAccessor", constantNull()));
        }
        method.getBlock().methodReturn(exprDotMethod(accessorField, getterMethod, memberCol("state", column), REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
    }
}
