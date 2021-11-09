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
package com.espertech.esper.compiler.internal.util;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.internal.compile.compiler.CompilerAbstraction;
import com.espertech.esper.common.internal.compile.compiler.CompilerAbstractionClassCollection;
import com.espertech.esper.common.internal.compile.compiler.CompilerAbstractionCompilationContext;
import com.espertech.esper.common.internal.compile.stage3.ModuleCompileTimeServices;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

public class CompileCallable implements Callable<CompilableItemResult> {
    private final CompilableItem compilableItem;
    private final ModuleCompileTimeServices compileTimeServices;
    private final List<EPCompiled> path;
    private final Semaphore semaphore;
    private final CompilerAbstraction compilerAbstraction;
    private final CompilerAbstractionClassCollection compilationState;

    CompileCallable(CompilableItem compilableItem, ModuleCompileTimeServices compileTimeServices, List<EPCompiled> path, Semaphore semaphore, CompilerAbstraction compilerAbstraction, CompilerAbstractionClassCollection compilationState) {
        this.compilableItem = compilableItem;
        this.compileTimeServices = compileTimeServices;
        this.path = path;
        this.semaphore = semaphore;
        this.compilerAbstraction = compilerAbstraction;
        this.compilationState = compilationState;
    }

    public CompilableItemResult call() throws Exception {
        try {
            CompilerAbstractionCompilationContext context = new CompilerAbstractionCompilationContext(compileTimeServices, path);
            compilerAbstraction.compileClasses(compilableItem.getClasses(), context, compilationState);
        } catch (Throwable t) {
            return new CompilableItemResult(t);
        } finally {
            semaphore.release();
            compilableItem.getPostCompileLatch().completed(compilationState.getClasses());
        }

        return new CompilableItemResult();
    }
}
