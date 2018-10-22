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

import com.espertech.esper.common.internal.event.property.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.internal.event.property.PropertyParser.unescapeBacktickForProperty;

public class PropertyTokenParser {
    private final ArrayDeque<Token> tokens;
    private Token lookahead;
    private boolean dynamic;

    public PropertyTokenParser(ArrayDeque<Token> tokens, boolean rootedDynamic) {
        if (tokens.isEmpty()) {
            throw new PropertyParseNodepException("Empty property name");
        }
        lookahead = tokens.getFirst();
        this.tokens = tokens;
        this.dynamic = rootedDynamic;
    }

    public Property property() throws PropertyParseNodepException {
        Property first = eventPropertyAtomic();

        if (lookahead.token == TokenType.END) {
            return first;
        }

        List<Property> props = new ArrayList<>(4);
        props.add(first);

        while (lookahead.token == TokenType.DOT) {
            nextToken();

            Property next = eventPropertyAtomic();
            props.add(next);
        }
        return new NestedProperty(props);
    }

    private Property eventPropertyAtomic() {
        if (lookahead.token != TokenType.IDENT && lookahead.token != TokenType.IDENTESCAPED) {
            expectOrFail(TokenType.IDENT);
        }
        String ident;
        if (lookahead.token == TokenType.IDENT) {
            ident = processIdent(lookahead.sequence);
        } else {
            ident = processIdent(unescapeBacktickForProperty(lookahead.sequence));
        }
        nextToken();

        if (lookahead.token == TokenType.LBRACK) {
            nextToken();
            expectOrFail(TokenType.LBRACK, TokenType.NUMBER);

            int index = Integer.parseInt(lookahead.sequence);
            nextToken();

            expectOrFail(TokenType.NUMBER, TokenType.RBRACK);
            nextToken();

            if (lookahead.token == TokenType.QUESTION) {
                nextToken();
                return new DynamicIndexedProperty(ident, index);
            }
            if (dynamic) {
                return new DynamicIndexedProperty(ident, index);
            }
            return new IndexedProperty(ident, index);
        }

        if (lookahead.token == TokenType.LPAREN) {
            nextToken();

            if (lookahead.token == TokenType.DOUBLEQUOTEDLITERAL || lookahead.token == TokenType.SINGLEQUOTEDLITERAL) {
                TokenType type = lookahead.token;
                String value = lookahead.sequence.trim();
                String key = value.substring(1, value.length() - 1);
                nextToken();

                expectOrFail(type, TokenType.RPAREN);
                nextToken();

                if (lookahead.token == TokenType.QUESTION) {
                    nextToken();
                    return new DynamicMappedProperty(ident, key);
                }
                if (dynamic) {
                    return new DynamicMappedProperty(ident, key);
                }
                return new MappedProperty(ident, key);
            }

            expectOrFail(TokenType.LPAREN, TokenType.DOUBLEQUOTEDLITERAL);
        }

        if (lookahead.token == TokenType.QUESTION) {
            nextToken();
            dynamic = true;
            return new DynamicSimpleProperty(ident);
        }
        if (dynamic) {
            return new DynamicSimpleProperty(ident);
        }
        return new SimpleProperty(ident);
    }

    private String processIdent(String ident) {
        if (!ident.contains(".")) {
            return ident;
        }
        return ident.replaceAll("\\\\.", ".");
    }

    private void expectOrFail(TokenType before, TokenType expected) {
        if (lookahead.token != expected) {
            throw new PropertyParseNodepException("Unexpected token " + lookahead.token + " value '" + lookahead.sequence + "', expecting " + expected + " after " + before);
        }
    }

    private void expectOrFail(TokenType expected) {
        if (lookahead.token != expected) {
            throw new PropertyParseNodepException("Unexpected token " + lookahead.token + " value '" + lookahead.sequence + "', expecting " + expected);
        }
    }

    private void nextToken() {
        tokens.pop();
        if (tokens.isEmpty())
            lookahead = new Token(TokenType.END, "");
        else
            lookahead = tokens.getFirst();
    }
}
