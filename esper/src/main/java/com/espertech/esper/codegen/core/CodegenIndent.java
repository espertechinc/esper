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

public class CodegenIndent {
    private final boolean indent;

    public CodegenIndent(boolean indent) {
        this.indent = indent;
    }

    public void indent(StringBuilder builder, int level) {
        if (!indent) {
            return;
        }
        for (int i = 0; i < level; i++) {
            builder.append("  ");
        }
    }
}
