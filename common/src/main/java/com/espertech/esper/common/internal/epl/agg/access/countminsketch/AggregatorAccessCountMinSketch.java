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
package com.espertech.esper.common.internal.epl.agg.access.countminsketch;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedMethods;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.agg.core.AggregatorAccess;
import com.espertech.esper.common.internal.epl.approx.countminsketch.CountMinSketchAggState;
import com.espertech.esper.common.internal.epl.approx.countminsketch.CountMinSketchSpec;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.rowDotMember;

/**
 * Implementation of access function for single-stream (not joins).
 */
public class AggregatorAccessCountMinSketch implements AggregatorAccess {
    private final AggregationStateCountMinSketchForge forge;
    private final CodegenExpressionMember state;
    private CodegenExpressionField spec;

    public AggregatorAccessCountMinSketch(AggregationStateCountMinSketchForge forge, int col, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {
        this.forge = forge;
        state = membersColumnized.addMember(col, CountMinSketchAggState.class, "state");
        spec = classScope.getPackageScope().addFieldUnshared(true, CountMinSketchSpec.class, forge.specification.codegenMake(classScope.getPackageScope().getInitMethod(), classScope));
        rowCtor.getBlock().assignRef(state, exprDotMethod(spec, "makeAggState"));
    }

    public void applyEnterCodegen(CodegenMethod method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodThrowUnsupported();
    }

    public void applyLeaveCodegen(CodegenMethod method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodThrowUnsupported();
    }

    public void clearCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().assignRef(state, exprDotMethod(spec, "makeAggState"));
    }

    public void writeCodegen(CodegenExpressionRef row, int col, CodegenExpressionRef output, CodegenExpressionRef unitKey, CodegenExpressionRef writer, CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().expression(staticMethod(AggregationStateSerdeCountMinSketch.class, "writeCountMinSketch", output, rowDotMember(row, state)));
    }

    public void readCodegen(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenMethod method, CodegenExpressionRef unitKey, CodegenClassScope classScope) {
        method.getBlock().assignRef(rowDotMember(row, state), staticMethod(AggregationStateSerdeCountMinSketch.class, "readCountMinSketch", input, spec));
    }

    public static CodegenExpression codegenGetAccessTableState(int column, CodegenMethodScope parent, CodegenClassScope classScope) {
        return memberCol("state", column);
    }
}
