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

import com.espertech.esper.common.client.EPCompilerPathable;
import com.espertech.esper.common.client.configuration.Configuration;

/**
 * Arguments holder for use with {@link EPCompiler#compile}.
 * <p>
 * The compiler arguments always contain a configuration. When there is no configuration provided the compiler uses the default
 * (empty) configuration.
 * </p>
 * <p>
 * The compiler path provides information on the EPL-objects that are visible at compilation time.
 * Add compiled modules and path information from runtimes to the path for modules to gain access to existing EPL objects.
 * </p>
 * <p>
 * Compiler options are callbacks as well as optional values for the compiler.
 * </p>
 */
public class CompilerArguments {
    private CompilerPath path;
    private Configuration configuration;
    private CompilerOptions options;

    /**
     * Empty constructor uses an empty {@link Configuration}
     */
    public CompilerArguments() {
        this(new Configuration());
    }

    /**
     * Ctor.
     *
     * @param configuration the compiler configuration
     */
    public CompilerArguments(Configuration configuration) {
        this.configuration = configuration;
        this.path = new CompilerPath();
        this.options = new CompilerOptions();
    }

    /**
     * Ctor.
     *
     * @param compilerPathable a compiler pathable provide path information
     */
    public CompilerArguments(EPCompilerPathable compilerPathable) {
        this();
        path.add(compilerPathable);
    }

    /**
     * Returns the path.
     *
     * @return path
     */
    public CompilerPath getPath() {
        return path;
    }

    /**
     * Sets the path
     *
     * @param path path
     * @return itself
     */
    public CompilerArguments setPath(CompilerPath path) {
        this.path = path;
        return this;
    }

    /**
     * Returns the configuration
     *
     * @return configuration
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Sets the configuration
     *
     * @param configuration to set
     * @return itself
     */
    public CompilerArguments setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    /**
     * Returns the compiler options
     *
     * @return options
     */
    public CompilerOptions getOptions() {
        return options;
    }

    /**
     * Sets the compiler options.
     *
     * @param options compiler options
     * @return itself
     */
    public CompilerArguments setOptions(CompilerOptions options) {
        this.options = options;
        return this;
    }
}
