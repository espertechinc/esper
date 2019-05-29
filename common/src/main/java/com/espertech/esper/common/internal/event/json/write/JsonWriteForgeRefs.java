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
package com.espertech.esper.common.internal.event.json.write;

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

public class JsonWriteForgeRefs {
    private final CodegenExpression writer;
    private final CodegenExpression field;
    private final CodegenExpression name;

    public JsonWriteForgeRefs(CodegenExpression writer, CodegenExpression field, CodegenExpression name) {
        this.writer = writer;
        this.field = field;
        this.name = name;
    }

    public CodegenExpression getWriter() {
        return writer;
    }

    public CodegenExpression getField() {
        return field;
    }

    public CodegenExpression getName() {
        return name;
    }
}
