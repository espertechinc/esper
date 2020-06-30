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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Locale;

public class ClassDescriptorParserWalker {
    private final ArrayDeque<ClassDescriptorToken> tokens;
    private ClassDescriptorToken lookahead;

    public ClassDescriptorParserWalker(ArrayDeque<ClassDescriptorToken> tokens) {
        if (tokens.isEmpty()) {
            throw new ClassDescriptorParseException("Empty class identifier");
        }
        lookahead = tokens.getFirst();
        this.tokens = tokens;
    }

    public ClassDescriptor walk(boolean typeParam) throws ClassDescriptorParseException {
        if (lookahead.token != ClassDescriptorTokenType.IDENTIFIER) {
            expectOrFail(ClassDescriptorTokenType.IDENTIFIER);
        }

        String name = lookahead.sequence;
        ClassDescriptor ident = new ClassDescriptor(name);

        nextToken();
        if (lookahead.token == ClassDescriptorTokenType.LESSER_THAN) {
            nextToken();
            walkTypeParams(ident);
        }
        if (lookahead.token == ClassDescriptorTokenType.LEFT_BRACKET) {
            nextToken();
            walkArray(ident);
        }

        if (!typeParam) {
            expectOrFail(ClassDescriptorTokenType.END);
            return ident;
        } else {
            return ident;
        }
    }

    private void walkArray(ClassDescriptor ident) {
        if (lookahead.token == ClassDescriptorTokenType.IDENTIFIER) {
            String name = lookahead.sequence;
            if (!name.toLowerCase(Locale.ENGLISH).trim().equals(ClassDescriptor.PRIMITIVE_KEYWORD)) {
                throw new ClassDescriptorParseException("Invalid array keyword '" + name + "', expected ']' or '" + ClassDescriptor.PRIMITIVE_KEYWORD + "'");
            }
            ident.setArrayOfPrimitive(true);
            nextToken();
        }

        while (true) {
            expectOrFail(ClassDescriptorTokenType.RIGHT_BRACKET);
            nextToken();
            ident.setArrayDimensions(ident.getArrayDimensions() + 1);
            if (lookahead.token != ClassDescriptorTokenType.LEFT_BRACKET) {
                break;
            } else {
                nextToken();
            }
        }
    }

    private void walkTypeParams(ClassDescriptor parent) {
        ClassDescriptor ident = walk(true);
        if (parent.getTypeParameters().isEmpty()) {
            parent.setTypeParameters(new ArrayList<>(2));
        }
        parent.getTypeParameters().add(ident);
        while (true) {
            if (lookahead.token == ClassDescriptorTokenType.COMMA) {
                nextToken();
                ident = walk(true);
                parent.getTypeParameters().add(ident);
                continue;
            }
            if (lookahead.token == ClassDescriptorTokenType.GREATER_THAN) {
                nextToken();
                break;
            }
            expectOrFail(ClassDescriptorTokenType.GREATER_THAN);
        }
    }

    private void nextToken() {
        tokens.pop();
        if (tokens.isEmpty())
            lookahead = new ClassDescriptorToken(ClassDescriptorTokenType.END, "");
        else
            lookahead = tokens.getFirst();
    }

    private void expectOrFail(ClassDescriptorTokenType expected) {
        if (lookahead.token != expected) {
            throw new ClassDescriptorParseException("Unexpected token " + lookahead.token + " value '" + lookahead.sequence + "', expecting " + expected);
        }
    }
}
