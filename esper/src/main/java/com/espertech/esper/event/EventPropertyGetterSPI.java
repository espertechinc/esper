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
    CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenContext context);
    CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenContext context);
    CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenContext context);
    CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenContext context);
    CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenContext context);
    CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenContext context);
}
