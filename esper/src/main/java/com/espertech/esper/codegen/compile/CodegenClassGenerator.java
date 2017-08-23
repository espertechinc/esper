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
import com.espertech.esper.codegen.core.CodegenClass;
import com.espertech.esper.codegen.core.CodegenIndent;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.core.CodegenMethod;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.util.JavaClassHelper;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static com.espertech.esper.codegen.compile.CodeGenerationUtil.*;
import static com.espertech.esper.codegen.core.CodeGenerationHelper.appendClassName;

public class CodegenClassGenerator {

    private static final CodegenIndent INDENT = new CodegenIndent(true);

    public static <T> T compile(CodegenClass clazz, EngineImportService engineImportService, Class<T> interfaceClass, Supplier<String> debugInformation) throws CodegenCompilerException {
        // build members and imports
        Set<CodegenMember> memberSet = new LinkedHashSet<>(clazz.getMembers().values());
        Set<Class> classes = clazz.getReferencedClasses();
        Map<Class, String> imports = compileImports(classes);

        // generate code
        String code = generateCode(imports, clazz, memberSet, null);
        String fullyQualifiedClassName = clazz.getPackageName() + "." + clazz.getClassName();

        // compiler
        Class<T> compiled = engineImportService.getCodegenCompiler().compileClass(code, fullyQualifiedClassName, engineImportService.getClassLoader(), debugInformation);

        // allocate constructor parameters
        Object[] params = new Object[memberSet.size()];
        int count = 0;
        for (CodegenMember member : memberSet) {
            params[count++] = member.getObject();
        }

        // instantiate
        try {
            return interfaceClass.cast(compiled.getConstructors()[0].newInstance(params));
        } catch (Exception ex) {
            throw new EPException("Failed to instantiate code-generated class: " + ex.getMessage(), ex);
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
            if (clazz.getPackage() != null && clazz.getPackage().getName().equals("java.lang")) {
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
            INDENT.indent(builder, 1);
            appendClassName(builder, getMemberClass(member), member.getOptionalTypeParam(), imports);
            builder.append(" ");
            member.getMemberId().render(builder);
            builder.append(";\n");
        }
        builder.append("\n");

        // ctor
        INDENT.indent(builder, 1);
        builder.append("public ").append(clazz.getClassName()).append("(");
        String delimiter = "";
        for (CodegenMember member : memberSet) {
            builder.append(delimiter);
            appendClassName(builder, getMemberClass(member), member.getOptionalTypeParam(), imports);
            builder.append(" ");
            member.getMemberId().renderPrefixed(builder, 'p');
            delimiter = ",";
        }
        builder.append("){\n");
        for (CodegenMember member : memberSet) {
            INDENT.indent(builder, 2);
            builder.append("this.");
            member.getMemberId().render(builder);
            builder.append("=");
            member.getMemberId().renderPrefixed(builder, 'p');
            builder.append(";\n");
        }
        INDENT.indent(builder, 1);
        builder.append("}\n");
        builder.append("\n");

        // public methods
        for (CodegenMethod publicMethod : clazz.getPublicMethods()) {
            publicMethod.render(builder, imports, true, INDENT);
            builder.append("\n");
        }

        // private methods
        Set<CodegenMethod> methodSet = new LinkedHashSet<>(clazz.getPrivateMethods());
        for (CodegenMethod method : methodSet) {
            method.render(builder, imports, false, INDENT);
            builder.append("\n");
        }

        // close
        builder.append("}\n");
        return builder.toString();
    }

    private static Class getMemberClass(CodegenMember member) {
        return member.getObject() == null ? member.getClazz() : member.getObject().getClass();
    }
}
