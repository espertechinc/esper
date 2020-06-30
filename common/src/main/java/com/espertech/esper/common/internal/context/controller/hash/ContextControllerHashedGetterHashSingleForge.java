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
package com.espertech.esper.common.internal.context.controller.hash;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.event.core.EventPropertyValueGetterForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ContextControllerHashedGetterHashSingleForge implements EventPropertyValueGetterForge {

    private final ExprNode eval;
    private final int granularity;

    public ContextControllerHashedGetterHashSingleForge(ExprNode eval, int granularity) {
        this.eval = eval;
        this.granularity = granularity;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param code        code
     * @param granularity granularity
     * @return hash
     */
    public static int objectToNativeHash(Object code, int granularity) {
        int value;
        if (code == null) {
            value = 0;
        } else {
            value = code.hashCode() % granularity;
        }

        if (value >= 0) {
            return value;
        }
        return -value;
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(EPTypePremade.OBJECT.getEPType(), this.getClass(), classScope).addParam(EventBean.EPTYPE, "eventBean");
        CodegenMethod methodExpr = CodegenLegoMethodExpression.codegenExpression(eval.getForge(), method, classScope);
        method.getBlock()
                .declareVar(EventBean.EPTYPEARRAY, "events", newArrayWithInit(EventBean.EPTYPE, ref("eventBean")))
                .declareVar(EPTypePremade.OBJECT.getEPType(), "code", localMethod(methodExpr, ref("events"), constantTrue(), constantNull()))
                .methodReturn(staticMethod(ContextControllerHashedGetterHashSingleForge.class, "objectToNativeHash", ref("code"), constant(granularity)));

        return localMethod(method, beanExpression);
    }
}
