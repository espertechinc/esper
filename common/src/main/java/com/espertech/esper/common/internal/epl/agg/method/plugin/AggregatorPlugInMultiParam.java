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
package com.espertech.esper.common.internal.epl.agg.method.plugin;

import com.espertech.esper.common.client.hook.aggfunc.AggregationFunction;
import com.espertech.esper.common.client.hook.aggfunc.AggregationFunctionFactory;
import com.espertech.esper.common.client.hook.aggfunc.AggregationFunctionModeMultiParam;
import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategyClassNewInstance;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethod;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.rowDotMember;

public class AggregatorPlugInMultiParam implements AggregatorMethod {

    protected CodegenExpressionMember plugin;
    private final AggregationFunctionModeMultiParam mode;

    public AggregatorPlugInMultiParam(AggregationForgeFactoryPlugin factory, int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope, AggregationFunctionModeMultiParam mode) {
        this.mode = mode;
        InjectionStrategyClassNewInstance injectionStrategy = (InjectionStrategyClassNewInstance) mode.getInjectionStrategyAggregationFunctionFactory();
        CodegenExpressionField factoryField = classScope.addFieldUnshared(true, AggregationFunctionFactory.class, injectionStrategy.getInitializationExpression(classScope));

        plugin = membersColumnized.addMember(col, AggregationFunction.class, "plugin");
        rowCtor.getBlock().assignRef(plugin, exprDotMethod(factoryField, "newAggregator", constantNull()));
    }

    public void applyEvalEnterCodegen(CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        apply(true, method, symbols, forges, classScope);
    }

    public void applyEvalLeaveCodegen(CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        apply(false, method, symbols, forges, classScope);
    }

    public void applyTableEnterCodegen(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(plugin, "enter", value);
    }

    public void applyTableLeaveCodegen(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(plugin, "leave", value);
    }

    public void clearCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(plugin, "clear");
    }

    public void getValueCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(exprDotMethod(plugin, "getValue"));
    }

    public void writeCodegen(CodegenExpressionRef row, int col, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenMethod method, CodegenClassScope classScope) {
        if (mode.isHasHA()) {
            method.getBlock().staticMethod(mode.getSerde(), "write", output, rowDotMember(row, plugin));
        }
    }

    public void readCodegen(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenExpressionRef unitKey, CodegenMethod method, CodegenClassScope classScope) {
        if (mode.isHasHA()) {
            method.getBlock().assignRef(rowDotMember(row, plugin), staticMethod(mode.getSerde(), "read", input));
        }
    }

    private void apply(boolean enter, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        CodegenExpression expression;
        if (forges.length == 0) {
            expression = constantNull();
        } else if (forges.length == 1) {
            expression = forges[0].evaluateCodegen(Object.class, method, symbols, classScope);
        } else {
            method.getBlock().declareVar(Object[].class, "params", newArrayByLength(Object.class, constant(forges.length)));
            for (int i = 0; i < forges.length; i++) {
                method.getBlock().assignArrayElement("params", constant(i), forges[i].evaluateCodegen(Object.class, method, symbols, classScope));
            }
            expression = ref("params");
        }
        method.getBlock().exprDotMethod(plugin, enter ? "enter" : "leave", expression);
    }
}
