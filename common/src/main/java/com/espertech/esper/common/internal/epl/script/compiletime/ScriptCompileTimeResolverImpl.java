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
package com.espertech.esper.common.internal.epl.script.compiletime;

import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.collection.PathException;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.collection.PathRegistryObjectType;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.context.module.ModuleDependenciesCompileTime;
import com.espertech.esper.common.internal.epl.script.core.NameAndParamNum;
import com.espertech.esper.common.internal.epl.util.CompileTimeResolver;

import java.util.Set;

public class ScriptCompileTimeResolverImpl implements ScriptCompileTimeResolver {
    private final String moduleName;
    private final Set<String> moduleUses;
    private final ScriptCompileTimeRegistry locals;
    private final PathRegistry<NameAndParamNum, ExpressionScriptProvided> path;
    private final ModuleDependenciesCompileTime moduleDependencies;
    private final boolean isFireAndForget;

    public ScriptCompileTimeResolverImpl(String moduleName, Set<String> moduleUses, ScriptCompileTimeRegistry locals, PathRegistry<NameAndParamNum, ExpressionScriptProvided> path, ModuleDependenciesCompileTime moduleDependencies, boolean isFireAndForget) {
        this.moduleName = moduleName;
        this.moduleUses = moduleUses;
        this.locals = locals;
        this.path = path;
        this.moduleDependencies = moduleDependencies;
        this.isFireAndForget = isFireAndForget;
    }

    public ExpressionScriptProvided resolve(String name, int numParameters) {
        NameAndParamNum key = new NameAndParamNum(name, numParameters);

        // try self-originated protected types first
        ExpressionScriptProvided localExpr = locals.getScripts().get(key);
        if (localExpr != null) {
            return localExpr;
        }
        try {
            Pair<ExpressionScriptProvided, String> expression = path.getAnyModuleExpectSingle(new NameAndParamNum(name, numParameters), moduleUses);
            if (expression != null) {
                if (!isFireAndForget && !NameAccessModifier.visible(expression.getFirst().getVisibility(), expression.getFirst().getModuleName(), moduleName)) {
                    return null;
                }

                moduleDependencies.addPathScript(key, expression.getSecond());
                return expression.getFirst();
            }
        } catch (PathException e) {
            throw CompileTimeResolver.makePathAmbiguous(PathRegistryObjectType.SCRIPT, name, e);
        }
        return null;
    }
}
