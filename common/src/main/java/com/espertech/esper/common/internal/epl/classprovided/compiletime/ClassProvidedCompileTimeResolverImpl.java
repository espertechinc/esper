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
package com.espertech.esper.common.internal.epl.classprovided.compiletime;

import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.collection.PathException;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.collection.PathRegistryObjectType;
import com.espertech.esper.common.internal.context.module.ModuleDependenciesCompileTime;
import com.espertech.esper.common.internal.epl.classprovided.core.ClassProvided;
import com.espertech.esper.common.internal.epl.util.CompileTimeResolver;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class ClassProvidedCompileTimeResolverImpl implements ClassProvidedCompileTimeResolver {
    private final String moduleName;
    private final Set<String> moduleUses;
    private final ClassProvidedCompileTimeRegistry locals;
    private final PathRegistry<String, ClassProvided> path;
    private final ModuleDependenciesCompileTime moduleDependencies;
    private final boolean isFireAndForget;

    public ClassProvidedCompileTimeResolverImpl(String moduleName, Set<String> moduleUses, ClassProvidedCompileTimeRegistry locals, PathRegistry<String, ClassProvided> path, ModuleDependenciesCompileTime moduleDependencies, boolean isFireAndForget) {
        this.moduleName = moduleName;
        this.moduleUses = moduleUses;
        this.locals = locals;
        this.path = path;
        this.moduleDependencies = moduleDependencies;
        this.isFireAndForget = isFireAndForget;
    }

    public ClassProvided resolve(String name) {
        // try self-originated protected types first
        ClassProvided localExpr = locals.getClasses().get(name);
        if (localExpr != null) {
            return localExpr;
        }
        try {
            Pair<ClassProvided, String> expression = path.getAnyModuleExpectSingle(name, moduleUses);
            if (expression != null) {
                if (!isFireAndForget && !NameAccessModifier.visible(expression.getFirst().getVisibility(), expression.getFirst().getModuleName(), moduleName)) {
                    return null;
                }

                moduleDependencies.addPathClass(name, expression.getSecond());
                return expression.getFirst();
            }
        } catch (PathException e) {
            throw CompileTimeResolver.makePathAmbiguous(PathRegistryObjectType.CLASSPROVIDED, name, e);
        }
        return null;
    }

    public boolean isEmpty() {
        return path.isEmpty() && locals.getClasses().isEmpty();
    }

    public void addTo(Map<String, byte[]> additionalClasses) {
        path.traverse(cp -> additionalClasses.putAll(cp.getBytes()));
    }

    public void removeFrom(Map<String, byte[]> moduleBytes) {
        Consumer<ClassProvided> classProvidedByteCodeRemover = item -> {
            for (Map.Entry<String, byte[]> entry : item.getBytes().entrySet()) {
                moduleBytes.remove(entry.getKey());
            }
        };
        path.traverse(classProvidedByteCodeRemover);
    }
}
