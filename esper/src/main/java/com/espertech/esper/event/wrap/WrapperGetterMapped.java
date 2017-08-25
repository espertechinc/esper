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
package com.espertech.esper.event.wrap;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.event.DecoratingEventBean;
import com.espertech.esper.event.EventPropertyGetterMappedSPI;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class WrapperGetterMapped implements EventPropertyGetterMappedSPI {

    private final EventPropertyGetterMappedSPI undMapped;

    public WrapperGetterMapped(EventPropertyGetterMappedSPI undMapped) {
        this.undMapped = undMapped;
    }

    public Object get(EventBean event, String key) throws PropertyAccessException {
        if (!(event instanceof DecoratingEventBean)) {
            throw new PropertyAccessException("Mismatched property getter to EventBean type");
        }
        DecoratingEventBean wrapper = (DecoratingEventBean) event;
        EventBean wrapped = wrapper.getUnderlyingEvent();
        if (wrapped == null) {
            return null;
        }
        return undMapped.get(wrapped, key);
    }

    public CodegenExpression eventBeanGetMappedCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, CodegenExpression beanExpression, CodegenExpression key) {
        CodegenMethodNode method = codegenMethodScope.makeChild(Object.class, WrapperGetterMapped.class, codegenClassScope).addParam(EventBean.class, "event").addParam(String.class, "key").getBlock()
                .declareVar(DecoratingEventBean.class, "wrapper", cast(DecoratingEventBean.class, ref("event")))
                .declareVar(EventBean.class, "wrapped", exprDotMethod(ref("wrapper"), "getUnderlyingEvent"))
                .ifRefNullReturnNull("wrapped")
                .methodReturn(undMapped.eventBeanGetMappedCodegen(codegenMethodScope, codegenClassScope, ref("wrapped"), ref("key")));
        return localMethodBuild(method).pass(beanExpression).pass(key).call();
    }
};
