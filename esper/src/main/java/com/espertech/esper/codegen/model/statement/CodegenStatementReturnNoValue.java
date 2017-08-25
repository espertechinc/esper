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
package com.espertech.esper.codegen.model.statement;

import java.util.Map;
import java.util.Set;

public class CodegenStatementReturnNoValue extends CodegenStatementBase implements CodegenStatement {
    public final static CodegenStatementReturnNoValue INSTANCE = new CodegenStatementReturnNoValue();

    private CodegenStatementReturnNoValue() {
    }

    public void renderStatement(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        builder.append("return");
    }

    public void mergeClasses(Set<Class> classes) {
    }
}
