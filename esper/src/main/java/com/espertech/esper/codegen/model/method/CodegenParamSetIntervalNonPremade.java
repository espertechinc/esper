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
package com.espertech.esper.codegen.model.method;

import com.espertech.esper.codegen.core.CodegenIndent;
import com.espertech.esper.codegen.model.expression.CodegenExpression;

import java.util.Map;
import java.util.Set;

public class CodegenParamSetIntervalNonPremade extends CodegenParamSet {

    private final CodegenExpression leftStart;
    private final CodegenExpression leftEnd;
    private final CodegenExpression rightStart;
    private final CodegenExpression rightEnd;

    public CodegenParamSetIntervalNonPremade(CodegenExpression leftStart, CodegenExpression leftEnd, CodegenExpression rightStart, CodegenExpression rightEnd) {
        this.leftStart = leftStart;
        this.leftEnd = leftEnd;
        this.rightStart = rightStart;
        this.rightEnd = rightEnd;
    }

    public CodegenExpression leftStart() {
        return leftStart;
    }

    public CodegenExpression leftEnd() {
        return leftEnd;
    }

    public CodegenExpression rightStart() {
        return rightStart;
    }

    public CodegenExpression rightEnd() {
        return rightEnd;
    }

    public void mergeClasses(Set<Class> classes) {
        // nothing to merge, all expressions return long
    }

    public void render(StringBuilder builder, Map<Class, String> imports, CodegenIndent codegenIndent, String optionalComment) {
        throw new IllegalStateException("Rendering not allowed, use " + CodegenParamSetIntervalPremade.class + ".INSTANCE instead (from " + optionalComment + ")");
    }

    public CodegenPassSet getPassAll() {
        return new CodegenPassSet() {
            public void render(StringBuilder builder, Map<Class, String> imports) {
                leftStart.render(builder, imports);
                builder.append(",");
                leftEnd.render(builder, imports);
                builder.append(",");
                rightStart.render(builder, imports);
                builder.append(",");
                rightEnd.render(builder, imports);
            }

            public void mergeClasses(Set<Class> classes) {
            }
        };
    }
}

