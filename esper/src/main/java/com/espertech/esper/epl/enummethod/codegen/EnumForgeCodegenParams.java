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
package com.espertech.esper.epl.enummethod.codegen;

import com.espertech.esper.codegen.model.expression.CodegenExpression;

public class EnumForgeCodegenParams {

    private final CodegenExpression eps;
    private final CodegenExpression enumcoll;
    private final CodegenExpression isNewData;
    private final CodegenExpression exprCtx;

    public EnumForgeCodegenParams(CodegenExpression eps, CodegenExpression enumcoll, CodegenExpression isNewData, CodegenExpression exprCtx) {
        this.eps = eps;
        this.enumcoll = enumcoll;
        this.isNewData = isNewData;
        this.exprCtx = exprCtx;
    }

    public CodegenExpression getEps() {
        return eps;
    }

    public CodegenExpression getEnumcoll() {
        return enumcoll;
    }

    public CodegenExpression getIsNewData() {
        return isNewData;
    }

    public CodegenExpression getExprCtx() {
        return exprCtx;
    }

    public CodegenExpression[] getExpressions() {
        return new CodegenExpression[] {eps, enumcoll, isNewData, exprCtx};
    }
}

