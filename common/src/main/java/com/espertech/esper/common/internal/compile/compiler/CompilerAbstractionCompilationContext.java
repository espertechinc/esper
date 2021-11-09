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
package com.espertech.esper.common.internal.compile.compiler;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.internal.compile.stage3.ModuleCompileTimeServices;
import com.espertech.esper.common.internal.context.util.ParentClassLoader;

import java.util.List;
import java.util.function.Consumer;

public class CompilerAbstractionCompilationContext {
    private final ModuleCompileTimeServices services;
    private final Consumer<Object> compileResultConsumer;
    private final List<EPCompiled> path;

    public CompilerAbstractionCompilationContext(ModuleCompileTimeServices services, Consumer<Object> compileResultConsumer, List<EPCompiled> path) {
        this.services = services;
        this.compileResultConsumer = compileResultConsumer;
        this.path = path;
    }

    public CompilerAbstractionCompilationContext(ModuleCompileTimeServices services, List<EPCompiled> path) {
        this(services, null, path);
    }

    public ParentClassLoader getParentClassLoader() {
        return services.getParentClassLoader();
    }

    public boolean isLogging() {
        return services.getConfiguration().getCompiler().getLogging().isEnableCode();
    }

    public ModuleCompileTimeServices getServices() {
        return services;
    }

    public Consumer<Object> getCompileResultConsumer() {
        return compileResultConsumer;
    }

    public String getGeneratedCodePackageName() {
        return services.getPackageName();
    }

    public List<EPCompiled> getPath() {
        return path;
    }
}
