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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.module.Module;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class EPRuntimeCompileReflectiveService {
    private final static String CLASSNAME_COMPILER_ARGUMENTS = "com.espertech.esper.compiler.client.CompilerArguments";
    private final static String CLASSNAME_COMPILER_PATH = "com.espertech.esper.compiler.client.CompilerPath";
    private final static String CLASSNAME_COMPILER_PROVIDER = "com.espertech.esper.compiler.client.EPCompilerProvider";
    private final static String CLASSNAME_COMPILER = "com.espertech.esper.compiler.internal.util.EPCompilerSPI";
    private final static String CLASSNAME_COMPILER_EXPRESSIONS = "com.espertech.esper.compiler.internal.util.EPCompilerSPIExpression";

    private Boolean available;
    private String message;
    private Constructor compilerArgsCtor;
    private Method compilerArgsGetPath;
    private Method compilerPathAdd;
    private Method compilerProviderGetCompiler;
    private Method compileFireAndForget;
    private Method compileModuleString;
    private Method compileModuleObject;
    private Method expressionCompiler;
    private Method compileValidate;
    private Method eplToModel;
    private Method parseModule;

    public EPRuntimeCompileReflectiveService() {
        this.available = init();
    }

    public boolean isCompilerAvailable() {
        return available;
    }

    public EPCompiled reflectiveCompile(String epl, Configuration configuration, EPCompilerPathable optionalPathable) {
        CompileMethod method = (compiler, args) -> compileModuleString.invoke(compiler, epl, args);
        return compileInternal(method, configuration, optionalPathable);
    }

    public EPCompiled reflectiveCompile(Module module, Configuration configuration, EPCompilerPathable optionalPathable) {
        CompileMethod method = (compiler, args) -> compileModuleObject.invoke(compiler, module, args);
        return compileInternal(method, configuration, optionalPathable);
    }

    public EPCompiled reflectiveCompileFireAndForget(String epl, Configuration configuration, EPCompilerPathable optionalPathable) throws EPException {
        CompileMethod method = (compiler, args) -> compileFireAndForget.invoke(compiler, epl, args);
        return compileInternal(method, configuration, optionalPathable);
    }

    public EPStatementObjectModel reflectiveEPLToModel(String epl, Configuration configuration) throws EPException {
        Object compiler = getCompiler();

        // Same as: compiler.eplToModel(epl, Configuration configuration) throws EPCompileException;
        try {
            return (EPStatementObjectModel) eplToModel.invoke(compiler, epl, configuration);
        } catch (Throwable t) {
            throw new EPException("Failed to invoke epl-to-model: " + t.getMessage(), t);
        }
    }

    public Module reflectiveParseModule(String epl) {
        Object compiler = getCompiler();

        // Same as: compiler.parseModule(epl);
        try {
            return (Module) parseModule.invoke(compiler, epl);
        } catch (Throwable t) {
            throw new EPException("Failed to invoke parse-module: " + t.getMessage(), t);
        }
    }

    public ExprNode reflectiveCompileExpression(String epl, EventType[] eventTypes, String[] streamNames, Configuration configuration) throws EPException {
        Object compiler = getCompiler();

        // Same as: EPCompilerSPIExpression exprCompiler = compiler.expressionCompiler(configuration);
        Object exprCompiler;
        try {
            exprCompiler = expressionCompiler.invoke(compiler, configuration);
        } catch (Throwable t) {
            throw new EPException("Failed to invoke expression-compiler-method of compiler path: " + t.getMessage(), t);
        }

        // Same as: exprCompiler.compileValidate(epl, eventTypes, streamNames)
        try {
            return (ExprNode) compileValidate.invoke(exprCompiler, epl, eventTypes, streamNames);
        } catch (Throwable t) {
            throw new EPException(t.getMessage(), t);
        }
    }

    private EPCompiled compileInternal(CompileMethod compileMethod, Configuration configuration, EPCompilerPathable optionalPathable) throws EPException {
        Object compiler = getCompiler();

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
        if (optionalPathable != null) {
            try {
                compilerPathAdd.invoke(path, optionalPathable);
            } catch (Throwable t) {
                throw new EPException("Failed to invoke add-method of compiler path: " + t.getMessage(), t);
            }
        }

        try {
            return (EPCompiled) compileMethod.compileMethod(compiler, compilerArguments);
        } catch (InvocationTargetException ex) {
            throw new EPException("Failed to compile: " + ex.getTargetException().getMessage(), ex.getTargetException());
        } catch (Throwable t) {
            throw new EPException("Failed to invoke compile method of compiler: " + t.getMessage(), t);
        }
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

        Class compilerExpressions = findClassByName(CLASSNAME_COMPILER_EXPRESSIONS);
        if (compilerExpressions == null) {
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

        compileModuleString = findMethod(compiler, "compile", String.class, compilerArgsClass);
        if (compileModuleString == null) {
            return false;
        }

        compileModuleObject = findMethod(compiler, "compile", Module.class, compilerArgsClass);
        if (compileModuleObject == null) {
            return false;
        }

        expressionCompiler = findMethod(compiler, "expressionCompiler", Configuration.class);
        if (expressionCompiler == null) {
            return false;
        }

        compileValidate = findMethod(compilerExpressions, "compileValidate", String.class, EventType[].class, String[].class);
        if (compileValidate == null) {
            return false;
        }

        eplToModel = findMethod(compiler, "eplToModel", String.class, Configuration.class);
        if (eplToModel == null) {
            return false;
        }

        parseModule = findMethod(compiler, "parseModule", String.class);
        if (parseModule == null) {
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

    private interface CompileMethod {
        Object compileMethod(Object compiler, Object compilerArguments) throws InvocationTargetException, IllegalAccessException;
    }

    private Object getCompiler() throws EPException {
        if (!available) {
            throw new EPException(message);
        }

        // Same as: EPCompiler compiler = EPCompilerProvider.getCompiler()
        try {
            return compilerProviderGetCompiler.invoke(null);
        } catch (Throwable t) {
            throw new EPException("Failed to invoke getCompiler-method of compiler provider: " + t.getMessage(), t);
        }
    }
}
