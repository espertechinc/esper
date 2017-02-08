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
package com.espertech.esper.epl.methodbase;

import com.espertech.esper.epl.expression.core.ExprNode;

public class DotMethodFPProvidedParam {

    private int lambdaParamNum; // 0 means not a lambda expression expected, 1 means "x=>", 2 means "(x,y)=>"
    private Class returnType;
    private ExprNode expression;

    public DotMethodFPProvidedParam(int lambdaParamNum, Class returnType, ExprNode expression) {
        this.lambdaParamNum = lambdaParamNum;
        this.returnType = returnType;
        this.expression = expression;
    }

    public int getLambdaParamNum() {
        return lambdaParamNum;
    }

    public Class getReturnType() {
        return returnType;
    }

    public ExprNode getExpression() {
        return expression;
    }
}
