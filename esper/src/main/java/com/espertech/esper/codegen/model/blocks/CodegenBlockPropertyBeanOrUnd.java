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
package com.espertech.esper.codegen.model.blocks;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.EventPropertyGetterSPI;

import static com.espertech.esper.codegen.model.blocks.CodegenBlockPropertyBeanOrUnd.AccessType.EXISTS;
import static com.espertech.esper.codegen.model.blocks.CodegenBlockPropertyBeanOrUnd.AccessType.FRAGMENT;
import static com.espertech.esper.codegen.model.blocks.CodegenBlockPropertyBeanOrUnd.AccessType.GET;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.cast;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

/**
 *  if (!(valueMap instanceof TYPE)) {
 *    if (value instanceof EventBean) {
 *      return getter.XXX((EventBean) value);
 *    }
 *    return XXXX;
 *  }
 *  return getter.getXXXX(value);
 */
public class CodegenBlockPropertyBeanOrUnd {
    public static String from(CodegenContext context, Class expectedUnderlyingType, EventPropertyGetterSPI innerGetter, AccessType accessType, Class generator) {
        CodegenBlock block = context.addMethod((accessType == EXISTS ? boolean.class : Object.class), Object.class, "value", generator)
                .ifNotInstanceOf("value", expectedUnderlyingType)
                    .ifInstanceOf("value", EventBean.class)
                        .declareVarWCast(EventBean.class, "bean", "value");

        if (accessType == GET) {
            block = block.blockReturn(innerGetter.codegenEventBeanGet(ref("bean"), context));
        }
        else if (accessType == EXISTS) {
            block = block.blockReturn(innerGetter.codegenEventBeanExists(ref("bean"), context));
        }
        else if (accessType == FRAGMENT) {
            block = block.blockReturn(innerGetter.codegenEventBeanFragment(ref("bean"), context));
        }
        else {
            throw new UnsupportedOperationException("Invalid access type " + accessType);
        }

        block = block.blockReturn(constant(accessType == EXISTS ? false : null));

        CodegenExpression expression;
        if (accessType == GET) {
            expression = innerGetter.codegenUnderlyingGet(cast(expectedUnderlyingType, ref("value")), context);
        }
        else if (accessType == EXISTS) {
            expression = innerGetter.codegenUnderlyingExists(cast(expectedUnderlyingType, ref("value")), context);
        }
        else if (accessType == FRAGMENT) {
            expression = innerGetter.codegenUnderlyingFragment(cast(expectedUnderlyingType, ref("value")), context);
        }
        else {
            throw new UnsupportedOperationException("Invalid access type " + accessType);
        }
        return block.methodReturn(expression);
    }

    public enum AccessType {
        GET,
        EXISTS,
        FRAGMENT
    }
}
