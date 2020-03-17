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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprArrayElementForgeProperty extends ExprArrayElementForge {
    private final int streamNum;
    private final EventPropertyGetterSPI getter;

    public ExprArrayElementForgeProperty(ExprArrayElement parent, Class componentType, Class arrayType, int streamNum, EventPropertyGetterSPI getter) {
        super(parent, componentType, arrayType);
        this.streamNum = streamNum;
        this.getter = getter;
    }

    public CodegenExpression evaluateArrayCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(arrayType, this.getClass(), classScope);
        method.getBlock()
            .declareVar(EventBean.class, "event", arrayAtIndex(symbols.getAddEPS(method), constant(streamNum)))
            .ifRefNullReturnNull("event")
            .methodReturn(CodegenLegoCast.castSafeFromObjectType(arrayType, getter.eventBeanGetCodegen(ref("event"), method, classScope)));
        return localMethod(method);
    }

    public ExprArrayElementIdentNodeExpressions getArrayExpressions(CodegenMethodScope parentScope, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenExpression index = parent.getChildNodes()[0].getForge().evaluateCodegen(Integer.class, parentScope, symbols, classScope);
        CodegenExpression arrayGet = evaluateArrayCodegen(parentScope, symbols, classScope);
        return new ExprArrayElementIdentNodeExpressions(index, arrayGet);
    }

    public int getStreamNum() {
        return streamNum;
    }

    public EventPropertyGetterSPI getGetter() {
        return getter;
    }
}
