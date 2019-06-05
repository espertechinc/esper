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
package com.espertech.esper.common.internal.epl.variable.compiletime;

import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.collection.PathException;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.collection.PathRegistryObjectType;
import com.espertech.esper.common.internal.context.module.ModuleDependenciesCompileTime;
import com.espertech.esper.common.internal.epl.util.CompileTimeResolver;
import com.espertech.esper.common.internal.epl.variable.core.VariableRepositoryPreconfigured;

import java.util.Set;

public class VariableCompileTimeResolverImpl implements VariableCompileTimeResolver {
    private final String moduleName;
    private final Set<String> moduleUses;
    private final VariableRepositoryPreconfigured publicVariables;
    private final VariableCompileTimeRegistry compileTimeRegistry;
    private final PathRegistry<String, VariableMetaData> pathVariables;
    private final ModuleDependenciesCompileTime moduleDependencies;
    private final boolean isFireAndForget;

    public VariableCompileTimeResolverImpl(String moduleName, Set<String> moduleUses, VariableRepositoryPreconfigured publicVariables, VariableCompileTimeRegistry compileTimeRegistry, PathRegistry<String, VariableMetaData> pathVariables, ModuleDependenciesCompileTime moduleDependencies, boolean isFireAndForget) {
        this.moduleName = moduleName;
        this.moduleUses = moduleUses;
        this.publicVariables = publicVariables;
        this.compileTimeRegistry = compileTimeRegistry;
        this.pathVariables = pathVariables;
        this.moduleDependencies = moduleDependencies;
        this.isFireAndForget = isFireAndForget;
    }

    public VariableMetaData resolve(String variableName) {
        VariableMetaData local = compileTimeRegistry.getVariable(variableName);
        VariableMetaData path = resolvePath(variableName);
        VariableMetaData preconfigured = resolvePreconfigured(variableName);

        return CompileTimeResolver.validateAmbiguous(local, path, preconfigured, PathRegistryObjectType.VARIABLE, variableName);
    }

    private VariableMetaData resolvePreconfigured(String variableName) {
        VariableMetaData metadata = publicVariables.getMetadata(variableName);
        if (metadata == null) {
            return null;
        }
        moduleDependencies.addPublicVariable(variableName);
        return metadata;
    }

    private VariableMetaData resolvePath(String variableName) {
        try {
            Pair<VariableMetaData, String> pair = pathVariables.getAnyModuleExpectSingle(variableName, moduleUses);
            if (pair == null) {
                return null;
            }

            if (!isFireAndForget && !NameAccessModifier.visible(pair.getFirst().getVariableVisibility(), pair.getFirst().getVariableModuleName(), moduleName)) {
                return null;
            }

            moduleDependencies.addPathVariable(variableName, pair.getSecond());
            return pair.getFirst();
        } catch (PathException e) {
            throw CompileTimeResolver.makePathAmbiguous(PathRegistryObjectType.VARIABLE, variableName, e);
        }
    }
}
