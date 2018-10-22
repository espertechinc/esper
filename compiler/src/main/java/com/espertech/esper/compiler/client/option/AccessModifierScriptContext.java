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
 * Provides the environment to {@link AccessModifierScriptOption}.
 */
public class AccessModifierScriptContext extends StatementOptionContextBase {

    private final String scriptName;
    private final int numParameters;

    /**
     * Ctor.
     *
     * @param base          statement info
     * @param scriptName    script name
     * @param numParameters script number of parameters
     */
    public AccessModifierScriptContext(StatementBaseInfo base, String scriptName, int numParameters) {
        super(base);
        this.scriptName = scriptName;
        this.numParameters = numParameters;
    }

    /**
     * Returns the script name
     *
     * @return script name
     */
    public String getScriptName() {
        return scriptName;
    }

    /**
     * Returns the script number of parameters
     *
     * @return script number of parameters
     */
    public int getNumParameters() {
        return numParameters;
    }
}
