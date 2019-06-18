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
package com.espertech.esper.common.internal.event.json.parser.delegates.endvalue;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.cast;

public class JsonEndValueForgeCast implements JsonEndValueForge {
    private final Class target;
    private final String targetClassName;

    public JsonEndValueForgeCast(Class target) {
        this.target = target;
        this.targetClassName = null;
    }

    public JsonEndValueForgeCast(String targetClassName) {
        this.targetClassName = targetClassName;
        this.target = null;
    }

    public CodegenExpression captureValue(JsonEndValueRefs refs, CodegenMethod method, CodegenClassScope classScope) {
        if (target != null) {
            return cast(target, refs.getValueObject());
        }
        return cast(targetClassName, refs.getValueObject());
    }
}
