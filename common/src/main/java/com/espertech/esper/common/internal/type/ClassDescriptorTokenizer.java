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
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassDescriptorTokenizer {

    private final LinkedList<ClassDescriptorTokenInfo> tokens = new LinkedList<>();

    public void add(String pattern, ClassDescriptorTokenType token) {
        tokens.add(new ClassDescriptorTokenInfo(Pattern.compile("^(" + pattern + ")"), token));
    }

    public ArrayDeque<ClassDescriptorToken> tokenize(String str) throws ClassDescriptorParseException {
        ArrayDeque<ClassDescriptorToken> tokens = new ArrayDeque<>(4);
        while (!str.equals("")) {
            boolean match = false;
            for (ClassDescriptorTokenInfo info : this.tokens) {
                Matcher m = info.regex.matcher(str);
                if (m.find()) {
                    match = true;

                    String tok = m.group().trim();
                    tokens.add(new ClassDescriptorToken(info.token, tok));

                    str = m.replaceFirst("").trim();
                    break;
                }
            }

            if (!match) {
                throw new ClassDescriptorParseException("Unexpected token '" + str + "'");
            }
        }
        return tokens;
    }

}
