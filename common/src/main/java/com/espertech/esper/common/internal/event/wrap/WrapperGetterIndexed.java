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
package com.espertech.esper.common.internal.event.wrap;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.core.DecoratingEventBean;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterIndexedSPI;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class WrapperGetterIndexed implements EventPropertyGetterIndexedSPI {
    private final EventPropertyGetterIndexedSPI undIndexed;

    public WrapperGetterIndexed(EventPropertyGetterIndexedSPI undIndexed) {
        this.undIndexed = undIndexed;
    }

    public Object get(EventBean event, int index) throws PropertyAccessException {
        if (!(event instanceof DecoratingEventBean)) {
            throw new PropertyAccessException("Mismatched property getter to EventBean type");
        }
        DecoratingEventBean wrapper = (DecoratingEventBean) event;
        EventBean wrapped = wrapper.getUnderlyingEvent();
        if (wrapped == null) {
            return null;
        }
        return undIndexed.get(wrapped, index);
    }

    public CodegenExpression eventBeanGetIndexedCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, CodegenExpression beanExpression, CodegenExpression key) {
        CodegenMethod method = codegenMethodScope.makeChild(Object.class, WrapperGetterIndexed.class, codegenClassScope).addParam(EventBean.class, "event").addParam(int.class, "index").getBlock()
                .declareVar(DecoratingEventBean.class, "wrapper", cast(DecoratingEventBean.class, ref("event")))
                .declareVar(EventBean.class, "wrapped", exprDotMethod(ref("wrapper"), "getUnderlyingEvent"))
                .ifRefNullReturnNull("wrapped")
                .methodReturn(undIndexed.eventBeanGetIndexedCodegen(codegenMethodScope, codegenClassScope, ref("wrapped"), ref("index")));
        return localMethodBuild(method).pass(beanExpression).pass(key).call();
    }
}
