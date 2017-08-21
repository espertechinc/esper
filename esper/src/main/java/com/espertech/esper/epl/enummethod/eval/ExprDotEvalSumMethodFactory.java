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
package com.espertech.esper.epl.enummethod.eval;

import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;

public interface ExprDotEvalSumMethodFactory {
    ExprDotEvalSumMethod getSumAggregator();
    Class getValueType();

    void codegenDeclare(CodegenBlock block);
    void codegenEnterNumberTypedNonNull(CodegenBlock block, CodegenExpressionRef value);
    void codegenEnterObjectTypedNonNull(CodegenBlock block, CodegenExpressionRef value);
    void codegenReturn(CodegenBlock block);
}
