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
package com.espertech.esper.epl.spec;

import java.io.Serializable;
import java.util.List;

public class ExpressionScriptProvided implements Serializable {

    private static final long serialVersionUID = -6815352549811750093L;
    private final String name;
    private final String expression;
    private final List<String> parameterNames;
    private final String optionalReturnTypeName;
    private final String optionalEventTypeName;
    private final boolean optionalReturnTypeIsArray;
    private final String optionalDialect;

    private transient ExpressionScriptCompiled compiled;

    public ExpressionScriptProvided(String name, String expression, List<String> parameterNames, String optionalReturnTypeName, boolean optionalReturnTypeIsArray, String optionalEventTypeName, String optionalDialect) {
        this.name = name;
        this.expression = expression;
        this.parameterNames = parameterNames;
        this.optionalReturnTypeName = optionalReturnTypeName;
        this.optionalReturnTypeIsArray = optionalReturnTypeIsArray;
        this.optionalEventTypeName = optionalEventTypeName;
        this.optionalDialect = optionalDialect;

        if (expression == null) {
            throw new IllegalArgumentException("Invalid null expression received");
        }
    }

    public String getName() {
        return name;
    }

    public String getExpression() {
        return expression;
    }

    public List<String> getParameterNames() {
        return parameterNames;
    }

    public String getOptionalReturnTypeName() {
        return optionalReturnTypeName;
    }

    public String getOptionalDialect() {
        return optionalDialect;
    }

    public ExpressionScriptCompiled getCompiled() {
        return compiled;
    }

    public boolean isOptionalReturnTypeIsArray() {
        return optionalReturnTypeIsArray;
    }

    public void setCompiled(ExpressionScriptCompiled compiled) {
        this.compiled = compiled;
    }

    public String getOptionalEventTypeName() {
        return optionalEventTypeName;
    }
}
