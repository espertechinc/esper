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
package com.espertech.esper.epl.script.mvel;

import com.espertech.esper.epl.spec.ExpressionScriptCompiled;

public class ExpressionScriptCompiledMVEL implements ExpressionScriptCompiled {

    private final Object compiled;      // expected type: org.mvel.ExecutableStatement

    public ExpressionScriptCompiledMVEL(Object compiled) {
        this.compiled = compiled;
    }

    public Object getCompiled() {
        return compiled;
    }

    public Class getKnownReturnType() {
        return MVELInvoker.getExecutableStatementKnownReturnType(compiled);
    }
}
