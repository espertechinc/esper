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

import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClass;
import com.espertech.esper.common.internal.compile.stage3.ModuleCompileTimeServices;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class CompileCallable implements Callable<CompilableItemResult> {
    private final CompilableItem compilableItem;
    private final ModuleCompileTimeServices compileTimeServices;
    private final Semaphore semaphore;
    private final ConcurrentHashMap<String, byte[]> statementBytes;

    CompileCallable(CompilableItem compilableItem, ModuleCompileTimeServices compileTimeServices, Semaphore semaphore, ConcurrentHashMap<String, byte[]> statementBytes) {
        this.compilableItem = compilableItem;
        this.compileTimeServices = compileTimeServices;
        this.semaphore = semaphore;
        this.statementBytes = statementBytes;
    }

    public CompilableItemResult call() throws Exception {
        try {
            for (CodegenClass clazz : compilableItem.getClasses()) {
                JaninoCompiler.compile(clazz, statementBytes, compileTimeServices);
            }
        } catch (Throwable t) {
            return new CompilableItemResult(t);
        } finally {
            semaphore.release();
            compilableItem.getPostCompileLatch().completed(statementBytes);
        }

        return new CompilableItemResult();
    }
}
