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
package com.espertech.esper.common.internal.compile.stage1.specmapper;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.compile.stage1.CompilerServices;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeResolver;
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredCompileTimeResolver;
import com.espertech.esper.common.internal.epl.script.compiletime.ScriptCompileTimeResolver;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeResolver;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableCompileTimeResolver;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;

public class StatementSpecMapEnv {
    private final ClasspathImportServiceCompileTime classpathImportService;
    private final VariableCompileTimeResolver variableCompileTimeResolver;
    private final Configuration configuration;
    private final ExprDeclaredCompileTimeResolver exprDeclaredCompileTimeResolver;
    private final ContextCompileTimeResolver contextCompileTimeResolver;
    private final TableCompileTimeResolver tableCompileTimeResolver;
    private final ScriptCompileTimeResolver scriptCompileTimeResolver;
    private final CompilerServices compilerServices;

    public StatementSpecMapEnv(ClasspathImportServiceCompileTime classpathImportService, VariableCompileTimeResolver variableCompileTimeResolver, Configuration configuration, ExprDeclaredCompileTimeResolver exprDeclaredCompileTimeResolver, ContextCompileTimeResolver contextCompileTimeResolver, TableCompileTimeResolver tableCompileTimeResolver, ScriptCompileTimeResolver scriptCompileTimeResolver, CompilerServices compilerServices) {
        this.classpathImportService = classpathImportService;
        this.variableCompileTimeResolver = variableCompileTimeResolver;
        this.configuration = configuration;
        this.exprDeclaredCompileTimeResolver = exprDeclaredCompileTimeResolver;
        this.contextCompileTimeResolver = contextCompileTimeResolver;
        this.tableCompileTimeResolver = tableCompileTimeResolver;
        this.scriptCompileTimeResolver = scriptCompileTimeResolver;
        this.compilerServices = compilerServices;
    }

    public ClasspathImportServiceCompileTime getClasspathImportService() {
        return classpathImportService;
    }

    public VariableCompileTimeResolver getVariableCompileTimeResolver() {
        return variableCompileTimeResolver;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public ExprDeclaredCompileTimeResolver getExprDeclaredCompileTimeResolver() {
        return exprDeclaredCompileTimeResolver;
    }

    public TableCompileTimeResolver getTableCompileTimeResolver() {
        return tableCompileTimeResolver;
    }

    public ContextCompileTimeResolver getContextCompileTimeResolver() {
        return contextCompileTimeResolver;
    }

    public ScriptCompileTimeResolver getScriptCompileTimeResolver() {
        return scriptCompileTimeResolver;
    }

    public CompilerServices getCompilerServices() {
        return compilerServices;
    }

    public boolean isAttachPatternText() {
        return configuration.getCompiler().getByteCode().isAttachPatternEPL();
    }
}
