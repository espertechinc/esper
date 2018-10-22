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
package com.espertech.esper.compiler.client.option;

import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;

/**
 * Provides the environment to {@link AccessModifierExpressionOption}.
 */
public class AccessModifierExpressionContext extends StatementOptionContextBase {

    private final String expressionName;

    /**
     * Ctor.
     *
     * @param base           statement info
     * @param expressionName expression name
     */
    public AccessModifierExpressionContext(StatementBaseInfo base, String expressionName) {
        super(base);
        this.expressionName = expressionName;
    }

    /**
     * Returns the expression name
     *
     * @return expression name
     */
    public String getExpressionName() {
        return expressionName;
    }
}
