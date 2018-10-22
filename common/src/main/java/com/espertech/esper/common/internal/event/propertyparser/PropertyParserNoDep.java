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
        // get argument
        int indexFirstDoubleQuote = property.indexOf("\"");
        int indexFirstSingleQuote = property.indexOf("'");
        int startArg;
        if ((indexFirstSingleQuote == -1) && (indexFirstDoubleQuote == -1)) {
            return null;
        }
        if ((indexFirstSingleQuote != -1) && (indexFirstDoubleQuote != -1)) {
            if (indexFirstSingleQuote < indexFirstDoubleQuote) {
                startArg = indexFirstSingleQuote;
            } else {
                startArg = indexFirstDoubleQuote;
            }
        } else if (indexFirstSingleQuote != -1) {
            startArg = indexFirstSingleQuote;
        } else {
            startArg = indexFirstDoubleQuote;
        }

        int indexLastDoubleQuote = property.lastIndexOf("\"");
        int indexLastSingleQuote = property.lastIndexOf("'");
        int endArg;
        if ((indexLastSingleQuote == -1) && (indexLastDoubleQuote == -1)) {
            return null;
        }
        if ((indexLastSingleQuote != -1) && (indexLastDoubleQuote != -1)) {
            if (indexLastSingleQuote > indexLastDoubleQuote) {
                endArg = indexLastSingleQuote;
            } else {
                endArg = indexLastDoubleQuote;
            }
        } else if (indexLastSingleQuote != -1) {
            if (indexLastSingleQuote == indexFirstSingleQuote) {
                return null;
            }
            endArg = indexLastSingleQuote;
        } else {
            if (indexLastDoubleQuote == indexFirstDoubleQuote) {
                return null;
            }
            endArg = indexLastDoubleQuote;
        }
        String argument = property.substring(startArg + 1, endArg);

        // get method
        String[] splitDots = property.split("[\\.]");
        if (splitDots.length == 0) {
            return null;
        }

        // find which element represents the method, its the element with the parenthesis
        int indexMethod = -1;
        for (int i = 0; i < splitDots.length; i++) {
            if (splitDots[i].contains("(")) {
                indexMethod = i;
                break;
            }
        }
        if (indexMethod == -1) {
            return null;
        }

        String method = splitDots[indexMethod];
        int indexParan = method.indexOf("(");
        method = method.substring(0, indexParan);
        if (method.length() == 0) {
            return null;
        }

        if (splitDots.length == 1) {
            // no class name
            return new MappedPropertyParseResult(null, method, argument);
        }


        // get class
        StringBuilder clazz = new StringBuilder();
        for (int i = 0; i < indexMethod; i++) {
            if (i > 0) {
                clazz.append('.');
            }
            clazz.append(splitDots[i]);
        }

        return new MappedPropertyParseResult(clazz.toString(), method, argument);
    }
}
