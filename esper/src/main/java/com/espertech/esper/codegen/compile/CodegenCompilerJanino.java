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
package com.espertech.esper.codegen.compile;

import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.client.EPException;
import com.espertech.esper.codegen.util.IdentifierUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import static com.espertech.esper.codegen.compile.CodeGenerationUtil.codeWithLineNum;

/**
 * Janino Usage Notes
 * <p>
 * Janino cannot do shortcut-evaluation in the form of "a ? b : c". This results in "operand stack underflow".
 * </p>
 * <p>
 * Janino cannot handle compilation error "result = method()" with method returning void. This results in "operand stack underflow".
 * </p>
 * <p>
 * Define "-Dorg.codehaus.janino.source_debugging.dir=....." and "-Dorg.codehaus.janino.source_debugging.enable=true" for debugging
 * </p>
 */
public class CodegenCompilerJanino implements CodegenCompiler {

    private static final Logger log = LoggerFactory.getLogger(CodegenCompilerJanino.class);

    private final boolean logging;
    private final boolean includeDebugSymbols;
    private final String packageName;

    private Constructor janinoCompilerCtor;
    private Method janinoCompilerCookMethod;
    private Method janinoCompilerGetClassLoaderMethod;
    private Method janinoCompilerSetDebuggingInformationMethod;
    private Method janinoCompilerSetParentClassLoaderMethod;

    public CodegenCompilerJanino(String engineURI, boolean logging, boolean includeDebugSymbols) {
        this.logging = logging;
        this.includeDebugSymbols = includeDebugSymbols;
        setupJanino();
        this.packageName = "com.espertech.esper.generated.uri_" + IdentifierUtil.getIdentifierMayStartNumeric(engineURI);
    }

    public String getPackageName() {
        return packageName;
    }

    public <T> Class<T> compileClass(String code, String fullyQualifiedClassName, ClassLoader classLoader, Supplier<String> debugInformation) throws CodegenCompilerException {

        if (log.isDebugEnabled() || logging) {
            String origin = debugInformation.get();
            StringWriter writer = new StringWriter();
            PrintWriter printer = new PrintWriter(writer);
            printer.append("Compiling class for ")
                    .append(origin)
                    .append(" includeDebugSymbols=").append(Boolean.toString(includeDebugSymbols))
                    .append(" classloader=").append(classLoader.toString())
                    .append(" code: ");
            printer.println();
            printer.append(codeWithLineNum(code));
            String text = writer.toString();
            if (log.isDebugEnabled()) {
                log.debug(text);
            } else if (logging) {
                log.info(text);
            }
        }

        // For: SimpleCompiler compiler = new SimpleCompiler(scanner, classloader);
        // This step actually compiles
        Object compiler;
        try {
            compiler = janinoCompilerCtor.newInstance();
        } catch (Exception ex) {
            throw new EPException("Failed to instantiate Janino scanner: " + ex.getMessage(), ex);
        }

        // For: compiler.setParentClassLoader(...)
        try {
            janinoCompilerSetParentClassLoaderMethod.invoke(compiler, classLoader);
        } catch (Exception ex) {
            throw new EPException("Failed to invoke Janino getClassLoader: " + ex.getMessage(), ex);
        }

        if (includeDebugSymbols) {
            try {
                janinoCompilerSetDebuggingInformationMethod.invoke(compiler, true, true, true);
            } catch (Exception ex) {
                throw new EPException("Failed to invoke Janino getClassLoader: " + ex.getMessage(), ex);
            }
        }

        try {
            janinoCompilerCookMethod.invoke(compiler, new StringReader(code));
        } catch (InvocationTargetException ite) {
            throw new CodegenCompilerException("Failed to compile generated code", ite.getTargetException(), code);
        } catch (Exception ex) {
            throw new EPException("Failed to invoke Janino compile method: " + ex.getMessage(), ex);
        }

        // For: ClassLoader classloader = compiler.getClassLoader();
        ClassLoader classLoaderForLoading;
        try {
            classLoaderForLoading = (ClassLoader) janinoCompilerGetClassLoaderMethod.invoke(compiler);
        } catch (Exception ex) {
            throw new EPException("Failed to invoke Janino getClassLoader: " + ex.getMessage(), ex);
        }

        try {
            return (Class<T>) Class.forName(fullyQualifiedClassName, true, classLoaderForLoading);
        } catch (ClassNotFoundException ex) {
            throw new EPException("Failed find compiled class: " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private void setupJanino() throws ConfigurationException {
        Class janinoCompilerClass;
        String classnameCompiler = "org.codehaus.janino.SimpleCompiler";
        try {
            janinoCompilerClass = Class.forName("org.codehaus.janino.SimpleCompiler");
        } catch (Exception ex) {
            throw new ConfigurationException("Failed to load Janino compiler class '" + classnameCompiler + "': " + ex.getMessage(), ex);
        }

        try {
            janinoCompilerCtor = janinoCompilerClass.getConstructor();
        } catch (Exception ex) {
            throw new ConfigurationException("Failed to find Janino compiler constructor taking no parameters: " + ex.getMessage(), ex);
        }

        try {
            janinoCompilerCookMethod = janinoCompilerClass.getMethod("cook", Reader.class);
        } catch (Exception ex) {
            throw new ConfigurationException("Failed to find Janino compiler cook method taking StringReader as parameter: " + ex.getMessage(), ex);
        }

        try {
            janinoCompilerGetClassLoaderMethod = janinoCompilerClass.getMethod("getClassLoader");
        } catch (Exception ex) {
            throw new ConfigurationException("Failed to find Janino compiler 'getClassLoader' method: " + ex.getMessage(), ex);
        }

        try {
            janinoCompilerSetDebuggingInformationMethod = janinoCompilerClass.getMethod("setDebuggingInformation", boolean.class, boolean.class, boolean.class);
        } catch (Exception ex) {
            throw new ConfigurationException("Failed to find Janino compiler 'setDebuggingInformation' method: " + ex.getMessage(), ex);
        }

        try {
            janinoCompilerSetParentClassLoaderMethod = janinoCompilerClass.getMethod("setParentClassLoader", ClassLoader.class);
        } catch (Exception ex) {
            throw new ConfigurationException("Failed to find Janino compiler 'janinoCompilerSetParentClassLoaderMethod' method: " + ex.getMessage(), ex);
        }
    }
}
