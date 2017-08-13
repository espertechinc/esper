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

public class CodegenMethodId {
    private final int methodNumber;
    private final String methodName;

    public CodegenMethodId(int methodNumber) {
        this.methodNumber = methodNumber;
        this.methodName = null;
    }

    public CodegenMethodId(String methodName) {
        this.methodNumber = -1;
        this.methodName = methodName;
    }

    public void render(StringBuilder builder) {
        if (methodName != null) {
            builder.append(methodName);
        } else {
            builder.append("m").append(methodNumber);
        }
    }
}
