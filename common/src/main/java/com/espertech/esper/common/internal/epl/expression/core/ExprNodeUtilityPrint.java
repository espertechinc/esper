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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.internal.epl.expression.chain.Chainable;
import com.espertech.esper.common.internal.epl.expression.chain.ChainableArray;
import com.espertech.esper.common.internal.epl.expression.chain.ChainableCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.List;

public class ExprNodeUtilityPrint {
    private static final Logger log = LoggerFactory.getLogger(ExprNodeUtilityPrint.class);

    public static String[] toExpressionStringMinPrecedenceAsArray(ExprNode[] nodes) {
        String[] expressions = new String[nodes.length];
        for (int i = 0; i < expressions.length; i++) {
            StringWriter writer = new StringWriter();
            nodes[i].toEPL(writer, ExprPrecedenceEnum.MINIMUM, ExprNodeRenderableFlags.DEFAULTFLAGS);
            expressions[i] = writer.toString();
        }
        return expressions;
    }

    public static String toExpressionStringMinPrecedenceAsList(ExprNode[] nodes) {
        StringWriter writer = new StringWriter();
        toExpressionStringMinPrecedenceAsList(nodes, writer);
        return writer.toString();
    }

    public static void toExpressionStringMinPrecedenceAsList(ExprNode[] nodes, StringWriter writer) {
        String delimiter = "";
        for (ExprNode node : nodes) {
            writer.append(delimiter);
            node.toEPL(writer, ExprPrecedenceEnum.MINIMUM, ExprNodeRenderableFlags.DEFAULTFLAGS);
            delimiter = ",";
        }
    }

    public static String[] toExpressionStringsMinPrecedence(ExprNode[] expressions) {
        String[] texts = new String[expressions.length];
        for (int i = 0; i < expressions.length; i++) {
            texts[i] = toExpressionStringMinPrecedenceSafe(expressions[i]);
        }
        return texts;
    }

    public static String[] toExpressionStringsMinPrecedence(ExprForge[] expressions) {
        String[] texts = new String[expressions.length];
        for (int i = 0; i < expressions.length; i++) {
            StringWriter writer = new StringWriter();
            expressions[i].getForgeRenderable().toEPL(writer, ExprPrecedenceEnum.MINIMUM, ExprNodeRenderableFlags.DEFAULTFLAGS);
            texts[i] = writer.toString();
        }
        return texts;
    }

    public static String toExpressionStringMinPrecedence(ExprForge expression) {
        StringWriter writer = new StringWriter();
        expression.getForgeRenderable().toEPL(writer, ExprPrecedenceEnum.MINIMUM, ExprNodeRenderableFlags.DEFAULTFLAGS);
        return writer.toString();
    }

    public static String toExpressionStringMinPrecedence(ExprNode expression, ExprNodeRenderableFlags flags) {
        StringWriter writer = new StringWriter();
        expression.toEPL(writer, ExprPrecedenceEnum.MINIMUM, flags);
        return writer.toString();
    }

    public static String toExpressionStringMinPrecedenceSafe(ExprNode node) {
        try {
            StringWriter writer = new StringWriter();
            node.toEPL(writer, ExprPrecedenceEnum.MINIMUM, ExprNodeRenderableFlags.DEFAULTFLAGS);
            return writer.toString();
        } catch (RuntimeException ex) {
            log.debug("Failed to render expression text: " + ex.getMessage(), ex);
            return "";
        }
    }

    public static void toExpressionStringParameterList(ExprNode[] childNodes, StringWriter buffer) {
        String delimiter = "";
        for (ExprNode childNode : childNodes) {
            buffer.append(delimiter);
            buffer.append(toExpressionStringMinPrecedenceSafe(childNode));
            delimiter = ",";
        }
    }

    public static void toExpressionStringWFunctionName(String functionName, ExprNode[] childNodes, StringWriter writer) {
        writer.append(functionName);
        writer.append("(");
        toExpressionStringParameterList(childNodes, writer);
        writer.append(')');
    }

    public static void toExpressionStringParams(StringWriter writer, ExprNode[] params) {
        writer.append('(');
        String delimiter = "";
        for (ExprNode childNode : params) {
            writer.append(delimiter);
            delimiter = ",";
            writer.append(toExpressionStringMinPrecedenceSafe(childNode));
        }
        writer.append(')');
    }

    public static void toExpressionStringParameterList(List<ExprNode> parameters, StringWriter buffer) {
        String delimiter = "";
        for (ExprNode param : parameters) {
            buffer.append(delimiter);
            delimiter = ",";
            buffer.append(toExpressionStringMinPrecedenceSafe(param));
        }
    }

    public static void toExpressionString(ExprNode node, StringWriter buffer) {
        node.toEPL(buffer, ExprPrecedenceEnum.MINIMUM, ExprNodeRenderableFlags.DEFAULTFLAGS);
    }

    public static void toExpressionStringIncludeParen(List<ExprNode> parameters, StringWriter buffer) {
        buffer.append("(");
        toExpressionStringParameterList(parameters, buffer);
        buffer.append(")");
    }

    public static void toExpressionString(List<Chainable> chainSpec, StringWriter buffer, boolean prefixDot, String functionName) {
        String delimiterOuter = "";
        if (prefixDot) {
            delimiterOuter = ".";
        }
        for (Chainable element : chainSpec) {
            if (element.isDistinct()) {
                buffer.append("distinct ");
            }

            if (element instanceof ChainableArray) {
                ChainableArray array = (ChainableArray) element;
                buffer.append("[");
                toExpressionStringParameterList(array.getIndexes(), buffer);
                buffer.append("]");
            } else {
                buffer.append(delimiterOuter);
                if (functionName != null) {
                    buffer.append(functionName);
                } else {
                    String name = element.getRootNameOrEmptyString();
                    buffer.append(name);
                }

                if (element instanceof ChainableCall) {
                    ChainableCall call = (ChainableCall) element;
                    toExpressionStringIncludeParen(call.getParameters(), buffer);
                }
            }
            if (element.isOptional()) {
                buffer.append("?");
            }
            delimiterOuter = ".";
        }
    }
}
