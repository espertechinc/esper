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
package com.espertech.esper.compiler.internal.util;

import com.espertech.esper.common.internal.compile.compiler.CompilerAbstractionClassCollection;
import com.espertech.esper.common.internal.compile.compiler.CompilerAbstractionCompilationContext;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClass;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompileException;
import com.espertech.esper.common.internal.compile.stage3.ModuleCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableStmtFields;
import com.espertech.esper.common.internal.context.module.StatementFields;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodForge;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodProvider;
import com.espertech.esper.compiler.client.CompilerPath;

import java.util.ArrayList;
import java.util.List;

public class CompilerHelperFAFQuery {
    public static String compileQuery(FAFQueryMethodForge query, String classPostfix, CompilerAbstractionClassCollection compilerState, ModuleCompileTimeServices compileTimeServices, CompilerPath path) throws StatementSpecCompileException {

        String statementFieldsClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementFields.class, classPostfix);
        CodegenPackageScope packageScope = new CodegenPackageScope(compileTimeServices.getPackageName(), statementFieldsClassName, compileTimeServices.isInstrumented(), compileTimeServices.getConfiguration().getCompiler().getByteCode());

        String queryMethodProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(FAFQueryMethodProvider.class, classPostfix);
        List<StmtClassForgeable> forgeablesQueryMethod = query.makeForgeables(queryMethodProviderClassName, classPostfix, packageScope);

        List<StmtClassForgeable> forgeables = new ArrayList<>(forgeablesQueryMethod);
        forgeables.add(new StmtClassForgeableStmtFields(statementFieldsClassName, packageScope));

        // forge with statement-fields last
        List<CodegenClass> classes = new ArrayList<>(forgeables.size());
        for (StmtClassForgeable forgeable : forgeables) {
            CodegenClass clazz = forgeable.forge(true, true);
            if (clazz == null) {
                continue;
            }
            classes.add(clazz);
        }

        // compile with statement-field first
        classes.sort((o1, o2) -> Integer.compare(o1.getClassType().getSortCode(), o2.getClassType().getSortCode()));

        // remove statement field initialization when unused
        packageScope.rewriteStatementFieldUse(classes);

        // add class-provided create-class to classpath
        compileTimeServices.getClassProvidedCompileTimeResolver().addTo(compilerState::add);

        CompilerAbstractionCompilationContext ctx = new CompilerAbstractionCompilationContext(compileTimeServices, path.getCompileds());
        compileTimeServices.getCompilerAbstraction().compileClasses(classes, ctx, compilerState);

        // remove path create-class class-provided byte code
        compileTimeServices.getClassProvidedCompileTimeResolver().removeFrom(compilerState::remove);

        return queryMethodProviderClassName;
    }
}
