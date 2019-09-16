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
package com.espertech.esper.common.internal.epl.methodbase;

import com.espertech.esper.common.internal.epl.util.EPLExpressionParamType;

import java.io.Serializable;

public class DotMethodFPParam implements Serializable {

    private static final long serialVersionUID = -8644850353486284119L;
    private final int lambdaParamNum; // 0 means not a lambda expression expected, 1 means "x=>", 2 means "(x,y)=>"
    private final String description;
    private final EPLExpressionParamType paramType;
    private final Class[] specificType;

    /**
     * Ctor.
     * @param lambdaParamNum number of parameters that are lambda-parameters, i.e. zero for no-lambda, or 1 for "a =&gt; ..." or
     *                       2 for "(a,b) =&gt; ..."
     * @param description parameter description
     * @param paramType parameter type
     */
    public DotMethodFPParam(int lambdaParamNum, String description, EPLExpressionParamType paramType) {
        this.lambdaParamNum = lambdaParamNum;
        this.description = description;
        this.paramType = paramType;
        this.specificType = null;
        if (paramType == EPLExpressionParamType.SPECIFIC) {
            throw new IllegalArgumentException("Invalid ctor for specific-type parameter");
        }
    }

    public DotMethodFPParam(String description, EPLExpressionParamType paramType) {
        this(description, paramType, (Class[]) null);
    }

    public DotMethodFPParam(String description, EPLExpressionParamType paramType, Class... specificType) {
        this.description = description;
        this.paramType = paramType;
        this.specificType = specificType;
        this.lambdaParamNum = 0;
    }

    public int getLambdaParamNum() {
        return lambdaParamNum;
    }

    public String getDescription() {
        return description;
    }

    public EPLExpressionParamType getParamType() {
        return paramType;
    }

    public Class[] getSpecificType() {
        return specificType;
    }
}
