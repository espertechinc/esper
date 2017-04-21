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

import com.espertech.esper.epl.util.EPLExpressionParamType;

public class DotMethodFPParam {

    private final int lambdaParamNum; // 0 means not a lambda expression expected, 1 means "x=>", 2 means "(x,y)=>"
    private final String description;
    private final EPLExpressionParamType type;
    private final Class[] specificType;

    public DotMethodFPParam(int lambdaParamNum, String description, EPLExpressionParamType type) {
        this.lambdaParamNum = lambdaParamNum;
        this.description = description;
        this.type = type;
        this.specificType = null;
        if (type == EPLExpressionParamType.SPECIFIC) {
            throw new IllegalArgumentException("Invalid ctor for specific-type parameter");
        }
    }

    public DotMethodFPParam(String description, EPLExpressionParamType type) {
        this(description, type, (Class[]) null);
    }

    public DotMethodFPParam(String description, EPLExpressionParamType type, Class ... specificType) {
        this.description = description;
        this.type = type;
        this.specificType = specificType;
        this.lambdaParamNum = 0;
    }

    public int getLambdaParamNum() {
        return lambdaParamNum;
    }

    public String getDescription() {
        return description;
    }

    public EPLExpressionParamType getType() {
        return type;
    }

    public Class[] getSpecificType() {
        return specificType;
    }
}
