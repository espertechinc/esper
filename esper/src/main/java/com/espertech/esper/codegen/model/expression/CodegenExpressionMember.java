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

import com.espertech.esper.codegen.base.CodegenMemberId;

import java.util.Map;
import java.util.Set;

public class CodegenExpressionMember implements CodegenExpression {
    private final CodegenMemberId memberId;

    public CodegenExpressionMember(CodegenMemberId memberId) {
        this.memberId = memberId;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        if (isInnerClass) {
            builder.append("o.");
        }
        memberId.render(builder);
    }

    public void mergeClasses(Set<Class> classes) {
    }
}
