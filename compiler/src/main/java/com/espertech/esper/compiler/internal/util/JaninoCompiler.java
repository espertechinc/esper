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
import com.espertech.esper.common.internal.context.util.ByteArrayProvidingClassLoader;
import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.Parser;
import org.codehaus.janino.Scanner;
import org.codehaus.janino.UnitCompiler;
import org.codehaus.janino.util.ClassFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;
import java.util.function.Consumer;

import static com.espertech.esper.compiler.internal.util.CodeGenerationUtil.codeWithLineNum;

public class JaninoCompiler {
    private final static Logger log = LoggerFactory.getLogger(JaninoCompiler.class);

    protected static void compile(CodegenClass clazz, Map<String, byte[]> classpath, Map<String, byte[]> output, ModuleCompileTimeServices compileTimeServices) {
        boolean withCodeLogging = compileTimeServices.getConfiguration().getCompiler().getLogging().isEnableCode();
        String code = CodegenClassGenerator.compile(clazz);
        compileInternal(code, clazz.getClassName(), classpath, output, withCodeLogging, compileTimeServices.getParentClassLoader(), true, null);
    }

    protected static void compile(String code, String filenameWithoutExtension, Map<String, byte[]> classpath, Map<String, byte[]> output, Consumer<Object> compileResultConsumer, ModuleCompileTimeServices compileTimeServices) {
        boolean withCodeLogging = compileTimeServices.getConfiguration().getCompiler().getLogging().isEnableCode();
        compileInternal(code, filenameWithoutExtension, classpath, output, withCodeLogging, compileTimeServices.getParentClassLoader(), false, compileResultConsumer);
    }

    private static void compileInternal(String code, String classNameForFile, Map<String, byte[]> classpath, Map<String, byte[]> output, boolean withCodeLogging, ClassLoader classLoader, boolean withErrorLogging, Consumer<Object> compileResultConsumer) {
        try {

            String optionalFileName = null;
            if (Boolean.getBoolean(Scanner.SYSTEM_PROPERTY_SOURCE_DEBUGGING_ENABLE)) {
                String dirName = System.getProperty(Scanner.SYSTEM_PROPERTY_SOURCE_DEBUGGING_DIR);
                if (dirName == null) {
                    dirName = System.getProperty("java.io.tmpdir");
                }
                File file = new File(dirName, classNameForFile + ".java");
                if (!file.exists()) {
                    boolean created = file.createNewFile();
                    if (!created) {
                        throw new RuntimeException("Failed to created file '" + file + "'");
                    }
                }

                FileWriter writer = null;
                try {
                    writer = new FileWriter(file);
                    PrintWriter print = new PrintWriter(writer);
                    print.write(code);
                    print.close();
                } catch (IOException ex) {
                    throw new RuntimeException("Failed to write to file '" + file + "'");
                } finally {
                    if (writer != null) {
                        writer.close();
                    }
                }

                boolean keep = Boolean.parseBoolean(System.getProperty(Scanner.SYSTEM_PROPERTY_SOURCE_DEBUGGING_KEEP));
                if (!keep) {
                    file.deleteOnExit();
                }
                optionalFileName = file.getAbsolutePath();
            }

            org.codehaus.janino.Scanner scanner = new Scanner(optionalFileName, new ByteArrayInputStream(
                code.getBytes("UTF-8")), "UTF-8");

            ByteArrayProvidingClassLoader cl = new ByteArrayProvidingClassLoader(classpath, classLoader);
            UnitCompiler unitCompiler = new UnitCompiler(
                new Parser(scanner).parseAbstractCompilationUnit(),
                new ClassLoaderIClassLoader(cl));
            ClassFile[] classFiles = unitCompiler.compileUnit(true, true, true);
            for (int i = 0; i < classFiles.length; i++) {
                output.put(classFiles[i].getThisClassName(), classFiles[i].toByteArray());
            }
            if (compileResultConsumer != null) {
                compileResultConsumer.accept(classFiles);
            }

            if (withCodeLogging) {
                log.info("Code:\n" + codeWithLineNum(code));
            }
        } catch (Exception ex) {
            if (withErrorLogging) {
                log.error("Failed to compile: " + ex.getMessage() + "\ncode:" + codeWithLineNum(code));
            }
            throw new RuntimeException(ex);
        }
    }
}
