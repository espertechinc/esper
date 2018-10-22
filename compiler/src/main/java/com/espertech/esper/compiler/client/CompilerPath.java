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
package com.espertech.esper.compiler.client;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPCompilerPathable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The compiler uses the path to determine the EPL-objects available to the module.
 * <p>
 * Visibility can be established by adding a compiled module
 * or by adding a {@link EPCompilerPathable} that can be obtained from a runtime.
 * </p>
 */
public class CompilerPath {
    private final List<EPCompiled> compileds = new ArrayList<>();
    private final List<EPCompilerPathable> compilerPathables = new ArrayList<>();

    /**
     * Add a compiled module
     *
     * @param compiled compiled module
     * @return itself
     */
    public CompilerPath add(EPCompiled compiled) {
        compileds.add(compiled);
        return this;
    }

    /**
     * Add all compiled modules
     *
     * @param compiledColl compiled module collection
     * @return tself
     */
    public CompilerPath addAll(Collection<EPCompiled> compiledColl) {
        compileds.addAll(compiledColl);
        return this;
    }

    /**
     * Returns the compiled modules in path.
     *
     * @return compiled modules
     */
    public List<EPCompiled> getCompileds() {
        return compileds;
    }

    /**
     * Adds a path object that can be obtains from a runtime.
     *
     * @param pathable runtime path information
     * @return itself
     */
    public CompilerPath add(EPCompilerPathable pathable) {
        compilerPathables.add(pathable);
        return this;
    }

    /**
     * Returns the path information provided by runtimes.
     *
     * @return path information provided by runtimes.
     */
    public List<EPCompilerPathable> getCompilerPathables() {
        return compilerPathables;
    }
}
