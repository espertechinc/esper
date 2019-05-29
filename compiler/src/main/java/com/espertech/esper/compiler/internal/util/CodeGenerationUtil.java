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

import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClassInterfacesAndExtension;
import com.espertech.esper.common.internal.util.FileUtil;

import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CodeGenerationUtil {
    static void packagedecl(StringBuilder builder, String packageName) {
        builder.append("package ");
        builder.append(packageName);
        builder.append(";\n");
    }

    static void importsdecl(StringBuilder builder, Collection<Class> imports) {
        for (Class importClass : imports) {
            if (importClass.getPackage() != null && importClass.getPackage().getName().equals("java.lang")) {
                continue;
            }
            importdecl(builder, importClass);
        }
    }

    static void classimplements(StringBuilder builder, String classname, CodegenClassInterfacesAndExtension supers, boolean isPublic, boolean isStatic, Map<Class, String> imports) {
        if (isPublic) {
            builder.append("public ");
        }
        if (isStatic) {
            builder.append("static ");
        }
        builder.append("class ");
        builder.append(classname);
        supers.render(builder, imports);
        builder.append(" {\n");
    }

    public static String codeWithLineNum(String code) {
        List<String> lines = FileUtil.readFile(new StringReader(code));
        StringBuilder builder = new StringBuilder();
        int linenum = 1;
        for (String line : lines) {
            paddedNumber(builder, linenum++, 4);
            builder.append("  ");
            builder.append(line);
            builder.append("\n");
        }
        return builder.toString();
    }

    private static void paddedNumber(StringBuilder builder, int num, int size) {
        String text = Integer.toString(num);
        if (text.length() < size) {
            for (int i = 0; i < size - text.length(); i++) {
                builder.append(" ");
            }
        }
        builder.append(text);
    }

    private static void importdecl(StringBuilder builder, Class clazz) {
        builder.append("import ");
        if (clazz.isArray()) {
            clazz = clazz.getComponentType();
        }
        if (clazz.getDeclaringClass() == null) {
            builder.append(clazz.getName());
        } else {
            builder.append(clazz.getName().replace("$", "."));
        }
        builder.append(";\n");
    }
}
