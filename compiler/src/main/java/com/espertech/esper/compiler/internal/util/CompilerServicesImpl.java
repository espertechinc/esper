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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClass;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClassMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClassType;
import com.espertech.esper.common.internal.compile.stage1.CompilerServices;
import com.espertech.esper.common.internal.compile.stage1.spec.StatementSpecRaw;
import com.espertech.esper.common.internal.compile.stage1.specmapper.StatementSpecMapEnv;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompileException;
import com.espertech.esper.common.internal.compile.stage3.ModuleCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.util.ByteArrayProvidingClassLoader;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CompilerServicesImpl implements CompilerServices {

    public StatementSpecRaw parseWalk(String epl, StatementSpecMapEnv mapEnv) throws StatementSpecCompileException {
        return CompilerHelperSingleEPL.parseWalk(epl, mapEnv);
    }

    public String lexSampleSQL(String querySQL) throws ExprValidationException {
        return SQLLexer.lexSampleSQL(querySQL);
    }

    public ExprNode compileExpression(String expression, StatementCompileTimeServices services)
        throws ExprValidationException {
        String toCompile = "select * from java.lang.Object#time(" + expression + ")";

        StatementSpecRaw raw;
        try {
            raw = services.getCompilerServices().parseWalk(toCompile, services.getStatementSpecMapEnv());
        } catch (StatementSpecCompileException e) {
            throw new ExprValidationException("Failed to compile expression '" + expression + "': " + e.getExpression(), e);
        }

        return raw.getStreamSpecs().get(0).getViewSpecs()[0].getObjectParameters().get(0);
    }

    public Class compileStandInClass(CodegenClassType classType, String classNameSimple, ModuleCompileTimeServices services) {
        Map<String, byte[]> classes = new HashMap<>();
        CodegenPackageScope packageScope = new CodegenPackageScope(services.getPackageName(), null, false);
        CodegenClassScope classScope = new CodegenClassScope(true, packageScope, null);
        CodegenClass clazz = new CodegenClass(classType, null, classNameSimple, classScope,
            Collections.emptyList(), null, new CodegenClassMethods(), Collections.emptyList());
        JaninoCompiler.compile(clazz, classes, services);
        ByteArrayProvidingClassLoader cl = new ByteArrayProvidingClassLoader(classes, services.getParentClassLoader());
        String classNameFull = services.getPackageName() + "." + classNameSimple;
        try {
            return Class.forName(classNameFull, false, cl);
        } catch (ClassNotFoundException e) {
            throw new EPException("Unexpected exception loading class " + classNameFull + ": " + e.getMessage(), e);
        }
    }
}
