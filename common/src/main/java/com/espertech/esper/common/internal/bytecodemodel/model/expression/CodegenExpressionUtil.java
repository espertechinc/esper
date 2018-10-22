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
package com.espertech.esper.common.internal.bytecodemodel.model.expression;

import com.espertech.esper.common.internal.util.apachecommonstext.StringEscapeUtils;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationHelper.appendClassName;

public class CodegenExpressionUtil {
    public static void renderConstant(StringBuilder builder, Object constant, Map<Class, String> imports) {
        if (constant instanceof String) {
            builder.append('"').append(StringEscapeUtils.escapeJava((String) constant)).append('"');
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
        } else if (constant instanceof Short) {
            builder.append("(short) ").append(constant);
        } else if (constant instanceof Byte) {
            builder.append("(byte)").append(constant);
        } else if (constant.getClass().isArray()) {
            if (Array.getLength(constant) == 0) {
                builder.append("new ");
                appendClassName(builder, constant.getClass().getComponentType(), null, imports);
                builder.append("[]{}");
            } else {
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
            }
        } else if (constant.getClass().isEnum()) {
            appendClassName(builder, constant.getClass(), null, imports);
            builder.append(".").append(constant);
        } else if (constant instanceof Class) {
            CodegenExpressionClass.renderClass((Class) constant, builder, imports);
        } else if (constant instanceof BigInteger) {
            renderBigInteger((BigInteger) constant, builder, imports);
        } else if (constant instanceof BigDecimal) {
            BigDecimal bigDecimal = (BigDecimal) constant;
            builder.append("new BigDecimal(");
            renderBigInteger(bigDecimal.unscaledValue(), builder, imports);
            builder.append(",").append(bigDecimal.scale()).append(")");
        } else {
            builder.append(constant);
        }
    }

    private static void renderBigInteger(BigInteger constant, StringBuilder builder, Map<Class, String> imports) {
        builder.append("new java.math.BigInteger(");
        renderConstant(builder, constant.toByteArray(), imports);
        builder.append(")");
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
