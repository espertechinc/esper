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

import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.core.CodeGenerationHelper.appendClassName;

public class CodegenNamedParam {
    private final Class type;
    private final String name;

    public CodegenNamedParam(Class type, String name) {
        if (type == null) {
            throw new IllegalArgumentException("Invalid null type");
        }
        this.type = type;
        this.name = name;
    }

    public CodegenNamedParam(Class type, CodegenExpressionRef name) {
        this(type, name.getRef());
    }

    public Class getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void render(StringBuilder builder, Map<Class, String> imports) {
        appendClassName(builder, type, null, imports);
        builder.append(" ").append(name);
    }

    public static List<CodegenNamedParam> from(Class typeOne, String nameOne) {
        List<CodegenNamedParam> result = new ArrayList<>(2);
        result.add(new CodegenNamedParam(typeOne, nameOne));
        return result;
    }

    public static List<CodegenNamedParam> from(Class typeOne, String nameOne, Class typeTwo, String nameTwo) {
        List<CodegenNamedParam> result = new ArrayList<>(2);
        result.add(new CodegenNamedParam(typeOne, nameOne));
        result.add(new CodegenNamedParam(typeTwo, nameTwo));
        return result;
    }

    public static List<CodegenNamedParam> from(Class typeOne, String nameOne, Class typeTwo, String nameTwo, Class typeThree, String nameThree) {
        List<CodegenNamedParam> result = new ArrayList<>(3);
        result.add(new CodegenNamedParam(typeOne, nameOne));
        result.add(new CodegenNamedParam(typeTwo, nameTwo));
        result.add(new CodegenNamedParam(typeThree, nameThree));
        return result;
    }

    public static List<CodegenNamedParam> from(Class typeOne, String nameOne, Class typeTwo, String nameTwo, Class typeThree, String nameThree, Class typeFour, String nameFour) {
        List<CodegenNamedParam> result = new ArrayList<>(4);
        result.add(new CodegenNamedParam(typeOne, nameOne));
        result.add(new CodegenNamedParam(typeTwo, nameTwo));
        result.add(new CodegenNamedParam(typeThree, nameThree));
        result.add(new CodegenNamedParam(typeFour, nameFour));
        return result;
    }

    public static List<CodegenNamedParam> from(Class typeOne, String nameOne, Class typeTwo, String nameTwo, Class typeThree, String nameThree, Class typeFour, String nameFour, Class typeFive, String nameFive) {
        List<CodegenNamedParam> result = new ArrayList<>(5);
        result.add(new CodegenNamedParam(typeOne, nameOne));
        result.add(new CodegenNamedParam(typeTwo, nameTwo));
        result.add(new CodegenNamedParam(typeThree, nameThree));
        result.add(new CodegenNamedParam(typeFour, nameFour));
        result.add(new CodegenNamedParam(typeFive, nameFive));
        return result;
    }

    public static List<CodegenNamedParam> from(Class typeOne, String nameOne, Class typeTwo, String nameTwo, Class typeThree, String nameThree, Class typeFour, String nameFour, Class typeFive, String nameFive, Class typeSix, String nameSix) {
        List<CodegenNamedParam> result = new ArrayList<>(6);
        result.add(new CodegenNamedParam(typeOne, nameOne));
        result.add(new CodegenNamedParam(typeTwo, nameTwo));
        result.add(new CodegenNamedParam(typeThree, nameThree));
        result.add(new CodegenNamedParam(typeFour, nameFour));
        result.add(new CodegenNamedParam(typeFive, nameFive));
        result.add(new CodegenNamedParam(typeSix, nameSix));
        return result;
    }

    public static List<CodegenNamedParam> from(Class typeOne, String nameOne, Class typeTwo, String nameTwo, Class typeThree, String nameThree, Class typeFour, String nameFour, Class typeFive, String nameFive, Class typeSix, String nameSix, Class typeSeven, String nameSeven) {
        List<CodegenNamedParam> result = new ArrayList<>();
        result.add(new CodegenNamedParam(typeOne, nameOne));
        result.add(new CodegenNamedParam(typeTwo, nameTwo));
        result.add(new CodegenNamedParam(typeThree, nameThree));
        result.add(new CodegenNamedParam(typeFour, nameFour));
        result.add(new CodegenNamedParam(typeFive, nameFive));
        result.add(new CodegenNamedParam(typeSix, nameSix));
        result.add(new CodegenNamedParam(typeSeven, nameSeven));
        return result;
    }

    public static List<CodegenNamedParam> from(Class typeOne, String nameOne, Class typeTwo, String nameTwo, Class typeThree, String nameThree, Class typeFour, String nameFour, Class typeFive, String nameFive, Class typeSix, String nameSix, Class typeSeven, String nameSeven, Class typeEight, String nameEight) {
        List<CodegenNamedParam> result = new ArrayList<>();
        result.add(new CodegenNamedParam(typeOne, nameOne));
        result.add(new CodegenNamedParam(typeTwo, nameTwo));
        result.add(new CodegenNamedParam(typeThree, nameThree));
        result.add(new CodegenNamedParam(typeFour, nameFour));
        result.add(new CodegenNamedParam(typeFive, nameFive));
        result.add(new CodegenNamedParam(typeSix, nameSix));
        result.add(new CodegenNamedParam(typeSeven, nameSeven));
        result.add(new CodegenNamedParam(typeEight, nameEight));
        return result;
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

    public static void render(StringBuilder builder, Map<Class, String> imports, List<CodegenNamedParam> params) {
        String delimiter = "";
        for (CodegenNamedParam param : params) {
            builder.append(delimiter);
            param.render(builder, imports);
            delimiter = ",";
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CodegenNamedParam param = (CodegenNamedParam) o;

        if (!type.equals(param.type)) return false;
        return name.equals(param.name);
    }

    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }
}
