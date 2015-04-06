/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.script.mvel;

import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.script.ExprNodeScript;
import com.espertech.esper.epl.spec.ExpressionScriptCompiled;
import com.espertech.esper.epl.spec.ExpressionScriptProvided;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * =============
 * Without MVEL dependencies in classpath, this call verifies and compiles an MVEL script.
 * =============
 */
public class MVELHelper {

    private static final Log log = LogFactory.getLog(MVELHelper.class);

    /**
     * Verify MVEL script (not compiling it).
     * Compiling is done by the first expression node using the expression since only then parameter types can be bound.
     * @param script to verify/analyze
     * @throws ExprValidationException when not all parameters are resolved
     */
    public static void verifyScript(ExpressionScriptProvided script) throws ExprValidationException {

        // Reflective invocation - do not add MVEL to classpath
        Object parserContext = MVELInvoker.newParserContext();

        // this populates the parser context with the actual undefined MVEL input parameters expected
        try {
            MVELInvoker.analysisCompile(script.getExpression(), parserContext);
        }
        catch (InvocationTargetException ex) {
            throw handleTargetException(script.getName(), ex);
        }
        catch (Exception ex) {
            throw handleGeneralException(script.getName(), ex);
        }

        // obtain input params
        Map<String, Class> scriptRequiredInputs = MVELInvoker.getParserContextInputs(parserContext);

        // ensure each of the input params (excluding 'epl' context) is provided as a parameter
        for (Map.Entry<String, Class> input : scriptRequiredInputs.entrySet()) {
            if (input.getKey().toLowerCase().trim().equals(ExprNodeScript.CONTEXT_BINDING_NAME)) {
                continue;
            }
            if (script.getParameterNames().contains(input.getKey())) {
                continue;
            }
            throw new ExprValidationException("For script '" + script.getName() + "' the variable '" + input.getKey() + "' has not been declared and is not a parameter");
        }
    }

    public static ExpressionScriptCompiled compile(String scriptName, String expression, Map<String, Class> mvelInputParamTypes)
        throws ExprValidationException {

        // Reflective invocation - do not add MVEL to classpath
        Object parserContext = MVELInvoker.newParserContext();
        MVELInvoker.setParserContextStrongTyping(parserContext);
        MVELInvoker.setParserContextInputs(parserContext, mvelInputParamTypes);

        Object executable;
        try {
            executable = MVELInvoker.compileExpression(expression, parserContext);
        }
        catch (InvocationTargetException ex) {
            throw handleTargetException(scriptName, ex);
        }
        catch (Exception ex) {
            throw handleGeneralException(scriptName, ex);
        }

        return new ExpressionScriptCompiledMVEL(executable);
    }

    private static ExprValidationException handleTargetException(String scriptName, InvocationTargetException ex) {
        Throwable mvelException = ex.getTargetException();
        String message = "Exception compiling MVEL script '" + scriptName + "': " + mvelException.getMessage();
        log.info(message, mvelException);
        return new ExprValidationException(message, mvelException);
    }

    private static ExprValidationException handleGeneralException(String scriptName, Exception ex) {
        String message = "Exception compiling MVEL script '" + scriptName + "': " + ex.getMessage();
        log.info(message, ex);
        return new ExprValidationException(message, ex);
    }
}
