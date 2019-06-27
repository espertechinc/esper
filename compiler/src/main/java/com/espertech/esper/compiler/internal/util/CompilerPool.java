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

import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerByteCode;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClass;
import com.espertech.esper.common.internal.compile.stage3.ModuleCompileTimeServices;
import com.espertech.esper.compiler.client.EPCompileException;

import java.util.concurrent.*;

class CompilerPool {
    private final ModuleCompileTimeServices compileTimeServices;
    private final ConcurrentHashMap<String, byte[]> moduleBytes;

    private ExecutorService compilerThreadPool;
    private Future<CompilableItemResult>[] futures;
    private Semaphore semaphore;

    CompilerPool(int size, ModuleCompileTimeServices compileTimeServices, ConcurrentHashMap<String, byte[]> moduleBytes) {
        this.compileTimeServices = compileTimeServices;
        this.moduleBytes = moduleBytes;

        ConfigurationCompilerByteCode config = compileTimeServices.getConfiguration().getCompiler().getByteCode();
        int numThreads = config.getThreadPoolCompilerNumThreads();
        if (numThreads > 0 && size > 1) {
            compilerThreadPool = Executors.newFixedThreadPool(numThreads);
            futures = new Future[size];

            Integer capacity = config.getThreadPoolCompilerCapacity();
            semaphore = new Semaphore(capacity == null ? Integer.MAX_VALUE : Math.max(1, capacity));
        }
    }

    void submit(int statementNumber, CompilableItem item) throws InterruptedException {
        // no thread pool, compile right there
        if (compilerThreadPool == null) {
            try {
                for (CodegenClass clazz : item.getClasses()) {
                    JaninoCompiler.compile(clazz, moduleBytes, compileTimeServices);
                }
            } finally {
                item.getPostCompileLatch().completed(moduleBytes);
            }
            return;
        }

        CompileCallable callable = new CompileCallable(item, compileTimeServices, semaphore, moduleBytes);
        semaphore.acquire();
        futures[statementNumber] = compilerThreadPool.submit(callable);
    }

    void shutdownCollectResults() throws EPCompileException {
        if (compilerThreadPool == null) {
            return;
        }

        compilerThreadPool.shutdown();
        try {
            compilerThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for (Future<CompilableItemResult> future : futures) {
            if (future == null) {
                continue;
            }
            CompilableItemResult result;
            try {
                result = future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new EPCompileException(e.getMessage(), e);
            }
            if (result.getException() != null) {
                throw new EPCompileException(result.getException().getMessage(), result.getException());
            }
        }
    }

    void shutdownNow() {
        if (compilerThreadPool == null) {
            return;
        }
        compilerThreadPool.shutdownNow();
    }

    public void shutdown() {
        if (compilerThreadPool == null) {
            return;
        }
        compilerThreadPool.shutdown();
    }
}
