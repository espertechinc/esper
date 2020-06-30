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
package com.espertech.esper.common.internal.type;

import com.espertech.esper.common.client.EPException;

import java.util.ArrayDeque;

/**
 * Parser similar in structure to:
 * http://cogitolearning.co.uk/docs/cogpar/files.html
 */
public class ClassDescriptorParser {
    private static ClassDescriptorTokenizer tokenizer;

    static {
        tokenizer = new ClassDescriptorTokenizer();
        tokenizer.add("([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*", ClassDescriptorTokenType.IDENTIFIER);
        tokenizer.add("\\[", ClassDescriptorTokenType.LEFT_BRACKET);
        tokenizer.add("\\]", ClassDescriptorTokenType.RIGHT_BRACKET);
        tokenizer.add("<", ClassDescriptorTokenType.LESSER_THAN);
        tokenizer.add(",", ClassDescriptorTokenType.COMMA);
        tokenizer.add(">", ClassDescriptorTokenType.GREATER_THAN);
    }

    protected static ClassDescriptor parse(String classIdent) throws ClassDescriptorParseException {
        try {
            ArrayDeque<ClassDescriptorToken> tokens = tokenizer.tokenize(classIdent);
            ClassDescriptorParserWalker parser = new ClassDescriptorParserWalker(tokens);
            return parser.walk(false);
        } catch (ClassDescriptorParseException ex) {
            throw new EPException("Failed to parse class identifier '" + classIdent + "': " + ex.getMessage(), ex);
        }
    }
}
