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
package com.espertech.esper.codegen.model.expression;

import java.lang.reflect.Array;
import java.util.Map;

import static com.espertech.esper.codegen.core.CodeGenerationHelper.appendClassName;

public class CodegenExpressionUtil {
    public static void renderConstant(StringBuilder builder, Object constant, Map<Class, String> imports) {
        if (constant instanceof String) {
            builder.append('"');
            String seq = (String) constant;
            if (seq.indexOf('\"') == -1) {
                builder.append(constant);
            } else {
                appendSequenceEscapeDQ(builder, seq);
            }
            builder.append('"');
        } else if (constant instanceof CharSequence) {
            appendSequenceEscapeDQ(builder, (CharSequence) constant);
        } else if (constant instanceof Character) {
            Character c = (Character) constant;
            if (c == '\'') {
                builder.append('\'');
                builder.append('\\');
                builder.append('\'');
                builder.append('\'');
            } else if (c == '\\') {
                builder.append('\'');
                builder.append('\\');
                builder.append('\\');
                builder.append('\'');
            } else {
                builder.append('\'');
                builder.append(c);
                builder.append('\'');
            }
        } else if (constant == null) {
            builder.append("null");
        } else if (constant instanceof Long) {
            builder.append(constant).append("L");
        } else if (constant instanceof Float) {
            builder.append(constant).append("F");
        } else if (constant instanceof Byte) {
            builder.append("(byte)").append(constant);
        } else if (constant.getClass().isArray()) {
            builder.append("new ");
            appendClassName(builder, constant.getClass().getComponentType(), null, imports);
            builder.append("[] {");
            String delimiter = "";
            for (int i = 0; i < Array.getLength(constant); i++) {
                builder.append(delimiter);
                renderConstant(builder, Array.get(constant, i), imports);
                delimiter = ",";
            }
            builder.append("}");
        } else {
            builder.append(constant);
        }
    }

    private static void appendSequenceEscapeDQ(StringBuilder builder, CharSequence seq) {
        for (int i = 0; i < seq.length(); i++) {
            char c = seq.charAt(i);
            if (c == '\"') {
                builder.append('\\');
                builder.append(c);
            } else {
                builder.append(c);
            }
        }
    }
}
