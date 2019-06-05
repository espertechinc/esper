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
package com.espertech.esper.common.internal.epl.expression.declared.compiletime;

import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.collection.PathException;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.collection.PathRegistryObjectType;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.context.module.ModuleDependenciesCompileTime;
import com.espertech.esper.common.internal.epl.util.CompileTimeResolver;

import java.util.Set;

public class ExprDeclaredCompileTimeResolverImpl implements ExprDeclaredCompileTimeResolver {
    private final String moduleName;
    private final Set<String> moduleUses;
    private final ExprDeclaredCompileTimeRegistry locals;
    private final PathRegistry<String, ExpressionDeclItem> path;
    private final ModuleDependenciesCompileTime moduleDependencies;
    private final boolean isFireAndForget;

    public ExprDeclaredCompileTimeResolverImpl(String moduleName, Set<String> moduleUses, ExprDeclaredCompileTimeRegistry locals, PathRegistry<String, ExpressionDeclItem> path, ModuleDependenciesCompileTime moduleDependencies, boolean isFireAndForget) {
        this.moduleName = moduleName;
        this.moduleUses = moduleUses;
        this.locals = locals;
        this.path = path;
        this.moduleDependencies = moduleDependencies;
        this.isFireAndForget = isFireAndForget;
    }

    public ExpressionDeclItem resolve(String name) {
        // try self-originated protected types first
        ExpressionDeclItem localExpr = locals.getExpressions().get(name);
        if (localExpr != null) {
            return localExpr;
        }
        try {
            Pair<ExpressionDeclItem, String> expression = path.getAnyModuleExpectSingle(name, moduleUses);
            if (expression != null) {
                if (!isFireAndForget && !NameAccessModifier.visible(expression.getFirst().getVisibility(), expression.getFirst().getModuleName(), moduleName)) {
                    return null;
                }

                moduleDependencies.addPathExpression(name, expression.getSecond());
                return expression.getFirst();
            }
        } catch (PathException e) {
            throw CompileTimeResolver.makePathAmbiguous(PathRegistryObjectType.EXPRDECL, name, e);
        }
        return null;
    }
}
