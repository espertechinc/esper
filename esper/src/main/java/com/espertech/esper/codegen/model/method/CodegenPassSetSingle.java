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

import com.espertech.esper.codegen.model.expression.CodegenExpression;

import java.util.Map;
import java.util.Set;

public class CodegenPassSetSingle extends CodegenPassSet {

    private final CodegenExpression expression;

    public CodegenPassSetSingle(CodegenExpression expression) {
        this.expression = expression;
    }

    public void render(StringBuilder builder, Map<Class, String> imports) {
        expression.render(builder, imports);
    }

    public void mergeClasses(Set<Class> classes) {
        expression.mergeClasses(classes);
    }
}
