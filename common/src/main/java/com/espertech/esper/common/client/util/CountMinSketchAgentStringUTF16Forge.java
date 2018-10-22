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
package com.espertech.esper.common.client.util;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

/**
 * Count-min sketch agent that handles String-type values and uses UTF-16 encoding
 * to transform strings to byte-array and back.
 */
public class CountMinSketchAgentStringUTF16Forge implements CountMinSketchAgentForge {
    public Class[] getAcceptableValueTypes() {
        return new Class[]{String.class};
    }

    public CodegenExpression codegenMake(CodegenMethod parent, CodegenClassScope classScope) {
        return newInstance(CountMinSketchAgentStringUTF16.class);
    }
}
