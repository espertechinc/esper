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
package com.espertech.esper.common.internal.epl.datetime.reformatop;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeLongCoercer;
import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeLongCoercerFactory;
import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeMethodDesc;
import com.espertech.esper.common.internal.epl.datetime.eval.FilterExprAnalyzerDTBetweenAffector;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeFilterAnalyzerInput;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeFilterAnalyzerInputProp;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeFilterAnalyzerInputStream;
import com.espertech.esper.common.internal.epl.join.analyze.FilterExprAnalyzerAffector;

import java.util.List;

public class ReformatBetweenNonConstantParamsForge implements ReformatForge {

    protected final ExprNode start;
    protected final DatetimeLongCoercer startCoercer;
    protected final ExprNode end;
    protected final DatetimeLongCoercer secondCoercer;

    protected boolean includeBoth;
    protected Boolean includeLow;
    protected Boolean includeHigh;
    protected ExprForge forgeIncludeLow;
    protected ExprForge forgeIncludeHigh;

    public ReformatBetweenNonConstantParamsForge(List<ExprNode> parameters)
            throws ExprValidationException {
        start = parameters.get(0);
        startCoercer = DatetimeLongCoercerFactory.getCoercer(start.getForge().getEvaluationType());
        end = parameters.get(1);
        secondCoercer = DatetimeLongCoercerFactory.getCoercer(end.getForge().getEvaluationType());

        if (parameters.size() == 2) {
            includeBoth = true;
            includeLow = true;
            includeHigh = true;
        } else {
            if (parameters.get(2).getForge().getForgeConstantType().isCompileTimeConstant()) {
                includeLow = getBooleanValue(parameters.get(2));
            } else {
                forgeIncludeLow = parameters.get(2).getForge();
            }
            if (parameters.get(3).getForge().getForgeConstantType().isCompileTimeConstant()) {
                includeHigh = getBooleanValue(parameters.get(3));
            } else {
                forgeIncludeHigh = parameters.get(3).getForge();
            }
            if (includeLow != null && includeHigh != null && includeLow && includeHigh) {
                includeBoth = true;
            }
        }
    }

    public ReformatOp getOp() {
        return new ReformatBetweenNonConstantParamsForgeOp(this, start.getForge().getExprEvaluator(), end.getForge().getExprEvaluator(),
                forgeIncludeLow == null ? null : forgeIncludeLow.getExprEvaluator(), forgeIncludeHigh == null ? null : forgeIncludeHigh.getExprEvaluator());
    }

    public CodegenExpression codegenLong(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ReformatBetweenNonConstantParamsForgeOp.codegenLong(this, inner, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression codegenDate(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ReformatBetweenNonConstantParamsForgeOp.codegenDate(this, inner, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression codegenCal(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ReformatBetweenNonConstantParamsForgeOp.codegenCal(this, inner, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression codegenLDT(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ReformatBetweenNonConstantParamsForgeOp.codegenLDT(this, inner, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression codegenZDT(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return ReformatBetweenNonConstantParamsForgeOp.codegenZDT(this, inner, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public Class getReturnType() {
        return Boolean.class;
    }

    public FilterExprAnalyzerAffector getFilterDesc(EventType[] typesPerStream, DatetimeMethodDesc currentMethod, List<ExprNode> currentParameters, ExprDotNodeFilterAnalyzerInput inputDesc) {
        if (includeLow == null || includeHigh == null) {
            return null;
        }

        int targetStreamNum;
        String targetProperty;
        if (inputDesc instanceof ExprDotNodeFilterAnalyzerInputStream) {
            ExprDotNodeFilterAnalyzerInputStream targetStream = (ExprDotNodeFilterAnalyzerInputStream) inputDesc;
            targetStreamNum = targetStream.getStreamNum();
            EventType targetType = typesPerStream[targetStreamNum];
            targetProperty = targetType.getStartTimestampPropertyName();
        } else if (inputDesc instanceof ExprDotNodeFilterAnalyzerInputProp) {
            ExprDotNodeFilterAnalyzerInputProp targetStream = (ExprDotNodeFilterAnalyzerInputProp) inputDesc;
            targetStreamNum = targetStream.getStreamNum();
            targetProperty = targetStream.getPropertyName();
        } else {
            return null;
        }

        return new FilterExprAnalyzerDTBetweenAffector(typesPerStream, targetStreamNum, targetProperty, start, end, includeLow, includeHigh);
    }

    private boolean getBooleanValue(ExprNode exprNode)
            throws ExprValidationException {
        Object value = exprNode.getForge().getExprEvaluator().evaluate(null, true, null);
        if (value == null) {
            throw new ExprValidationException("Date-time method 'between' requires non-null parameter values");
        }
        return (Boolean) value;
    }
}
