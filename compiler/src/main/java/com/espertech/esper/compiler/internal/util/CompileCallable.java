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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

public class CompileCallable implements Callable<CompilableItemResult> {
    private final CompilableItem compilableItem;
    private final ModuleCompileTimeServices compileTimeServices;
    private final Semaphore semaphore;
    private final Map<String, byte[]> statementBytes = new HashMap<>();

    CompileCallable(CompilableItem compilableItem, ModuleCompileTimeServices compileTimeServices, Semaphore semaphore) {
        this.compilableItem = compilableItem;
        this.compileTimeServices = compileTimeServices;
        this.semaphore = semaphore;
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
        }

        return new CompilableItemResult(statementBytes);
    }
}
