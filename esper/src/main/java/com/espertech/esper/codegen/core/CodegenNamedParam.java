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
package com.espertech.esper.codegen.core;

import java.util.*;

import static com.espertech.esper.codegen.core.CodeGenerationHelper.appendClassName;

public class CodegenNamedParam {
    private final Class type;
    private final String name;

    public CodegenNamedParam(Class type, String name) {
        this.type = type;
        this.name = name;
    }

    public void render(StringBuilder builder, Map<Class, String> imports) {
        appendClassName(builder, type, null, imports);
        builder.append(" ").append(name);
    }

    public static List<CodegenNamedParam> from(Class typeOne, String nameOne, Class typeTwo, String nameTwo, Class typeThree, String nameThree, Class typeFour, String nameFour) {
        List<CodegenNamedParam> result = new ArrayList<>(4);
        result.add(new CodegenNamedParam(typeOne, nameOne));
        result.add(new CodegenNamedParam(typeTwo, nameTwo));
        result.add(new CodegenNamedParam(typeThree, nameThree));
        result.add(new CodegenNamedParam(typeFour, nameFour));
        return result;
    }

    public static List<CodegenNamedParam> from(Class typeOne, String nameOne) {
        return Collections.singletonList(new CodegenNamedParam(typeOne, nameOne));
    }

    public static void render(StringBuilder builder, List<CodegenNamedParam> params, Map<Class, String> imports) {
        String delimiter = "";
        for (CodegenNamedParam param : params) {
            builder.append(delimiter);
            param.render(builder, imports);
            delimiter = ",";
        }
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(type);
    }
}
