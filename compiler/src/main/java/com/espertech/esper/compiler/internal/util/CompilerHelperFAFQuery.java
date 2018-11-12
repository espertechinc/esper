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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClass;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompileException;
import com.espertech.esper.common.internal.compile.stage3.ModuleCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgableStmtFields;
import com.espertech.esper.common.internal.context.module.StatementFields;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodForge;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class CompilerHelperFAFQuery {
    public static String compileQuery(FAFQueryMethodForge query, String classPostfix, String packageName, Map<String, byte[]> moduleBytes, ModuleCompileTimeServices compileTimeServices) throws StatementSpecCompileException {

        String statementFieldsClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementFields.class, classPostfix);
        CodegenPackageScope packageScope = new CodegenPackageScope(packageName, statementFieldsClassName, compileTimeServices.isInstrumented());

        String queryMethodProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(FAFQueryMethodProvider.class, classPostfix);
        List<StmtClassForgable> forgablesQueryMethod = query.makeForgables(queryMethodProviderClassName, classPostfix, packageScope);

        List<StmtClassForgable> forgables = new ArrayList<>(forgablesQueryMethod);
        forgables.add(new StmtClassForgableStmtFields(statementFieldsClassName, packageScope, 0));

        // forge with statement-fields last
        List<CodegenClass> classes = new ArrayList<>(forgables.size());
        for (StmtClassForgable forgable : forgables) {
            CodegenClass clazz = forgable.forge(true);
            classes.add(clazz);
        }

        // compile with statement-field first
        classes.sort(new Comparator<CodegenClass>() {
            public int compare(CodegenClass o1, CodegenClass o2) {
                return o1.getInterfaceImplemented() == StatementFields.class ? -1 : 0;
            }
        });

        for (CodegenClass clazz : classes) {
            JaninoCompiler.compile(clazz, moduleBytes, compileTimeServices);
        }

        return queryMethodProviderClassName;
    }
}
