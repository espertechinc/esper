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

import com.espertech.esper.common.internal.collection.NameParameterCountKey;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclDesc;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.script.jsr223.JSR223Helper;
import com.espertech.esper.common.internal.epl.script.mvel.MVELHelper;
import com.espertech.esper.common.internal.epl.script.mvel.MVELInvoker;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ScriptValidationPrecompileUtil {
    // All scripts get compiled/verfied - to ensure they are compiled once and not multiple times.
    public static void validateScripts(List<ExpressionScriptProvided> scripts, ExpressionDeclDesc expressionDeclDesc, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        if (scripts == null) {
            return;
        }
        String defaultDialect = compileTimeServices.getConfiguration().getCompiler().getScripts().getDefaultDialect();
        Set<NameParameterCountKey> scriptsSet = new HashSet<NameParameterCountKey>();
        for (ExpressionScriptProvided script : scripts) {
            validateScript(script, defaultDialect, compileTimeServices.getClasspathImportServiceCompileTime());

            NameParameterCountKey key = new NameParameterCountKey(script.getName(), script.getParameterNames().length);
            if (scriptsSet.contains(key)) {
                throw new ExprValidationException("Script name '" + script.getName() + "' has already been defined with the same number of parameters");
            }
            scriptsSet.add(key);
        }

        if (expressionDeclDesc != null) {
            for (ExpressionDeclItem declItem : expressionDeclDesc.getExpressions()) {
                if (scriptsSet.contains(new NameParameterCountKey(declItem.getName(), 0))) {
                    throw new ExprValidationException("Script name '" + declItem.getName() + "' overlaps with another expression of the same name");
                }
            }
        }
    }

    private static void validateScript(ExpressionScriptProvided script, String defaultDialect, ClasspathImportServiceCompileTime classpathImportService) throws ExprValidationException {
        String dialect = script.getOptionalDialect() == null ? defaultDialect : script.getOptionalDialect();
        if (dialect == null) {
            throw new ExprValidationException("Failed to determine script dialect for script '" + script.getName() + "', please configure a default dialect or provide a dialect explicitly");
        }

        ExpressionScriptCompiled compiledBuf;
        if (dialect.trim().toLowerCase(Locale.ENGLISH).equals("mvel")) {
            if (!MVELInvoker.isMVELInClasspath(classpathImportService)) {
                throw new ExprValidationException("MVEL scripting runtime not found in classpath, script dialect 'mvel' requires mvel in classpath for script '" + script.getName() + "'");
            }
            MVELHelper.verifyScript(script, classpathImportService);
            compiledBuf = null;
        } else {
            compiledBuf = JSR223Helper.verifyCompileScript(script.getName(), script.getExpression(), dialect);
        }
        script.setCompiledBuf(compiledBuf);

        if (script.getParameterNames().length != 0) {
            HashSet<String> parameters = new HashSet<String>();
            for (String param : script.getParameterNames()) {
                if (parameters.contains(param)) {
                    throw new ExprValidationException("Invalid script parameters for script '" + script.getName() + "', parameter '" + param + "' is defined more then once");
                }
                parameters.add(param);
            }
        }
    }
}
