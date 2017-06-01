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

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.util.ClassLoaderProvider;
import com.espertech.esper.codegen.core.CodegenClass;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.core.CodegenMethod;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.compile.CodeGenerationUtil.*;
import static com.espertech.esper.codegen.core.CodeGenerationHelper.appendClassName;

public class CodegenCompiler {

    public static final boolean DEBUG = true;
    private static final Logger log = LoggerFactory.getLogger(CodegenCompiler.class);

    private static Class janinoCompilerClass;
    private static Constructor janinoCompilerCtor;
    private static Method janinoCompilerCookMethod;
    private static Method janinoCompilerGetClassLoaderMethod;
    private static Method janinoCompilerSetDebuggingInformationMethod;
    private static Method janinoCompilerSetParentClassLoaderMethod;

    public static <T> T compile(CodegenClass clazz, ClassLoaderProvider classLoaderProvider, Class<T> interfaceClass, String classLevelComment) {
        if (janinoCompilerClass == null) {
            setupJanino();
        }

        // build members and imports
        Set<CodegenMember> memberSet = new LinkedHashSet<>(clazz.getMembers());
        Set<Class> classes = clazz.getReferencedClasses();
        Map<Class, String> imports = compileImports(classes);

        // generate code
        String code = generateCode(imports, clazz, memberSet, classLevelComment);
        String fullyQualifiedClassName = clazz.getPackageName() + "." + clazz.getClassName();

        // allocate constructor parameters
        Object[] params = new Object[memberSet.size()];
        int count = 0;
        for (CodegenMember member : memberSet) {
            params[count++] = member.getObject();
        }

        // compiler
        Class<T> compiled = compileWJanino(code, fullyQualifiedClassName, classLoaderProvider);

        // instantiate
        try {
            return interfaceClass.cast(compiled.getConstructors()[0].newInstance(params));
        } catch (Exception ex) {
            throw new EPException("Failed to instantiate code-generated class: " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static void setupJanino() {
        String classnameCompiler = "org.codehaus.janino.SimpleCompiler";
        try {
            janinoCompilerClass = Class.forName("org.codehaus.janino.SimpleCompiler");
        } catch (Exception ex) {
            throw new EPException("Failed to load Janino compiler class '" + classnameCompiler + "': " + ex.getMessage(), ex);
        }

        try {
            janinoCompilerCtor = janinoCompilerClass.getConstructor();
        } catch (Exception ex) {
            throw new EPException("Failed to find Janino compiler constructor taking no parameters: " + ex.getMessage(), ex);
        }

        try {
            janinoCompilerCookMethod = janinoCompilerClass.getMethod("cook", Reader.class);
        } catch (Exception ex) {
            throw new EPException("Failed to find Janino compiler cook method taking StringReader as parameter: " + ex.getMessage(), ex);
        }

        try {
            janinoCompilerGetClassLoaderMethod = janinoCompilerClass.getMethod("getClassLoader");
        } catch (Exception ex) {
            throw new EPException("Failed to find Janino compiler 'getClassLoader' method: " + ex.getMessage(), ex);
        }

        try {
            janinoCompilerSetDebuggingInformationMethod = janinoCompilerClass.getMethod("setDebuggingInformation", boolean.class, boolean.class, boolean.class);
        } catch (Exception ex) {
            throw new EPException("Failed to find Janino compiler 'setDebuggingInformation' method: " + ex.getMessage(), ex);
        }

        try {
            janinoCompilerSetParentClassLoaderMethod = janinoCompilerClass.getMethod("setParentClassLoader", ClassLoader.class);
        } catch (Exception ex) {
            throw new EPException("Failed to find Janino compiler 'janinoCompilerSetParentClassLoaderMethod' method: " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> compileWJanino(String code, String fullyQualifiedClassName, ClassLoaderProvider classLoaderProvider) {

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
            janinoCompilerSetParentClassLoaderMethod.invoke(compiler, classLoaderProvider.classloader());
        } catch (Exception ex) {
            throw new EPException("Failed to invoke Janino getClassLoader: " + ex.getMessage(), ex);
        }

        if (DEBUG) {
            try {
                janinoCompilerSetDebuggingInformationMethod.invoke(compiler, true, true, true);
            } catch (Exception ex) {
                throw new EPException("Failed to invoke Janino getClassLoader: " + ex.getMessage(), ex);
            }
        }

        try {
            if (DEBUG) {
                log.info("Compiling class " + fullyQualifiedClassName + ":\n" + codeWithLineNum(code));
            }
            janinoCompilerCookMethod.invoke(compiler, new StringReader(code));
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            String message = "Failed code generation: " + t.getMessage() + "\ncode:\n" + codeWithLineNum(code);
            log.error(message, t);
            throw new EPException(message, t);
        } catch (Exception ex) {
            throw new EPException("Failed to invoke Janino compile method: " + ex.getMessage(), ex);
        }

        // For: ClassLoader classloader = compiler.getClassLoader();
        ClassLoader classLoader;
        try {
            classLoader = (ClassLoader) janinoCompilerGetClassLoaderMethod.invoke(compiler);
        } catch (Exception ex) {
            throw new EPException("Failed to invoke Janino getClassLoader: " + ex.getMessage(), ex);
        }

        try {
            return (Class<T>) Class.forName(fullyQualifiedClassName, true, classLoader);
        } catch (ClassNotFoundException ex) {
            throw new EPException("Failed find compiled class: " + ex.getMessage(), ex);
        }
    }

    private static Map<Class, String> compileImports(Set<Class> classes) {
        Map<Class, String> imports = new HashMap<>();
        Map<String, Class> assignments = new HashMap<>();
        for (Class clazz : classes) {
            if (clazz == null) {
                continue;
            }
            if (clazz.isArray()) {
                compileImports(JavaClassHelper.getComponentTypeOutermost(clazz), imports, assignments);
            } else {
                compileImports(clazz, imports, assignments);
            }
        }
        return imports;
    }

    private static void compileImports(Class clazz, Map<Class, String> imports, Map<String, Class> assignments) {
        if (clazz == null || clazz.isPrimitive()) {
            return;
        }

        try {
            if (clazz.getPackage().getName().equals("java.lang")) {
                imports.put(clazz, clazz.getSimpleName());
                return;
            }
        } catch (Throwable r) {
            System.out.println(r);
        }

        if (assignments.containsKey(clazz.getSimpleName())) {
            return;
        }
        imports.put(clazz, clazz.getSimpleName());
        assignments.put(clazz.getSimpleName(), clazz);
    }

    private static String generateCode(Map<Class, String> imports, CodegenClass clazz, Set<CodegenMember> memberSet, String classLevelComment) {
        StringBuilder builder = new StringBuilder();

        packagedecl(builder, clazz.getPackageName());
        importsdecl(builder, imports.keySet());
        if (classLevelComment != null) {
            builder.append("// ").append(classLevelComment).append("\n");
        }
        classimplements(builder, clazz.getClassName(), clazz.getInterfaceImplemented());

        // members
        for (CodegenMember member : memberSet) {
            appendClassName(builder, member.getClazz(), member.getOptionalTypeParam(), imports);
            builder.append(" ").append(member.getMemberName()).append(";\n");
        }

        // ctor
        builder.append("public ").append(clazz.getClassName()).append("(");
        String delimiter = "";
        for (CodegenMember member : memberSet) {
            builder.append(delimiter);
            appendClassName(builder, member.getClazz(), member.getOptionalTypeParam(), imports);
            builder.append(" ").append(member.getMemberName());
            delimiter = ",";
        }
        builder.append("){\n");
        for (CodegenMember member : memberSet) {
            builder.append("this.").append(member.getMemberName()).append("=").append(member.getMemberName()).append(";\n");
        }
        builder.append("}\n");

        // public methods
        for (CodegenMethod publicMethod : clazz.getPublicMethods()) {
            publicMethod.render(builder, imports, true);
        }

        // private methods
        Set<CodegenMethod> methodSet = new LinkedHashSet<>(clazz.getPrivateMethods());
        for (CodegenMethod method : methodSet) {
            method.render(builder, imports, false);
        }

        // close
        builder.append("}\n");
        return builder.toString();
    }
}
