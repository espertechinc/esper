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

/**
 * Provides the environment to {@link ModuleNameOption}.
 */
public class ModuleNameContext {
    private final String moduleNameProvided;

    /**
     * Ctor
     *
     * @param moduleNameProvided module name or null when none provided
     */
    public ModuleNameContext(String moduleNameProvided) {
        this.moduleNameProvided = moduleNameProvided;
    }

    /**
     * Returns the module name or null when none provided
     *
     * @return module name or null when none provided
     */
    public String getModuleNameProvided() {
        return moduleNameProvided;
    }
}
