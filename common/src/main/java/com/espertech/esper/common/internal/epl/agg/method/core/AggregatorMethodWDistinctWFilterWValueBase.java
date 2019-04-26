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
package com.espertech.esper.common.internal.epl.agg.method.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public abstract class AggregatorMethodWDistinctWFilterWValueBase extends AggregatorMethodWDistinctWFilterBase {
    protected abstract void applyEvalEnterNonNull(CodegenExpressionRef value, Class valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope);

    protected abstract void applyEvalLeaveNonNull(CodegenExpressionRef value, Class valueType, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope);

    protected abstract void applyTableEnterNonNull(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope);

    protected abstract void applyTableLeaveNonNull(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope);

    public AggregatorMethodWDistinctWFilterWValueBase(AggregationForgeFactory factory, int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope, Class optionalDistinctValueType, DataInputOutputSerdeForge optionalDistinctSerde, boolean hasFilter, ExprNode optionalFilter) {
        super(factory, col, rowCtor, membersColumnized, classScope, optionalDistinctValueType, optionalDistinctSerde, hasFilter, optionalFilter);
    }

    protected final void applyEvalEnterFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        applyEvalValuePrefix(true, method, symbols, forges, classScope);
        applyEvalEnterNonNull(ref("val"), forges[0].getEvaluationType(), method, symbols, forges, classScope);
    }

    protected final void applyTableEnterFiltered(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        applyTableValuePrefix(true, value, method, classScope);
        applyTableEnterNonNull(value, evaluationTypes, method, classScope);
    }

    protected final void applyEvalLeaveFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        applyEvalValuePrefix(false, method, symbols, forges, classScope);
        applyEvalLeaveNonNull(ref("val"), forges[0].getEvaluationType(), method, symbols, forges, classScope);
    }

    protected final void applyTableLeaveFiltered(CodegenExpressionRef value, Class[] evaluationTypes, CodegenMethod method, CodegenClassScope classScope) {
        applyTableValuePrefix(false, value, method, classScope);
        applyTableLeaveNonNull(value, evaluationTypes, method, classScope);
    }

    private void applyEvalValuePrefix(boolean enter, CodegenMethod method, ExprForgeCodegenSymbol symbols, ExprForge[] forges, CodegenClassScope classScope) {
        Class type = forges[0].getEvaluationType();
        CodegenExpression expr = forges[0].evaluateCodegen(type, method, symbols, classScope);
        method.getBlock().declareVar(type, "val", expr);
        if (!type.isPrimitive()) {
            method.getBlock().ifRefNull("val").blockReturnNoValue();
        }
        if (distinct != null) {
            method.getBlock().ifCondition(not(exprDotMethod(distinct, enter ? "add" : "remove", toDistinctValueKey(ref("val"))))).blockReturnNoValue();
        }
    }

    private void applyTableValuePrefix(boolean enter, CodegenExpressionRef value, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().ifCondition(equalsNull(value)).blockReturnNoValue();
        if (distinct != null) {
            method.getBlock().ifCondition(not(exprDotMethod(distinct, enter ? "add" : "remove", toDistinctValueKey(value)))).blockReturnNoValue();
        }
    }
}
