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
package com.espertech.esper.runtime.internal.kernel.service;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPCompilerPathable;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.runtime.client.EPRuntime;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class EPRuntimeCompileReflective {
    private final static String CLASSNAME_COMPILER_ARGUMENTS = "com.espertech.esper.compiler.client.CompilerArguments";
    private final static String CLASSNAME_COMPILER_PATH = "com.espertech.esper.compiler.client.CompilerPath";
    private final static String CLASSNAME_COMPILER_PROVIDER = "com.espertech.esper.compiler.client.EPCompilerProvider";
    private final static String CLASSNAME_COMPILER = "com.espertech.esper.compiler.client.EPCompiler";

    public final EPRuntime runtime;
    private Boolean available;
    private String message;
    private Constructor compilerArgsCtor;
    private Method compilerArgsGetPath;
    private Method compilerPathAdd;
    private Method compilerProviderGetCompiler;
    private Method compileFireAndForget;
    private Method compileModule;

    EPRuntimeCompileReflective(EPRuntime runtime) {
        this.runtime = runtime;
        this.available = init();
    }

    public boolean isCompilerAvailable() {
        return available;
    }

    public EPCompiled compile(String epl) {
        return compileInternal(epl, false);
    }

    public EPCompiled compileFireAndForget(String epl) throws EPException {
        return compileInternal(epl, true);
    }

    private EPCompiled compileInternal(String epl, boolean fireAndForget) throws EPException {
        if (!available) {
            throw new EPException(message);
        }

        // Obtain a copy of the engine configuration
        Configuration configuration = runtime.getConfigurationDeepCopy();

        // Same as: CompilerArguments args = new CompilerArguments(configuration);
        Object compilerArguments;
        try {
            compilerArguments = compilerArgsCtor.newInstance(configuration);
        } catch (Throwable t) {
            throw new EPException("Failed to instantiate compiler arguments: " + t.getMessage(), t);
        }

        // Same as: CompilerPath path = args.getPath()
        Object path;
        try {
            path = compilerArgsGetPath.invoke(compilerArguments);
        } catch (Throwable t) {
            throw new EPException("Failed to instantiate compiler arguments: " + t.getMessage(), t);
        }

        // Same as: path.add(runtime.getRuntimePath());
        try {
            compilerPathAdd.invoke(path, runtime.getRuntimePath());
        } catch (Throwable t) {
            throw new EPException("Failed to invoke add-method of compiler path: " + t.getMessage(), t);
        }

        // Same as: EPCompiler compiler = EPCompilerProvider.getCompiler()
        Object compiler;
        try {
            compiler = compilerProviderGetCompiler.invoke(null);
        } catch (Throwable t) {
            throw new EPException("Failed to invoke getCompiler-method of compiler provider: " + t.getMessage(), t);
        }

        // Same as: compiler.compileQuery(epl, args)
        EPCompiled compiled;
        try {
            if (fireAndForget) {
                compiled = (EPCompiled) compileFireAndForget.invoke(compiler, epl, compilerArguments);
            }
            else {
                compiled = (EPCompiled) compileModule.invoke(compiler, epl, compilerArguments);
            }
        } catch (InvocationTargetException ex) {
            throw new EPException("Failed to compile: " + ex.getTargetException().getMessage(), ex.getTargetException());
        } catch (Throwable t) {
            throw new EPException("Failed to invoke compileQuery-method of compiler: " + t.getMessage(), t);
        }

        return compiled;
    }

    private boolean init() {
        Class compilerArgsClass = findClassByName(CLASSNAME_COMPILER_ARGUMENTS);
        if (compilerArgsClass == null) {
            return false;
        }

        Class compilerPathClass = findClassByName(CLASSNAME_COMPILER_PATH);
        if (compilerPathClass == null) {
            return false;
        }

        Class compilerProvider = findClassByName(CLASSNAME_COMPILER_PROVIDER);
        if (compilerProvider == null) {
            return false;
        }

        Class compiler = findClassByName(CLASSNAME_COMPILER);
        if (compiler == null) {
            return false;
        }

        compilerArgsCtor = findConstructor(compilerArgsClass, Configuration.class);
        if (compilerArgsCtor == null) {
            return false;
        }

        compilerArgsGetPath = findMethod(compilerArgsClass, "getPath");
        if (compilerArgsGetPath == null) {
            return false;
        }

        compilerPathAdd = findMethod(compilerPathClass, "add", EPCompilerPathable.class);
        if (compilerPathAdd == null) {
            return false;
        }

        compilerProviderGetCompiler = findMethod(compilerProvider, "getCompiler");
        if (compilerProviderGetCompiler == null) {
            return false;
        }

        compileModule = findMethod(compiler, "compile", String.class, compilerArgsClass);
        if (compileModule == null) {
            return false;
        }

        compileFireAndForget = findMethod(compiler, "compileQuery", String.class, compilerArgsClass);
        return compileFireAndForget != null;
    }

    private Class findClassByName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            message = "Failed to find class " + className + ": " + ex.getMessage();
        }
        return null;
    }

    private Constructor findConstructor(Class clazz, Class... args) {
        try {
            return clazz.getConstructor(args);
        } catch (NoSuchMethodException ex) {
            message = "Failed to find constructor of class " + clazz.getName() + " taking parameters " + JavaClassHelper.getParameterAsString(args) + ": " + ex.getMessage();
        }
        return null;
    }

    private Method findMethod(Class clazz, String name, Class... args) {
        try {
            return clazz.getMethod(name, args);
        } catch (NoSuchMethodException ex) {
            message = "Failed to find method '" + name + "' of class " + clazz.getName() + " taking parameters " + JavaClassHelper.getParameterAsString(args) + ": " + ex.getMessage();
        }
        return null;
    }
}
