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
package com.espertech.esper.regressionlib.suite.client.compile;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.util.ClassLoaderProvider;
import com.espertech.esper.common.internal.context.module.ModuleProvider;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.compiler.client.option.CompilerPathCache;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ClientCompilePathCache {
    private final static String EPL_PROVIDE = "@public create variable int myvariable = 10;\n" +
            "@public create schema MySchema();\n" +
            "@public create expression myExpr { 'abc' };\n" +
            "@public create window MyWindow#keepall as SupportBean_S0;\n" +
            "@public create table MyTable(y string);\n" +
            "@public create context MyContext start SupportBean_S0 end SupportBean_S1;\n" +
            "@public create expression myScript() [ 2 ];\n" +
            "@public create inlined_class \"\"\" public class MyClass { public static String doIt(String parameter) { return \"def\"; } }\"\"\";\n" +
            "@public @buseventtype create json schema CarLocUpdateEvent(carId string, direction int);\n";
    private final static String EPL_CONSUME = "@name('s0') select myvariable as c0, myExpr() as c1, myScript() as c2," +
            "MyClass.doIt(theString) as c4 from SupportBean;\n" +
            "select * from MySchema;" +
            "on SupportBean_S1 delete from MyWindow;\n" +
            "on SupportBean_S1 delete from MyTable;\n" +
            "context MyContext select * from SupportBean;\n" +
            "select carId, direction, count(*) as cnt from CarLocUpdateEvent(direction = 1)#time(1 min);\n";

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientCompilePathCacheObjectTypes());
        execs.add(new ClientCompilePathCacheProtected());
        execs.add(new ClientCompilePathCacheFillByCompile());
        execs.add(new ClientCompilePathCacheEventTypeChain());
        execs.add(new ClientCompilePathCacheInvalid());
        return execs;
    }

    private static class ClientCompilePathCacheEventTypeChain implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            CompilerPathCache cache = CompilerPathCache.getInstance();
            List<EPCompiled> path = new ArrayList<>();
            compileAdd(env, cache, path, new SupportModuleLoadDetector(), "@public create schema L0()");

            for (int i = 1; i < 10; i++) {
                String epl = String.format("@public create schema L%d(l%d L%d)", i, i - 1, i - 1);
                compileAdd(env, cache, path, new SupportModuleLoadDetector(), epl);
            }
        }
    }

    private static class ClientCompilePathCacheInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            assertDuplicate(env, false);
            assertDuplicate(env, true);
        }

        private void assertDuplicate(RegressionEnvironment env, boolean withCache) {
            CompilerPathCache cache = CompilerPathCache.getInstance();
            List<EPCompiled> pathOne = new ArrayList<>();
            compileAdd(env, cache, pathOne, new SupportModuleLoadDetector(), "@public create schema A()");

            List<EPCompiled> pathTwo = new ArrayList<>();
            compileAdd(env, cache, pathTwo, new SupportModuleLoadDetector(), "@public create schema A()");

            CompilerArguments args = new CompilerArguments();
            if (withCache) {
                args.getOptions().setPathCache(cache);
            }
            args.getPath().addAll(pathOne);
            args.getPath().addAll(pathTwo);

            try {
                EPCompilerProvider.getCompiler().compile("create schema B()", args);
                fail();
            } catch (EPCompileException e) {
                SupportMessageAssertUtil.assertMessage(e, "Invalid path: An event type by name 'A' has already been created for module '(unnamed)'");
            }
        }
    }

    private static class ClientCompilePathCacheFillByCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled e1 = env.compile(EPL_PROVIDE);

            CompilerPathCache cache = CompilerPathCache.getInstance();
            List<EPCompiled> path = new ArrayList<>();
            path.add(e1);

            SupportModuleLoadDetector detectorOne = new SupportModuleLoadDetector();
            compileAdd(env, cache, path, detectorOne, "create schema X()");
            assertTrue(detectorOne.isLoadedModuleProvider());

            SupportModuleLoadDetector detectorTwo = new SupportModuleLoadDetector();
            compileAdd(env, cache, path, detectorTwo, EPL_CONSUME);
            assertFalse(detectorTwo.isLoadedModuleProvider());
        }
    }

    private static class ClientCompilePathCacheProtected implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            CompilerPathCache cache = CompilerPathCache.getInstance();
            SupportModuleLoadDetector classLoaderProvider = new SupportModuleLoadDetector();
            List<EPCompiled> path = new ArrayList<>();

            String eplProvide = "module a.b.c; @protected create variable int myvariable = 10;\n";
            compileAdd(env, cache, path, classLoaderProvider, eplProvide);

            String eplConsume = "module a.b.c; select myvariable from SupportBean;\n";
            compileAdd(env, cache, path, classLoaderProvider, eplConsume);
            assertFalse(classLoaderProvider.isLoadedModuleProvider());
        }
    }

    private static class ClientCompilePathCacheObjectTypes implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            CompilerPathCache cache = CompilerPathCache.getInstance();
            SupportModuleLoadDetector classLoaderProvider = new SupportModuleLoadDetector();
            List<EPCompiled> path = new ArrayList<>();

            compileAdd(env, cache, path, classLoaderProvider, EPL_PROVIDE);

            compileAdd(env, cache, path, classLoaderProvider, EPL_CONSUME);

            compileFAF(cache, path, classLoaderProvider, "select * from MyWindow");
            assertFalse(classLoaderProvider.isLoadedModuleProvider());
        }
    }

    private static void compileAdd(RegressionEnvironment env, CompilerPathCache cache, List<EPCompiled> path, SupportModuleLoadDetector classLoaderProvider, String epl) {
        Configuration configuration;
        try {
            configuration = SerializableObjectCopier.copy(env.getConfiguration());
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        configuration.getCommon().setTransientConfiguration(Collections.singletonMap(ClassLoaderProvider.NAME, classLoaderProvider));

        CompilerArguments args = new CompilerArguments(configuration);
        args.getOptions().setPathCache(cache);
        args.getPath().addAll(path);

        try {
            EPCompiled compiled = EPCompilerProvider.getCompiler().compile(epl, args);
            path.add(compiled);
        } catch (EPCompileException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static void compileFAF(CompilerPathCache cache, List<EPCompiled> path, SupportModuleLoadDetector classLoaderProvider, String epl) {
        Configuration configuration = new Configuration();
        configuration.getCommon().getTransientConfiguration().put(ClassLoaderProvider.NAME, classLoaderProvider);

        CompilerArguments args = new CompilerArguments(configuration);
        args.getOptions().setPathCache(cache);
        args.getPath().addAll(path);

        try {
            EPCompilerProvider.getCompiler().compileQuery(epl, args);
        } catch (EPCompileException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static class SupportModuleLoadDetector extends ClassLoader implements ClassLoaderProvider  {
        boolean loadedModuleProvider = false;

        public ClassLoader classloader() {
            return this;
        }

        public Class<?> loadClass(String name) throws ClassNotFoundException {
            return super.loadClass(name);
        }

        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (name.contains(ModuleProvider.class.getSimpleName())) {
                loadedModuleProvider = true;
            }
            return super.findClass(name);
        }

        public boolean isLoadedModuleProvider() {
            return loadedModuleProvider;
        }
    }
}
