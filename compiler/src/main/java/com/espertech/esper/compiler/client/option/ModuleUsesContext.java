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
package com.espertech.esper.compiler.client.option;

import java.util.Set;

/**
 * Provides the environment to {@link ModuleUsesOption}.
 */
public class ModuleUsesContext {
    private final String moduleName;
    private final Set<String> moduleUsesProvided;

    /**
     * Ctor.
     *
     * @param moduleName         module name or null when none provided
     * @param moduleUsesProvided module uses or null when none provided
     */
    public ModuleUsesContext(String moduleName, Set<String> moduleUsesProvided) {
        this.moduleName = moduleName;
        this.moduleUsesProvided = moduleUsesProvided;
    }

    /**
     * Returns the module name or null when none provided
     *
     * @return module name or null when none provided
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Returns the module uses or null when none provided
     *
     * @return module uses
     */
    public Set<String> getModuleUsesProvided() {
        return moduleUsesProvided;
    }
}
