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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEventEvaluatorForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class FilterSpecCompilerIndexLimitedLookupableGetterForge implements ExprEventEvaluatorForge {
    private final ExprNode lookupable;

    public FilterSpecCompilerIndexLimitedLookupableGetterForge(ExprNode lookupable) {
        this.lookupable = lookupable;
    }

    public CodegenExpression eventBeanWithCtxGet(CodegenExpression beanExpression, CodegenExpression ctxExpression, CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(Object.class, this.getClass(), classScope).addParam(EventBean.class, "eventBean").addParam(ExprEvaluatorContext.class, "ctx");
        CodegenMethod getImpl = CodegenLegoMethodExpression.codegenExpression(lookupable.getForge(), method, classScope);
        method.getBlock()
            .declareVar(EventBean[].class, "events", newArrayWithInit(EventBean.class, ref("eventBean")))
            .methodReturn(localMethod(getImpl, newArrayWithInit(EventBean.class, ref("eventBean")), constantTrue(), ref("ctx")));
        return localMethod(method, beanExpression, ctxExpression);
    }
}
