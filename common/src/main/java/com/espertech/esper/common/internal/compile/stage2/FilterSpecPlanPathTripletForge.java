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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbolWEventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCompare;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiler.NEWLINE;
import static com.espertech.esper.common.internal.compile.stage2.FilterSpecPlanForge.optionalEvaluator;

public class FilterSpecPlanPathTripletForge {
    private final FilterSpecParamForge param;
    private ExprNode tripletConfirm;

    public FilterSpecPlanPathTripletForge(FilterSpecParamForge param, ExprNode tripletConfirm) {
        this.param = param;
        this.tripletConfirm = tripletConfirm;
    }

    public FilterSpecParamForge getParam() {
        return param;
    }

    public ExprNode getTripletConfirm() {
        return tripletConfirm;
    }

    public void setTripletConfirm(ExprNode tripletConfirm) {
        this.tripletConfirm = tripletConfirm;
    }

    public boolean equalsFilter(FilterSpecPlanPathTripletForge other) {
        if (!ExprNodeUtilityCompare.deepEqualsNullChecked(tripletConfirm, other.tripletConfirm, true)) {
            return false;
        }
        return param.equals(other.getParam());
    }

    public CodegenMethod codegen(CodegenMethodScope parent, SAIFFInitializeSymbolWEventType symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(FilterSpecPlanPathTriplet.EPTYPE, FilterSpecParamForge.class, classScope);
        method.getBlock()
            .declareVarNewInstance(FilterSpecPlanPathTriplet.EPTYPE, "triplet")
            .exprDotMethod(ref("triplet"), "setParam", localMethod(param.makeCodegen(classScope, method, symbols)))
            .exprDotMethod(ref("triplet"), "setTripletConfirm", optionalEvaluator(tripletConfirm, method, classScope))
            .methodReturn(ref("triplet"));
        return method;
    }

    protected void appendFilterPlanTriplet(int indexForge, StringBuilder buf) {
        buf.append("    -triplet #").append(indexForge).append(NEWLINE);
        if (tripletConfirm != null) {
            buf.append("      -triplet-confirm-expression: ").append(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(tripletConfirm)).append(NEWLINE);
        }
        param.appendFilterPlanParam(buf);
    }
}
