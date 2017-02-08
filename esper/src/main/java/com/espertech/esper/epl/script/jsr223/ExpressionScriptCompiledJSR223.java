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
package com.espertech.esper.epl.script.jsr223;

import com.espertech.esper.epl.spec.ExpressionScriptCompiled;

import javax.script.CompiledScript;

public class ExpressionScriptCompiledJSR223 implements ExpressionScriptCompiled {
    private final CompiledScript compiled;

    public ExpressionScriptCompiledJSR223(CompiledScript compiled) {
        this.compiled = compiled;
    }

    public CompiledScript getCompiled() {
        return compiled;
    }

    public Class getKnownReturnType() {
        return null;
    }
}
