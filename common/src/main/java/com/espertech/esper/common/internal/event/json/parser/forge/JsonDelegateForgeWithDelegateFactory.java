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
package com.espertech.esper.common.internal.event.json.parser.forge;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateBase;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateFactory;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class JsonDelegateForgeWithDelegateFactory implements JsonDelegateForge {

    private final String delegateFactoryClassName;

    public JsonDelegateForgeWithDelegateFactory(String delegateFactoryClassName) {
        this.delegateFactoryClassName = delegateFactoryClassName;
    }

    public CodegenExpression newDelegate(JsonDelegateRefs fields, CodegenMethod parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(JsonDelegateBase.class, JsonForgeFactoryEventTypeTyped.class, classScope);
        method.getBlock()
            .declareVar(JsonDelegateFactory.class, "factory", newInstance(delegateFactoryClassName))
            .methodReturn(exprDotMethod(ref("factory"), "make", fields.getBaseHandler(), fields.getThis()));
        return localMethod(method);
    }
}
