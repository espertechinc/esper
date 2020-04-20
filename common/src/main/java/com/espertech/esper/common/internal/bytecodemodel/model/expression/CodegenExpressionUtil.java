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

import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.apachecommonstext.StringEscapeUtils;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationHelper.appendClassName;

public class CodegenExpressionUtil {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

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
        } else if (constant instanceof byte[]) {
            byte[] bytes = (byte[]) constant;
            if (bytes.length > 10) {
                String hex = bytesToHex((byte[]) constant);
                builder.append(CodegenExpressionUtil.class.getName()).append(".").append("hexStringToByteArray(\"").append(hex).append("\")");
            } else {
                renderArray(constant, builder, imports);
            }
        } else if (constant.getClass().isArray()) {
            renderArray(constant, builder, imports);
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

    public static boolean canRenderConstant(Object constant) {
        return constant == null ||
            JavaClassHelper.isJavaBuiltinDataType(constant.getClass()) ||
            constant.getClass().isEnum() ||
            constant instanceof CharSequence ||
            constant instanceof Class ||
            constant instanceof BigInteger ||
            constant instanceof BigDecimal;
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

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static void renderArray(Object constant, StringBuilder builder, Map<Class, String> imports) {
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
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param s bytes hex
     * @return byte array
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
