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
 * Provides the environment to {@link CompilerHookOption}.
 */
public class CompilerHookContext {
    private final String moduleName;

    /**
     * Ctor.
     *
     * @param moduleName         module name or null when none provided
     */
    public CompilerHookContext(String moduleName) {
        this.moduleName = moduleName;
    }

    /**
     * Returns the module name or null when none provided
     *
     * @return module name or null when none provided
     */
    public String getModuleName() {
        return moduleName;
    }
}
