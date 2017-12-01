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
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.core.*;
import com.espertech.esper.epl.core.engineimport.EngineImportService;

import java.util.*;
import java.util.function.Supplier;

import static com.espertech.esper.codegen.compile.CodeGenerationUtil.*;
import static com.espertech.esper.codegen.core.CodeGenerationHelper.appendClassName;
import static com.espertech.esper.codegen.util.CodegenClassUtil.getComponentTypeOutermost;

public class CodegenClassGenerator {

    private static final CodegenIndent INDENT = new CodegenIndent(true);

    public static <T> T compile(CodegenClass clazz, EngineImportService engineImportService, Class<T> interfaceClass, Supplier<String> debugInformation) throws CodegenCompilerException {
        // build members and imports
        Set<CodegenMember> memberSet = new LinkedHashSet<>(clazz.getImplicitMembers().values());
        Set<Class> classes = clazz.getReferencedClasses();
        Map<Class, String> imports = compileImports(classes);

        // generate code
        String code = generateCode(imports, clazz, memberSet);
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
            if (clazz == null || clazz.getEnclosingClass() != null) {
                continue;
            }
            if (clazz.isArray()) {
                compileImports(getComponentTypeOutermost(clazz), imports, assignments);
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

    private static String generateCode(Map<Class, String> imports, CodegenClass clazz, Set<CodegenMember> implicitMemberSet) {
        StringBuilder builder = new StringBuilder();

        packagedecl(builder, clazz.getPackageName());
        importsdecl(builder, imports.keySet());
        classimplements(builder, clazz.getClassName(), clazz.getInterfaceImplemented(), true, false, imports);

        // members
        generateCodeMembers(builder, clazz.getExplicitMembers(), clazz.getOptionalCtor(), implicitMemberSet, imports, 1);

        // ctor
        generateCodeCtor(builder, clazz.getClassName(), false, clazz.getOptionalCtor(), implicitMemberSet, imports, 0);

        // methods
        generateCodeMethods(builder, false, clazz.getPublicMethods(), clazz.getPrivateMethods(), imports, 0);

        // inner classes
        for (CodegenInnerClass inner : clazz.getInnerClasses()) {
            builder.append("\n");
            INDENT.indent(builder, 1);
            classimplements(builder, inner.getClassName(), inner.getInterfaceImplemented(), false, true, imports);

            Set<CodegenMember> innerMembers = new LinkedHashSet<>(inner.getImplicitMembers().values());
            generateCodeMembers(builder, inner.getExplicitMembers(), inner.getCtor(), innerMembers, imports, 2);

            generateCodeCtor(builder, inner.getClassName(), true, inner.getCtor(), innerMembers, imports, 1);

            generateCodeMethods(builder, true, inner.getMethods().getPublicMethods(), inner.getMethods().getPrivateMethods(), imports, 1);
            INDENT.indent(builder, 1);
            builder.append("}\n");
        }

        // close
        builder.append("}\n");
        return builder.toString();
    }

    private static void generateCodeMethods(StringBuilder builder, boolean isInnerClass, List<CodegenMethod> publicMethods, List<CodegenMethod> privateMethods, Map<Class, String> imports, int additionalIndent) {
        // public methods
        String delimiter = "";
        for (CodegenMethod publicMethod : publicMethods) {
            builder.append(delimiter);
            publicMethod.render(builder, imports, true, isInnerClass, INDENT, additionalIndent);
            delimiter = "\n";
        }

        // private methods
        for (CodegenMethod method : privateMethods) {
            builder.append(delimiter);
            method.render(builder, imports, false, isInnerClass, INDENT, additionalIndent);
            delimiter = "\n";
        }
    }

    private static void generateCodeCtor(StringBuilder builder, String className, boolean isInnerClass, CodegenCtor optionalCtor, Set<CodegenMember> memberSet, Map<Class, String> imports, int additionalIndent) {
        INDENT.indent(builder, 1 + additionalIndent);
        builder.append("public ").append(className).append("(");
        String delimiter = "";

        // parameters
        if (optionalCtor != null) {
            for (CodegenTypedParam param : optionalCtor.getCtorParams()) {
                builder.append(delimiter);
                param.renderAsParameter(builder, imports);
                delimiter = ",";
            }
        }
        for (CodegenMember member : memberSet) {
            builder.append(delimiter);
            appendClassName(builder, member.getMemberClass(), member.getOptionalTypeParam(), imports);
            builder.append(" ");
            member.getMemberId().renderPrefixed(builder, 'p');
            delimiter = ",";
        }

        builder.append("){\n");

        // code assigning parameters
        if (optionalCtor != null) {
            for (CodegenTypedParam param : optionalCtor.getCtorParams()) {
                INDENT.indent(builder, 2 + additionalIndent);
                builder.append("this.").append(param.getName()).append("=").append(param.getName()).append(";\n");
            }
        }
        for (CodegenMember member : memberSet) {
            INDENT.indent(builder, 2 + additionalIndent);
            builder.append("this.");
            member.getMemberId().render(builder);
            builder.append("=");
            member.getMemberId().renderPrefixed(builder, 'p');
            builder.append(";\n");
        }
        if (optionalCtor != null) {
            optionalCtor.getBlock().render(builder, imports, isInnerClass, 2 + additionalIndent, INDENT);
        }

        INDENT.indent(builder, 1 + additionalIndent);
        builder.append("}\n");
        builder.append("\n");
    }

    private static void generateCodeMembers(StringBuilder builder, List<CodegenTypedParam> explicitMembers, CodegenCtor optionalCtor, Set<CodegenMember> memberSet, Map<Class, String> imports, int indent) {
        if (optionalCtor != null) {
            for (CodegenTypedParam param : optionalCtor.getCtorParams()) {
                INDENT.indent(builder, indent);
                builder.append("final ");
                param.renderAsMember(builder, imports);
                builder.append(";\n");
            }
        }
        for (CodegenMember member : memberSet) {
            INDENT.indent(builder, indent);
            builder.append("final ");
            appendClassName(builder, member.getMemberClass(), member.getOptionalTypeParam(), imports);
            builder.append(" ");
            member.getMemberId().render(builder);
            builder.append(";\n");
        }
        for (CodegenTypedParam param : explicitMembers) {
            INDENT.indent(builder, indent);
            builder.append("final ");
            param.renderType(builder, imports);
            builder.append(" ").append(param.getName());
            builder.append(";\n");
        }
        builder.append("\n");
    }
}
