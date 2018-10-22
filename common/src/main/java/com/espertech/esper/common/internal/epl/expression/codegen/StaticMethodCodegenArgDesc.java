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
package com.espertech.esper.common.internal.epl.expression.codegen;

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

public class StaticMethodCodegenArgDesc {
    private final String blockRefName;
    private final Class declareType;
    private final CodegenExpression argExpression;

    public StaticMethodCodegenArgDesc(String blockRefName, Class declareType, CodegenExpression argExpression) {
        this.blockRefName = blockRefName;
        this.declareType = declareType;
        this.argExpression = argExpression;
    }

    public String getBlockRefName() {
        return blockRefName;
    }

    public Class getDeclareType() {
        return declareType;
    }

    public CodegenExpression getArgExpression() {
        return argExpression;
    }
}
