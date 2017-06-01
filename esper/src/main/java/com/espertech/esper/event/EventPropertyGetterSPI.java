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
package com.espertech.esper.event;

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;

public interface EventPropertyGetterSPI extends EventPropertyGetter {
    CodegenExpression codegenEventBeanGet(CodegenExpression beanExpression, CodegenContext context);
    CodegenExpression codegenEventBeanExists(CodegenExpression beanExpression, CodegenContext context);
    CodegenExpression codegenEventBeanFragment(CodegenExpression beanExpression, CodegenContext context);
    CodegenExpression codegenUnderlyingGet(CodegenExpression underlyingExpression, CodegenContext context);
    CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context);
    CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context);
}
