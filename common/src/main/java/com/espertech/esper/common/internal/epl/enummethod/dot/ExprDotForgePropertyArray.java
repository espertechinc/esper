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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEval;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEvalVisitor;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotForge;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;

import java.lang.reflect.Array;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast.castSafeFromObjectType;

public class ExprDotForgePropertyArray implements ExprDotEval, ExprDotForge {

    private final EventPropertyGetterSPI getter;
    private final EPType returnType;
    private final ExprNode indexExpression;
    private final Class arrayType;
    private final String propertyName;

    public ExprDotForgePropertyArray(EventPropertyGetterSPI getter, EPType returnType, ExprNode indexExpression, Class arrayType, String propertyName) {
        this.getter = getter;
        this.returnType = returnType;
        this.indexExpression = indexExpression;
        this.arrayType = arrayType;
        this.propertyName = propertyName;
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (!(target instanceof EventBean)) {
            return null;
        }
        Object array = getter.get((EventBean) target);
        if (array == null) {
            return null;
        }
        Integer index = (Integer) indexExpression.getForge().getExprEvaluator().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (index == null) {
            return null;
        }
        return Array.get(array, index);
    }

    public EPType getTypeInfo() {
        return returnType;
    }

    public void visit(ExprDotEvalVisitor visitor) {
        visitor.visitPropertySource();
    }

    public ExprDotEval getDotEvaluator() {
        return this;
    }

    public ExprDotForge getDotForge() {
        return this;
    }

    public CodegenExpression codegen(CodegenExpression inner, Class innerType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        Class type = EPTypeHelper.getCodegenReturnType(returnType);
        CodegenMethod method = parent.makeChild(type, ExprDotForgeProperty.class, classScope).addParam(innerType, "target").addParam(Integer.class, "index");
        method.getBlock()
            .ifNotInstanceOf("target", EventBean.class)
            .blockReturn(constantNull())
            .declareVar(arrayType, "array", castSafeFromObjectType(arrayType, getter.eventBeanGetCodegen(cast(EventBean.class, ref("target")), method, classScope)))
            .ifRefNullReturnNull("index")
            .ifCondition(relational(ref("index"), CodegenExpressionRelational.CodegenRelational.GE, arrayLength(ref("array"))))
            .blockThrow(newInstance(EPException.class, concat(constant("Array length "), arrayLength(ref("array")), constant(" less than index "), ref("index"), constant(" for property '" + propertyName + "'"))))
            .methodReturn(castSafeFromObjectType(type, arrayAtIndex(ref("array"), cast(int.class, ref("index")))));
        return localMethod(method, inner, indexExpression.getForge().evaluateCodegen(Integer.class, method, symbols, classScope));
    }
}
