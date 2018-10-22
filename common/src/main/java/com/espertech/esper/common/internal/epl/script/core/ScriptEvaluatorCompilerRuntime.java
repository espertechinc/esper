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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.script.jsr223.ExpressionScriptCompiledJSR223;
import com.espertech.esper.common.internal.epl.script.jsr223.ScriptEvaluatorJSR223;
import com.espertech.esper.common.internal.epl.script.mvel.ExpressionScriptCompiledMVEL;
import com.espertech.esper.common.internal.epl.script.mvel.ScriptEvaluatorMVEL;

public class ScriptEvaluatorCompilerRuntime {
    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param descriptor descriptor
     * @return evaluator
     */
    public static ScriptEvaluator compileScriptEval(ScriptDescriptorRuntime descriptor) {
        String dialect = descriptor.getOptionalDialect() == null ? descriptor.getDefaultDialect() : descriptor.getOptionalDialect();
        ExpressionScriptCompiled compiled;
        try {
            compiled = ExpressionNodeScriptCompiler.compileScript(dialect, descriptor.getScriptName(), descriptor.getExpression(), descriptor.getParameterNames(), descriptor.getEvaluationTypes(), null, descriptor.getClasspathImportService());
        } catch (ExprValidationException ex) {
            throw new EPException("Failed to compile script '" + descriptor.getScriptName() + "': " + ex.getMessage());
        }

        if (compiled instanceof ExpressionScriptCompiledJSR223) {
            ExpressionScriptCompiledJSR223 jsr223 = (ExpressionScriptCompiledJSR223) compiled;
            return new ScriptEvaluatorJSR223(descriptor.getScriptName(), descriptor.getParameterNames(), descriptor.getParameters(), descriptor.getCoercer(), jsr223.getCompiled());
        }

        ExpressionScriptCompiledMVEL mvel = (ExpressionScriptCompiledMVEL) compiled;
        return new ScriptEvaluatorMVEL(descriptor.getScriptName(), descriptor.getParameterNames(), descriptor.getParameters(), descriptor.getCoercer(), mvel.getCompiled());
    }
}
