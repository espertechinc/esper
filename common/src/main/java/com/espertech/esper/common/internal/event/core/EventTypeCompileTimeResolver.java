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
package com.espertech.esper.common.internal.event.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.collection.PathException;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.collection.PathRegistryObjectType;
import com.espertech.esper.common.internal.context.module.EventTypeCompileTimeRegistry;
import com.espertech.esper.common.internal.context.module.ModuleDependenciesCompileTime;
import com.espertech.esper.common.internal.epl.util.CompileTimeResolver;
import com.espertech.esper.common.internal.event.eventtyperepo.EventTypeRepositoryImpl;

import java.util.Set;

public class EventTypeCompileTimeResolver implements EventTypeNameResolver, CompileTimeResolver {
    private final String moduleName;
    private final Set<String> moduleUses;
    private final EventTypeCompileTimeRegistry locals;
    private final EventTypeRepositoryImpl publics;
    private final PathRegistry<String, EventType> path;
    private final ModuleDependenciesCompileTime moduleDependencies;
    private final boolean isFireAndForget;

    public EventTypeCompileTimeResolver(String moduleName, Set<String> moduleUses, EventTypeCompileTimeRegistry locals, EventTypeRepositoryImpl publics, PathRegistry<String, EventType> path, ModuleDependenciesCompileTime moduleDependencies, boolean isFireAndForget) {
        this.moduleName = moduleName;
        this.moduleUses = moduleUses;
        this.locals = locals;
        this.publics = publics;
        this.path = path;
        this.moduleDependencies = moduleDependencies;
        this.isFireAndForget = isFireAndForget;
    }

    public PathRegistry<String, EventType> getPath() {
        return path;
    }

    public EventType getTypeByName(String typeName) {
        EventType local = locals.getModuleTypes(typeName);
        EventType path = resolvePath(typeName);
        EventType preconfigured = resolvePreconfigured(typeName);
        return CompileTimeResolver.validateAmbiguous(local, path, preconfigured, PathRegistryObjectType.EVENTTYPE, typeName);
    }

    private EventType resolvePreconfigured(String typeName) {
        EventType eventType = publics.getTypeByName(typeName);
        if (eventType == null) {
            return null;
        }
        moduleDependencies.addPublicEventType(typeName);
        return eventType;
    }

    private EventType resolvePath(String typeName) {
        try {
            Pair<EventType, String> typeAndModule = path.getAnyModuleExpectSingle(typeName, moduleUses);
            if (typeAndModule == null) {
                return null;
            }

            if (!isFireAndForget && !NameAccessModifier.visible(typeAndModule.getFirst().getMetadata().getAccessModifier(), typeAndModule.getSecond(), moduleName)) {
                return null;
            }

            moduleDependencies.addPathEventType(typeName, typeAndModule.getSecond());
            return typeAndModule.getFirst();
        } catch (PathException e) {
            throw CompileTimeResolver.makePathAmbiguous(PathRegistryObjectType.EVENTTYPE, typeName, e);
        }
    }
}
