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
package com.espertech.esper.codegen.model.expression;

import java.util.Set;

public class CodegenChainElement {
    private final String method;
    private final Object[] consts;

    public CodegenChainElement(String method, Object[] consts) {
        this.method = method;
        this.consts = consts;
    }

    public void render(StringBuilder builder) {
        builder.append(method).append("(");
        if (consts != null) {
            String delimiter = "";
            for (Object constant : consts) {
                builder.append(delimiter);
                if (constant instanceof CharSequence) {
                    builder.append("\"");
                    builder.append(constant);
                    builder.append("\"");
                } else {
                    builder.append(constant);
                }
                delimiter = ",";
            }
        }
        builder.append(")");
    }

    public void mergeClasses(Set<Class> classes) {
    }
}
