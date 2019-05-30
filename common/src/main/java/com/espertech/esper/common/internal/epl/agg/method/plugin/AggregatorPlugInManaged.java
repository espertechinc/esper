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
import com.espertech.esper.common.client.hook.aggfunc.AggregationFunctionModeManaged;
import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategyClassNewInstance;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.agg.method.core.AggregatorMethodWDistinctWFilterWValueBase;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.rowDotMember;

public class AggregatorPlugInManaged extends AggregatorMethodWDistinctWFilterWValueBase {

    protected CodegenExpressionMember plugin;
    private final AggregationFunctionModeManaged mode;

    public AggregatorPlugInManaged(AggregationForgeFactoryPlugin factory, int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope, Class optionalDistinctValueType, DataInputOutputSerdeForge optionalDistinctSerde, boolean hasFilter, ExprNode optionalFilter, AggregationFunctionModeManaged mode) {
        super(factory, col, rowCtor, membersColumnized, classScope, optionalDistinctValueType, optionalDistinctSerde, hasFilter, optionalFilter);
        this.mode = mode;

        InjectionStrategyClassNewInstance injectionStrategy = (InjectionStrategyClassNewInstance) mode.getInjectionStrategyAggregationFunctionFactory();
        CodegenExpressionField factoryField = classScope.addFieldUnshared(true, AggregationFunctionFactory.class, injectionStrategy.getInitializationExpression(classScope));

        plugin = membersColumnized.addMember(col, AggregationFunction.class, "plugin");
        rowCtor.getBlock().assignRef(plugin, exprDotMethod(factoryField, "newAggregator", constantNull()));
    }

    protected void applyEvalEnterNonNull(CodegenExpressionRef value, Class valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(plugin, "enter", value);
    }

    protected void applyEvalLeaveNonNull(CodegenExpressionRef value, Class valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(plugin, "leave", value);
    }

    protected void applyTableEnterNonNull(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(plugin, "enter", value);
    }

    protected void applyTableLeaveNonNull(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(plugin, "leave", value);
    }

    protected void clearWODistinct(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(plugin, "clear");
    }

    protected void writeWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenMethod method, CodegenClassScope classScope) {
        if (mode.isHasHA()) {
            method.getBlock().staticMethod(mode.getSerde(), "write", output, rowDotMember(row, plugin));
        }
    }

    protected void readWODistinct(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenExpressionRef unitKey, CodegenMethod method, CodegenClassScope classScope) {
        if (mode.isHasHA()) {
            method.getBlock().assignRef(rowDotMember(row, plugin), staticMethod(mode.getSerde(), "read", input));
        }
    }

    public void getValueCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(exprDotMethod(plugin, "getValue"));
    }
}
