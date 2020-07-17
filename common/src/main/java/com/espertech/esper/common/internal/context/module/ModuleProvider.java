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
package com.espertech.esper.common.internal.context.module;

import com.espertech.esper.common.client.module.ModuleProperty;
import com.espertech.esper.common.client.type.EPTypeClass;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface ModuleProvider {
    EPTypeClass EPTYPE = new EPTypeClass(ModuleProvider.class);

    default String getModuleName() {
        return null;
    }

    default Map<ModuleProperty, Object> getModuleProperties() {
        return Collections.emptyMap();
    }

    ModuleDependenciesRuntime getModuleDependencies();

    default void initializeEventTypes(EPModuleEventTypeInitServices svc) {
    }

    default void initializeNamedWindows(EPModuleNamedWindowInitServices svc) {
    }

    default void initializeIndexes(EPModuleIndexInitServices svc) {
    }

    default void initializeContexts(EPModuleContextInitServices svc) {
    }

    default void initializeVariables(EPModuleVariableInitServices svc) {
    }

    default void initializeExprDeclareds(EPModuleExprDeclaredInitServices svc) {
    }

    default void initializeTables(EPModuleTableInitServices svc) {
    }

    default void initializeScripts(EPModuleScriptInitServices svc) {
    }

    default void initializeClassProvided(EPModuleClassProvidedInitServices svc) {
    }

    List<StatementProvider> statements();
}
