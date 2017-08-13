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
package com.espertech.esper.codegen.core;

public class CodegenMemberId {
    private final int memberNumber;

    public CodegenMemberId(int memberNumber) {
        this.memberNumber = memberNumber;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CodegenMemberId that = (CodegenMemberId) o;

        return memberNumber == that.memberNumber;
    }

    public int hashCode() {
        return memberNumber;
    }

    public void render(StringBuilder builder) {
        builder.append("v").append(memberNumber);
    }

    public void renderPrefixed(StringBuilder builder, char prefix) {
        builder.append(prefix).append(memberNumber);
    }
}
