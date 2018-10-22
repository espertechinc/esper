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
package com.espertech.esper.common.internal.context.controller.core;

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class ContextControllerFactoryEnv {
    private final String outermostContextName;
    private final String contextName;
    private final int nestingLevel;
    private final int numNestingLevels;

    public ContextControllerFactoryEnv(String outermostContextName, String contextName, int nestingLevel, int numNestingLevels) {
        this.outermostContextName = outermostContextName;
        this.contextName = contextName;
        this.nestingLevel = nestingLevel;
        this.numNestingLevels = numNestingLevels;
    }

    public String getOutermostContextName() {
        return outermostContextName;
    }

    public String getContextName() {
        return contextName;
    }

    public int getNestingLevel() {
        return nestingLevel;
    }

    public int getNumNestingLevels() {
        return numNestingLevels;
    }

    public CodegenExpression toExpression() {
        return newInstance(this.getClass(), constant(outermostContextName), constant(contextName), constant(nestingLevel), constant(numNestingLevels));
    }

    public boolean isLeaf() {
        return nestingLevel == numNestingLevels;
    }

    public boolean isRoot() {
        return numNestingLevels == 1 || nestingLevel == 1;
    }
}
