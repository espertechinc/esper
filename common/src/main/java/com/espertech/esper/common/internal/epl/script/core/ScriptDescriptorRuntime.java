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
package com.espertech.esper.common.internal.epl.script.core;

import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.settings.ClasspathImportService;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;

public class ScriptDescriptorRuntime {
    private String optionalDialect;
    private String scriptName;
    private String expression;
    private String[] parameterNames;
    private ExprEvaluator[] parameters;
    private Class[] evaluationTypes;
    private SimpleNumberCoercer coercer;

    private String defaultDialect;
    private ClasspathImportService classpathImportService;

    public void setOptionalDialect(String optionalDialect) {
        this.optionalDialect = optionalDialect;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public void setParameterNames(String[] parameterNames) {
        this.parameterNames = parameterNames;
    }

    public void setEvaluationTypes(Class[] evaluationTypes) {
        this.evaluationTypes = evaluationTypes;
    }

    public String getOptionalDialect() {
        return optionalDialect;
    }

    public String getScriptName() {
        return scriptName;
    }

    public String getExpression() {
        return expression;
    }

    public String[] getParameterNames() {
        return parameterNames;
    }

    public Class[] getEvaluationTypes() {
        return evaluationTypes;
    }

    public String getDefaultDialect() {
        return defaultDialect;
    }

    public void setDefaultDialect(String defaultDialect) {
        this.defaultDialect = defaultDialect;
    }

    public ClasspathImportService getClasspathImportService() {
        return classpathImportService;
    }

    public void setClasspathImportService(ClasspathImportService classpathImportService) {
        this.classpathImportService = classpathImportService;
    }

    public ExprEvaluator[] getParameters() {
        return parameters;
    }

    public void setParameters(ExprEvaluator[] parameters) {
        this.parameters = parameters;
    }

    public SimpleNumberCoercer getCoercer() {
        return coercer;
    }

    public void setCoercer(SimpleNumberCoercer coercer) {
        this.coercer = coercer;
    }
}
