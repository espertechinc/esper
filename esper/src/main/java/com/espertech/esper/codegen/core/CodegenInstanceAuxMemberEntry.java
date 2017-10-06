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

import com.espertech.esper.codegen.model.expression.CodegenExpression;

public class CodegenInstanceAuxMemberEntry {
    private final String name;
    private final Class clazz;
    private final CodegenExpression initializer;

    public CodegenInstanceAuxMemberEntry(String name, Class clazz, CodegenExpression initializer) {
        this.name = name;
        this.clazz = clazz;
        this.initializer = initializer;
    }

    public String getName() {
        return name;
    }

    public Class getClazz() {
        return clazz;
    }

    public CodegenExpression getInitializer() {
        return initializer;
    }
}
