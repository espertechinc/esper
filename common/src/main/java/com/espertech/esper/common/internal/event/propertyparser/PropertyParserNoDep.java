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
package com.espertech.esper.common.internal.event.propertyparser;

import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.event.property.MappedPropertyParseResult;
import com.espertech.esper.common.internal.event.property.Property;

import java.util.ArrayDeque;

/**
 * Parser similar in structure to:
 * http://cogitolearning.co.uk/docs/cogpar/files.html
 */
public class PropertyParserNoDep {
    private static Tokenizer tokenizer;

    static {
        tokenizer = new Tokenizer();
        tokenizer.add("[a-zA-Z]([a-zA-Z0-9_]|\\\\.)*", TokenType.IDENT);
        tokenizer.add("`[^`]*`", TokenType.IDENTESCAPED);
        tokenizer.add("[0-9]+", TokenType.NUMBER);
        tokenizer.add("\\[", TokenType.LBRACK);
        tokenizer.add("\\]", TokenType.RBRACK);
        tokenizer.add("\\(", TokenType.LPAREN);
        tokenizer.add("\\)", TokenType.RPAREN);
        tokenizer.add("\"([^\\\\\"]|\\\\\\\\|\\\\\")*\"", TokenType.DOUBLEQUOTEDLITERAL);
        tokenizer.add("\'([^\\']|\\\\\\\\|\\')*\'", TokenType.SINGLEQUOTEDLITERAL);
        tokenizer.add("\\.", TokenType.DOT);
        tokenizer.add("\\?", TokenType.QUESTION);
    }

    public static Property parseAndWalkLaxToSimple(String expression, boolean rootedDynamic) throws PropertyAccessException {
        try {
            ArrayDeque<Token> tokens = tokenizer.tokenize(expression);
            PropertyTokenParser parser = new PropertyTokenParser(tokens, rootedDynamic);
            return parser.property();
        } catch (PropertyParseNodepException ex) {
            throw new PropertyAccessException("Failed to parse property '" + expression + "': " + ex.getMessage(), ex);
        }
    }

    /**
     * Parse the mapped property into classname, method and string argument.
     * Mind this has been parsed already and is a valid mapped property.
     *
     * @param property is the string property to be passed as a static method invocation
     * @return descriptor object
     */
    public static MappedPropertyParseResult parseMappedProperty(String property) {
        // split the class and method from the parentheses and argument
        int indexOpenParen = property.indexOf("(");
        if (indexOpenParen == -1) {
            return null;
        }
        String classAndMethod = property.substring(0, indexOpenParen);
        String parensAndArg = property.substring(indexOpenParen);
        if (classAndMethod.length() == 0 || parensAndArg.length() == 0) {
            return null;
        }
        // find the first quote
        int startArg;
        int indexFirstDoubleQuote = parensAndArg.indexOf("\"");
        int indexFirstSingleQuote = parensAndArg.indexOf("'");
        if (indexFirstSingleQuote != -1 && indexFirstDoubleQuote != -1) {
            startArg = Math.min(indexFirstDoubleQuote, indexFirstSingleQuote);
        } else if (indexFirstSingleQuote != -1) {
            startArg = indexFirstSingleQuote;
        } else if (indexFirstDoubleQuote != -1) {
            startArg = indexFirstDoubleQuote;
        } else {
            return null;
        }
        // find the last quote
        int endArg;
        int indexLastDoubleQuote = parensAndArg.lastIndexOf("\"");
        int indexLastSingleQuote = parensAndArg.lastIndexOf("'");
        if (indexLastSingleQuote != -1 && indexLastDoubleQuote != -1) {
            endArg = Math.max(indexLastDoubleQuote, indexLastSingleQuote);
        } else if (indexLastSingleQuote != -1) {
            endArg = indexLastSingleQuote;
        } else if (indexLastDoubleQuote != -1) {
            endArg = indexLastDoubleQuote;
        } else {
            return null;
        }
        if (startArg == endArg) {
            return null;
        }
        String argument = parensAndArg.substring(startArg + 1, endArg);
        // split the class from the method
        int indexLastDot = classAndMethod.lastIndexOf(".");
        if (indexLastDot == -1) {
            // no class name
            return new MappedPropertyParseResult(null, classAndMethod, argument);
        }
        String method = classAndMethod.substring(indexLastDot + 1);
        if (method.length() == 0) {
            return null;
        }
        String clazz = classAndMethod.substring(0, indexLastDot);
        return new MappedPropertyParseResult(clazz, method, argument);
    }
}
