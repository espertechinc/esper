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

public class ClassDescriptorToken {
    protected final ClassDescriptorTokenType token;
    protected final String sequence;

    public ClassDescriptorToken(ClassDescriptorTokenType token, String sequence) {
        this.token = token;
        this.sequence = sequence;
    }

    public String toString() {
        return "ClassIdentifierWArrayToken{" +
            "token=" + token +
            ", sequence='" + sequence + '\'' +
            '}';
    }
}
