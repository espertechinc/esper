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
package com.espertech.esper.compiler.internal.compiler.abstraction;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.internal.compile.compiler.*;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClass;
import com.espertech.esper.compiler.internal.compiler.toolprovider.JavaFileManagerForwarding;
import com.espertech.esper.compiler.internal.compiler.toolprovider.JavaFileObjectSource;
import com.espertech.esper.compiler.internal.util.CodegenClassGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.*;
import java.io.StringWriter;
import java.net.URI;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.espertech.esper.compiler.internal.util.CodeGenerationUtil.codeWithLineNum;

public class CompilerAbstractionToolProvider implements CompilerAbstraction {
    private final static Logger log = LoggerFactory.getLogger(CompilerAbstractionToolProvider.class);

    private static final String ID_PATTERN = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
    private static final Pattern FQCN = Pattern.compile(ID_PATTERN);

    private final JavaCompiler javaCompiler;

    public CompilerAbstractionToolProvider(JavaCompiler javaCompiler) {
        if (javaCompiler == null) {
            throw new IllegalArgumentException("Compiler tool option expected " + JavaCompiler.class.getName() + " but received a null value (check tool presence in classpath i.e. JDK vs JRE)");
        }
        this.javaCompiler = javaCompiler;
    }

    public CompilerAbstractionClassCollection newClassCollection() {
        return new CompilerAbstractionClassCollectionImpl();
    }

    public void compileClasses(List<CodegenClass> classes, CompilerAbstractionCompilationContext context, CompilerAbstractionClassCollection state) {
        List<JavaFileObject> fileObjects = new ArrayList<>(classes.size());
        for (CodegenClass clazz : classes) {
            String code = CodegenClassGenerator.generate(clazz);
            String filename = clazz.getClassName() + ".java";
            URI fileURI = URI.create(filename);
            fileObjects.add(new JavaFileObjectSource(fileURI, JavaFileObject.Kind.SOURCE, code));
        }

        compileFileObjects(fileObjects, state, true, context);
    }

    public CompilerAbstractionCompileSourcesResult compileSources(List<String> sources, CompilerAbstractionCompilationContext context, CompilerAbstractionClassCollection state) {
        List<JavaFileObject> fileObjects = new ArrayList<>(sources.size());
        Set<String> classNames = new LinkedHashSet<>();
        for (String code : sources) {
            String classNameNoPackage = findClassName(code);
            String classNameFull = context.getGeneratedCodePackageName() + "." + classNameNoPackage;
            if (classNames.contains(classNameFull)) {
                throw new RuntimeException("Class name '" + classNameFull + "' appears multiple times");
            }
            classNames.add(classNameFull);
            String filename = classNameNoPackage + ".java";
            URI fileURI = URI.create(filename);
            String sourceWithPackage = buildSourceWithPackage(code, context.getGeneratedCodePackageName());
            JavaFileObjectSource sourceFile = new JavaFileObjectSource(fileURI, JavaFileObject.Kind.SOURCE, sourceWithPackage);
            fileObjects.add(sourceFile);
        }

        Set<String> classNamesProduced = compileFileObjects(fileObjects, state, false, context);
        return new CompilerAbstractionCompileSourcesResult(classNamesProduced);
    }

    private String buildSourceWithPackage(String code, String generatedCodePackageName) {
        StringWriter writer = new StringWriter();
        writer.append("package ").append(generatedCodePackageName).append(";").append(System.lineSeparator()).append(code);
        return writer.toString();
    }

    private synchronized Set<String> compileFileObjects(List<JavaFileObject> fileObjects, CompilerAbstractionClassCollection state, boolean logCodeOnError, CompilerAbstractionCompilationContext context) {
        Map<String, byte[]> classes = new HashMap<>(state.getClasses());
        for (EPCompiled compiled : context.getPath()) {
            for (String clazz : context.getParentClassLoader().getClasses().keySet()) {
                byte[] bytes = compiled.getClasses().get(clazz);
                if (bytes != null) {
                    classes.put(clazz, bytes);
                }
            }
        }

        DiagnosticCollector<JavaFileObject> ds = new DiagnosticCollector<>();
        StandardJavaFileManager mgr = javaCompiler.getStandardFileManager(ds, null, null);
        JavaFileManagerForwarding fileManager = new JavaFileManagerForwarding(context.getGeneratedCodePackageName(), mgr, classes);
        JavaCompiler.CompilationTask task = javaCompiler.getTask(null, fileManager, ds, null, null, fileObjects);
        Boolean result = task.call();

        List<Diagnostic<? extends JavaFileObject>> diagnostics = ds.getDiagnostics();
        if (result == null || !result) {
            if (diagnostics.isEmpty()) {
                throw new RuntimeException("Failed to compile (without diagnostics, please check the log and console output)");
            }
            if (logCodeOnError) {
                logCodeWithErrors(diagnostics);
            }
            Diagnostic<? extends JavaFileObject> d = diagnostics.get(0);
            String message = format(d);
            throw new RuntimeException("Failed to compile: " + message);
        }

        if (context.isLogging()) {
            for (JavaFileObject in : fileObjects) {
                log.info("Code:\n" + codeWithLineNum(((JavaFileObjectSource) in).getCode()));
            }
        }

        fileManager.addClasses(state);
        return fileManager.getClassNamesProduced();
    }

    private void logCodeWithErrors(List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        Map<JavaFileObjectSource, List<Diagnostic<? extends JavaFileObject>>> perSource = new LinkedHashMap<>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
            if (diagnostic.getKind() != Diagnostic.Kind.ERROR) {
                continue;
            }
            JavaFileObjectSource source = (JavaFileObjectSource) diagnostic.getSource();
            List<Diagnostic<? extends JavaFileObject>> existing = perSource.computeIfAbsent(source, k -> new ArrayList<>(2));
            existing.add(diagnostic);
        }

        for (Map.Entry<JavaFileObjectSource, List<Diagnostic<? extends JavaFileObject>>> entry : perSource.entrySet()) {
            List<Diagnostic<? extends JavaFileObject>> list = entry.getValue();
            if (list.size() == 1) {
                log.error("Failed to compile: {}\ncode:{}", format(list.get(0)), codeWithLineNum(entry.getKey().getCode()));
            } else {
                int index = 1;
                for (Diagnostic<? extends JavaFileObject> diagnostic : entry.getValue()) {
                    log.error("Diagnostic #{}: {}", index, format(diagnostic));
                    index++;
                }
                log.error("Failed to compile with {} diagnostics is code: {}", entry.getValue().size(), codeWithLineNum(entry.getKey().getCode()));
            }
        }
    }

    private String findClassName(String code) {
        String[] prefixes = new String[] {"public class ", "public interface ", "public @interface ", "public enum "};
        Supplier<RuntimeException> exceptionSupplier = () -> new RuntimeException("Failed to determine class name based on occurrence of " + Arrays.asList(prefixes) + " in source [" + code + "]");

        int indexFound = -1;
        String prefixFound = null;
        for (String prefix : prefixes) {
            int index = code.indexOf(prefix);
            if (index != -1) {
                indexFound = index;
                prefixFound = prefix;
                break;
            }
        }
        if (indexFound == -1) {
            throw exceptionSupplier.get();
        }

        String begin = code.substring(indexFound + prefixFound.length());
        Matcher matcher = FQCN.matcher(begin);
        if (!matcher.find()) {
            throw exceptionSupplier.get();
        }
        return matcher.group();
    }

    private String format(Diagnostic<? extends JavaFileObject> d) {
        return String.format("Line: %d, %s in %s",
                d.getLineNumber(), d.getMessage(null),
                d.getSource().getName());
    }
}
