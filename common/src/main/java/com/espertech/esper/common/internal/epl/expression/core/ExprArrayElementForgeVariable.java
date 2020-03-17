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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;

public class ExprArrayElementForgeVariable extends ExprArrayElementForge {
    private final VariableMetaData meta;

    public ExprArrayElementForgeVariable(ExprArrayElement parent, Class componentType, Class arrayType, VariableMetaData meta) {
        super(parent, componentType, arrayType);
        this.meta = meta;
    }

}
