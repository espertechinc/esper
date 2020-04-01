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
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class PropertyDotNonLambdaFragmentIndexedForge implements ExprForge, ExprEvaluator, ExprNodeRenderable {

    private final int streamId;
    private final EventPropertyGetterSPI getter;
    private final ExprNode indexExpr;
    private final String propertyName;

    public PropertyDotNonLambdaFragmentIndexedForge(int streamId, EventPropertyGetterSPI getter, ExprNode indexExpr, String propertyName) {
        this.streamId = streamId;
        this.getter = getter;
        this.indexExpr = indexExpr;
        this.propertyName = propertyName;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public Class getEvaluationType() {
        return EventBean.class;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        EventBean event = eventsPerStream[streamId];
        if (event == null) {
            return null;
        }
        Object result = getter.getFragment(event);
        if (result == null || !result.getClass().isArray()) {
            return null;
        }
        EventBean[] events = (EventBean[]) result;
        Integer index = (Integer) indexExpr.getForge().getExprEvaluator().evaluate(eventsPerStream, isNewData, context);
        if (index == null) {
            return null;
        }
        return events[index];
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(EventBean.class, PropertyDotNonLambdaFragmentIndexedForge.class, classScope);
        CodegenExpressionRef refEPS = symbols.getAddEPS(method);
        method.getBlock()
                .declareVar(EventBean.class, "event", arrayAtIndex(refEPS, constant(streamId)))
                .ifRefNullReturnNull("event")
                .declareVar(EventBean[].class, "array", cast(EventBean[].class, getter.eventBeanFragmentCodegen(ref("event"), method, classScope)))
                .declareVar(Integer.class, "index", indexExpr.getForge().evaluateCodegen(Integer.class, method, symbols, classScope))
                .ifRefNullReturnNull("index")
                .ifCondition(relational(ref("index"), CodegenExpressionRelational.CodegenRelational.GE, arrayLength(ref("array"))))
                .blockThrow(newInstance(EPException.class, concat(constant("Array length "), arrayLength(ref("array")), constant(" less than index "), ref("index"), constant(" for property '" + propertyName + "'"))))
                .methodReturn(CodegenLegoCast.castSafeFromObjectType(EventBean.class, arrayAtIndex(ref("array"), cast(int.class, ref("index")))));
        return localMethod(method);
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
        writer.append(this.getClass().getSimpleName());
    }
}
