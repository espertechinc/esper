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

import com.espertech.esper.common.internal.bytecodemodel.core.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.util.CodegenClassUtil.getComponentTypeOutermost;

public class CodegenClassGenerator {

    private static final CodegenIndent INDENT = new CodegenIndent(true);

    public static String compile(CodegenClass clazz) {
        // build members and imports
        Set<Class> classes = clazz.getReferencedClasses();
        Map<Class, String> imports = compileImports(classes);

        // generate code
        return generateCode(imports, clazz);
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

    private static String generateCode(Map<Class, String> imports, CodegenClass clazz) {
        StringBuilder builder = new StringBuilder();

        CodeGenerationUtil.packagedecl(builder, clazz.getPackageName());
        CodeGenerationUtil.importsdecl(builder, imports.keySet());
        CodeGenerationUtil.classimplements(builder, clazz.getClassName(), clazz.getSupers(), true, false, imports);

        // members
        generateCodeMembers(builder, clazz.getExplicitMembers(), clazz.getOptionalCtor(), imports, 1);

        // ctor
        generateCodeCtor(builder, clazz.getClassName(), false, clazz.getOptionalCtor(), imports, 0);

        // methods
        generateCodeMethods(builder, false, clazz.getPublicMethods(), clazz.getPrivateMethods(), imports, 0);

        // inner classes
        for (CodegenInnerClass inner : clazz.getInnerClasses()) {
            builder.append("\n");
            INDENT.indent(builder, 1);
            CodeGenerationUtil.classimplements(builder, inner.getClassName(), inner.getSupers(), true, true, imports);

            generateCodeMembers(builder, inner.getExplicitMembers(), inner.getCtor(), imports, 2);

            generateCodeCtor(builder, inner.getClassName(), true, inner.getCtor(), imports, 1);

            generateCodeMethods(builder, true, inner.getMethods().getPublicMethods(), inner.getMethods().getPrivateMethods(), imports, 1);
            INDENT.indent(builder, 1);
            builder.append("}\n");
        }

        // close
        builder.append("}\n");
        return builder.toString();
    }

    protected static void generateCodeMethods(StringBuilder builder, boolean isInnerClass, List<CodegenMethodWGraph> publicMethods, List<CodegenMethodWGraph> privateMethods, Map<Class, String> imports, int additionalIndent) {
        // public methods
        String delimiter = "";
        for (CodegenMethodWGraph publicMethod : publicMethods) {
            builder.append(delimiter);
            publicMethod.render(builder, imports, true, isInnerClass, INDENT, additionalIndent);
            delimiter = "\n";
        }

        // private methods
        for (CodegenMethodWGraph method : privateMethods) {
            builder.append(delimiter);
            method.render(builder, imports, false, isInnerClass, INDENT, additionalIndent);
            delimiter = "\n";
        }
    }

    private static void generateCodeCtor(StringBuilder builder, String className, boolean isInnerClass, CodegenCtor optionalCtor, Map<Class, String> imports, int additionalIndent) {
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

        builder.append("){\n");

        // code assigning parameters
        if (optionalCtor != null) {
            for (CodegenTypedParam param : optionalCtor.getCtorParams()) {
                if (param.isMemberWhenCtorParam()) {
                    INDENT.indent(builder, 2 + additionalIndent);
                    builder.append("this.").append(param.getName()).append("=").append(param.getName()).append(";\n");
                }
            }
        }
        if (optionalCtor != null) {
            optionalCtor.getBlock().render(builder, imports, isInnerClass, 2 + additionalIndent, INDENT);
        }

        INDENT.indent(builder, 1 + additionalIndent);
        builder.append("}\n");
        builder.append("\n");
    }

    private static void generateCodeMembers(StringBuilder builder, List<CodegenTypedParam> explicitMembers, CodegenCtor optionalCtor, Map<Class, String> imports, int indent) {
        if (optionalCtor != null) {
            for (CodegenTypedParam param : optionalCtor.getCtorParams()) {
                if (param.isMemberWhenCtorParam()) {
                    INDENT.indent(builder, indent);
                    builder.append("final ");
                    param.renderAsMember(builder, imports);
                    builder.append(";\n");
                }
            }
        }
        for (CodegenTypedParam param : explicitMembers) {
            INDENT.indent(builder, indent);
            if (param.isPublic()) {
                builder.append("public ");
            }
            if (!param.isPublic() && param.isFinal()) {
                builder.append("final ");
            }
            if (param.isStatic()) {
                builder.append("static ");
            }
            param.renderType(builder, imports);
            builder.append(" ").append(param.getName());
            builder.append(";\n");
        }
        builder.append("\n");
    }
}
