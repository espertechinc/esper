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

import java.util.List;
import java.util.Map;

public interface ModuleProvider {
    String getModuleName();

    Map<ModuleProperty, Object> getModuleProperties();

    ModuleDependenciesRuntime getModuleDependencies();

    void initializeEventTypes(EPModuleEventTypeInitServices svc);

    void initializeNamedWindows(EPModuleNamedWindowInitServices svc);

    void initializeIndexes(EPModuleIndexInitServices svc);

    void initializeContexts(EPModuleContextInitServices svc);

    void initializeVariables(EPModuleVariableInitServices svc);

    void initializeExprDeclareds(EPModuleExprDeclaredInitServices svc);

    void initializeTables(EPModuleTableInitServices svc);

    void initializeScripts(EPModuleScriptInitServices svc);

    List<StatementProvider> statements();
}
