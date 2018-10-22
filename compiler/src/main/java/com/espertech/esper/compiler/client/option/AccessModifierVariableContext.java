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
 * Provides the environment to {@link AccessModifierVariableOption}.
 */
public class AccessModifierVariableContext extends StatementOptionContextBase {

    private final String variableName;

    /**
     * Ctor
     *
     * @param base         statement info
     * @param variableName returns the variable name
     */
    public AccessModifierVariableContext(StatementBaseInfo base, String variableName) {
        super(base);
        this.variableName = variableName;
    }

    /**
     * Returns the variable name
     *
     * @return the variable name
     */
    public String getVariableName() {
        return variableName;
    }
}
