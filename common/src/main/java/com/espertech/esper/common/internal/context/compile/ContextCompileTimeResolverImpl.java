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
package com.espertech.esper.common.internal.context.compile;

import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.collection.PathException;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.collection.PathRegistryObjectType;
import com.espertech.esper.common.internal.context.module.ModuleDependenciesCompileTime;
import com.espertech.esper.common.internal.epl.util.CompileTimeResolver;

import java.util.Set;

public class ContextCompileTimeResolverImpl implements ContextCompileTimeResolver {
    private final String moduleName;
    private final Set<String> moduleUses;
    private final ContextCompileTimeRegistry locals;
    private final PathRegistry<String, ContextMetaData> path;
    private final ModuleDependenciesCompileTime moduleDependencies;
    private final boolean isFireAndForget;

    public ContextCompileTimeResolverImpl(String moduleName, Set<String> moduleUses, ContextCompileTimeRegistry locals, PathRegistry<String, ContextMetaData> path, ModuleDependenciesCompileTime moduleDependencies, boolean isFireAndForget) {
        this.moduleName = moduleName;
        this.moduleUses = moduleUses;
        this.locals = locals;
        this.path = path;
        this.moduleDependencies = moduleDependencies;
        this.isFireAndForget = isFireAndForget;
    }

    public ContextMetaData getContextInfo(String contextName) {
        // try self-originated protected types first
        ContextMetaData localContext = locals.getContexts().get(contextName);
        if (localContext != null) {
            return localContext;
        }
        try {
            Pair<ContextMetaData, String> pair = path.getAnyModuleExpectSingle(contextName, moduleUses);
            if (pair != null) {
                if (!isFireAndForget && !NameAccessModifier.visible(pair.getFirst().getContextVisibility(), pair.getFirst().getContextModuleName(), moduleName)) {
                    return null;
                }

                moduleDependencies.addPathContext(contextName, pair.getSecond());
                return pair.getFirst();
            }
        } catch (PathException e) {
            throw CompileTimeResolver.makePathAmbiguous(PathRegistryObjectType.CONTEXT, contextName, e);
        }
        return null;
    }
}
