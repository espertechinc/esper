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

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {

    private final LinkedList<TokenInfo> tokenInfos = new LinkedList<>();

    public void add(String pattern, TokenType token) {
        tokenInfos.add(new TokenInfo(Pattern.compile("^(" + pattern + ")"), token));
    }

    public ArrayDeque<Token> tokenize(String str) throws PropertyParseNodepException {
        ArrayDeque<Token> tokens = new ArrayDeque<>(4);
        while (!str.equals("")) {
            boolean match = false;
            for (TokenInfo info : tokenInfos) {
                Matcher m = info.regex.matcher(str);
                if (m.find()) {
                    match = true;

                    String tok = m.group().trim();
                    tokens.add(new Token(info.token, tok));

                    str = m.replaceFirst("").trim();
                    break;
                }
            }

            if (!match) {
                throw new PropertyParseNodepException("Unexpected token '" + str + "'");
            }
        }
        return tokens;
    }

}
