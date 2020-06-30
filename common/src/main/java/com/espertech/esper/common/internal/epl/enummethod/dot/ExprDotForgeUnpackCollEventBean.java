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
package com.espertech.esper.common.internal.epl.enummethod.dot;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.collection.EventUnderlyingCollection;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEval;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEvalVisitor;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotForge;
import com.espertech.esper.common.internal.rettype.EPChainableType;
import com.espertech.esper.common.internal.rettype.EPChainableTypeHelper;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprDotForgeUnpackCollEventBean implements ExprDotForge, ExprDotEval {

    private final EPChainableType typeInfo;

    public ExprDotForgeUnpackCollEventBean(EventType type) {
        typeInfo = EPChainableTypeHelper.collectionOfSingleValue(type.getUnderlyingEPType());
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (target == null) {
            return null;
        }
        Collection<EventBean> it = (Collection<EventBean>) target;
        return new EventUnderlyingCollection(it);
    }

    public CodegenExpression codegen(CodegenExpression inner, EPTypeClass innerType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod methodNode = parent.makeChild(EPTypePremade.COLLECTION.getEPType(), ExprDotForgeUnpackCollEventBean.class, classScope).addParam(EPTypePremade.COLLECTION.getEPType(), "target");

        methodNode.getBlock()
                .ifRefNullReturnNull("target")
                .methodReturn(newInstance(EventUnderlyingCollection.EPTYPE, ref("target")));
        return localMethod(methodNode, inner);
    }

    public EPChainableType getTypeInfo() {
        return typeInfo;
    }

    public void visit(ExprDotEvalVisitor visitor) {
        visitor.visitUnderlyingEventColl();
    }

    public ExprDotEval getDotEvaluator() {
        return this;
    }

    public ExprDotForge getDotForge() {
        return this;
    }
}
