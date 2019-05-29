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
import com.espertech.esper.common.internal.event.json.parser.forge.JsonDelegateForge;
import com.espertech.esper.common.internal.event.json.parser.forge.JsonDelegateRefs;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class JsonDelegateForgeByClass implements JsonDelegateForge {
    private final Class clazz;
    private final CodegenExpression[] parameters;

    public JsonDelegateForgeByClass(Class clazz) {
        this.clazz = clazz;
        this.parameters = new CodegenExpression[0];
    }

    public JsonDelegateForgeByClass(Class clazz, CodegenExpression... params) {
        this.clazz = clazz;
        this.parameters = params;
    }

    public CodegenExpression newDelegate(JsonDelegateRefs fields, CodegenMethod method, CodegenClassScope classScope) {
        CodegenExpression[] allParams = new CodegenExpression[2 + parameters.length];
        allParams[0] = fields.getBaseHandler();
        allParams[1] = fields.getThis();
        System.arraycopy(parameters, 0, allParams, 2, parameters.length);
        return newInstance(clazz, allParams);
    }
}
