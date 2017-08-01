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
package com.espertech.esper.epl.datetime.reformatop;

import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.datetime.eval.DatetimeMethodEnum;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.dot.ExprDotNodeFilterAnalyzerInput;
import com.espertech.esper.epl.join.plan.FilterExprAnalyzerAffector;

import java.util.List;

public interface ReformatForge {

    ReformatOp getOp();

    Class getReturnType();

    FilterExprAnalyzerAffector getFilterDesc(EventType[] typesPerStream, DatetimeMethodEnum currentMethod, List<ExprNode> currentParameters, ExprDotNodeFilterAnalyzerInput inputDesc);

    CodegenExpression codegenLong(CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context);
    CodegenExpression codegenDate(CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context);
    CodegenExpression codegenCal(CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context);
    CodegenExpression codegenLDT(CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context);
    CodegenExpression codegenZDT(CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context);
}
