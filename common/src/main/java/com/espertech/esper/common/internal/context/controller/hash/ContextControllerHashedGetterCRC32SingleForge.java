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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.event.core.EventPropertyValueGetterForge;

import java.util.zip.CRC32;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ContextControllerHashedGetterCRC32SingleForge implements EventPropertyValueGetterForge {

    private final ExprNode eval;
    private final int granularity;

    public ContextControllerHashedGetterCRC32SingleForge(ExprNode eval, int granularity) {
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
    public static int stringToCRC32Hash(String code, int granularity) {
        long value;
        if (code == null) {
            value = 0;
        } else {
            CRC32 crc = new CRC32();
            crc.update(code.getBytes());
            value = crc.getValue() % granularity;
        }

        int result = (int) value;
        if (result >= 0) {
            return result;
        }
        return -result;
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(Object.class, this.getClass(), classScope).addParam(EventBean.class, "eventBean");
        CodegenMethod methodExpr = CodegenLegoMethodExpression.codegenExpression(eval.getForge(), method, classScope);
        method.getBlock()
                .declareVar(EventBean[].class, "events", newArrayWithInit(EventBean.class, ref("eventBean")))
                .declareVar(String.class, "code", cast(String.class, localMethod(methodExpr, ref("events"), constantTrue(), constantNull())))
                .methodReturn(staticMethod(ContextControllerHashedGetterCRC32SingleForge.class, "stringToCRC32Hash", ref("code"), constant(granularity)));

        return localMethod(method, beanExpression);
    }
}
