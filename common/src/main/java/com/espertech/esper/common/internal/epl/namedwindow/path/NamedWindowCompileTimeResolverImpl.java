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
package com.espertech.esper.common.internal.epl.namedwindow.path;

import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.collection.PathException;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.collection.PathRegistryObjectType;
import com.espertech.esper.common.internal.context.module.ModuleDependenciesCompileTime;
import com.espertech.esper.common.internal.epl.namedwindow.compile.NamedWindowCompileTimeRegistry;
import com.espertech.esper.common.internal.epl.util.CompileTimeResolver;

import java.util.Set;

public class NamedWindowCompileTimeResolverImpl implements NamedWindowCompileTimeResolver {
    private final String moduleName;
    private final Set<String> moduleUses;
    private final NamedWindowCompileTimeRegistry locals;
    private final PathRegistry<String, NamedWindowMetaData> path;
    private final ModuleDependenciesCompileTime moduleDependencies;
    private final boolean isFireAndForget;

    public NamedWindowCompileTimeResolverImpl(String moduleName, Set<String> moduleUses, NamedWindowCompileTimeRegistry locals, PathRegistry<String, NamedWindowMetaData> path, ModuleDependenciesCompileTime moduleDependencies, boolean isFireAndForget) {
        this.moduleName = moduleName;
        this.moduleUses = moduleUses;
        this.locals = locals;
        this.path = path;
        this.moduleDependencies = moduleDependencies;
        this.isFireAndForget = isFireAndForget;
    }

    public NamedWindowMetaData resolve(String namedWindowName) {
        // try self-originated protected types first
        NamedWindowMetaData localNamedWindow = locals.getNamedWindows().get(namedWindowName);
        if (localNamedWindow != null) {
            return localNamedWindow;
        }
        try {
            Pair<NamedWindowMetaData, String> pair = path.getAnyModuleExpectSingle(namedWindowName, moduleUses);
            if (pair != null) {
                if (!isFireAndForget && !NameAccessModifier.visible(pair.getFirst().getEventType().getMetadata().getAccessModifier(), pair.getFirst().getNamedWindowModuleName(), moduleName)) {
                    return null;
                }

                moduleDependencies.addPathNamedWindow(namedWindowName, pair.getSecond());
                return pair.getFirst();
            }
        } catch (PathException e) {
            throw CompileTimeResolver.makePathAmbiguous(PathRegistryObjectType.NAMEDWINDOW, namedWindowName, e);
        }
        return null;
    }
}
