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

import com.espertech.esper.common.client.hook.expr.EPLScriptContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.script.jsr223.JSR223Helper;
import com.espertech.esper.common.internal.epl.script.mvel.MVELHelper;
import com.espertech.esper.common.internal.settings.ClasspathImportService;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.espertech.esper.common.internal.epl.script.core.ExprNodeScript.CONTEXT_BINDING_NAME;

public class ExpressionNodeScriptCompiler {
    public static ExpressionScriptCompiled compileScript(String dialect, String scriptName, String expression, String[] parameterNames, Class[] evaluationTypes, ExpressionScriptCompiled optionalPrecompiled, ClasspathImportService classpathImportService)
            throws ExprValidationException {
        ExpressionScriptCompiled compiled;
        if (dialect.toLowerCase(Locale.ENGLISH).trim().equals("mvel")) {
            Map<String, Class> mvelInputParamTypes = new HashMap<String, Class>();
            for (int i = 0; i < parameterNames.length; i++) {
                String mvelParamName = parameterNames[i];
                mvelInputParamTypes.put(mvelParamName, evaluationTypes[i]);
            }
            mvelInputParamTypes.put(CONTEXT_BINDING_NAME, EPLScriptContext.class);
            compiled = MVELHelper.compile(scriptName, expression, mvelInputParamTypes, classpathImportService);
        } else {
            if (optionalPrecompiled != null) {
                return optionalPrecompiled;
            }
            return JSR223Helper.verifyCompileScript(scriptName, expression, dialect);
        }
        return compiled;
    }

}
