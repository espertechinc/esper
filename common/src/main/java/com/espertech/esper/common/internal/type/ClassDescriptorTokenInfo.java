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

import java.util.regex.Pattern;

public class ClassDescriptorTokenInfo {
    protected final Pattern regex;
    protected final ClassDescriptorTokenType token;

    public ClassDescriptorTokenInfo(Pattern regex, ClassDescriptorTokenType token) {
        this.regex = regex;
        this.token = token;
    }

    public String toString() {
        return "ClassIdentifierWArrayTokenType{" +
            "regex=" + regex +
            ", token=" + token +
            '}';
    }
}
