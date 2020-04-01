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
package com.espertech.esper.common.internal.epl.updatehelper;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EventBeanUpdateItemArray {

    private final String propertyName;
    private final ExprNode indexExpression;
    private final Class arrayType;
    private final EventPropertyGetterSPI getter;

    public EventBeanUpdateItemArray(String propertyName, ExprNode indexExpression, Class arrayType, EventPropertyGetterSPI getter) {
        this.propertyName = propertyName;
        this.indexExpression = indexExpression;
        this.arrayType = arrayType;
        this.getter = getter;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Class getArrayType() {
        return arrayType;
    }

    public EventBeanUpdateItemArrayExpressions getArrayExpressions(CodegenMethodScope parentScope, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenExpression index = indexExpression.getForge().evaluateCodegen(Integer.class, parentScope, symbols, classScope);
        CodegenExpression arrayGet = evaluateArrayCodegen(parentScope, symbols, classScope);
        return new EventBeanUpdateItemArrayExpressions(index, arrayGet);
    }

    private CodegenExpression evaluateArrayCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(arrayType, this.getClass(), classScope);
        method.getBlock()
            .declareVar(EventBean.class, "event", arrayAtIndex(symbols.getAddEPS(method), constant(0)))
            .ifRefNullReturnNull("event")
            .methodReturn(CodegenLegoCast.castSafeFromObjectType(arrayType, getter.eventBeanGetCodegen(ref("event"), method, classScope)));
        return localMethod(method);
    }
}
