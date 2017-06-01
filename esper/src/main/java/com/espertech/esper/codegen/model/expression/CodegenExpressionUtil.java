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

public class CodegenExpressionUtil {
    public static void renderConstant(StringBuilder builder, Object constant) {
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
        } else if (constant == null) {
            builder.append("null");
        } else if (constant instanceof int[]) {
            builder.append("new int[] {");
            int[] nums = (int[]) constant;
            String delimiter = "";
            for (int num : nums) {
                builder.append(delimiter).append(num);
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
