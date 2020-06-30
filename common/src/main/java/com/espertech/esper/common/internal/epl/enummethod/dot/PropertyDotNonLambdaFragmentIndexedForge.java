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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
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

    public EPTypeClass getEvaluationType() {
        return EventBean.EPTYPE;
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

    public CodegenExpression evaluateCodegen(EPTypeClass requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(EventBean.EPTYPE, PropertyDotNonLambdaFragmentIndexedForge.class, classScope);
        CodegenExpressionRef refEPS = symbols.getAddEPS(method);
        method.getBlock()
                .declareVar(EventBean.EPTYPE, "event", arrayAtIndex(refEPS, constant(streamId)))
                .ifRefNullReturnNull("event")
                .declareVar(EventBean.EPTYPEARRAY, "array", cast(EventBean.EPTYPEARRAY, getter.eventBeanFragmentCodegen(ref("event"), method, classScope)))
                .declareVar(EPTypePremade.INTEGERBOXED.getEPType(), "index", indexExpr.getForge().evaluateCodegen(EPTypePremade.INTEGERBOXED.getEPType(), method, symbols, classScope))
                .ifRefNullReturnNull("index")
                .ifCondition(relational(ref("index"), CodegenExpressionRelational.CodegenRelational.GE, arrayLength(ref("array"))))
                .blockThrow(newInstance(EPException.EPTYPE, concat(constant("Array length "), arrayLength(ref("array")), constant(" less than index "), ref("index"), constant(" for property '" + propertyName + "'"))))
                .methodReturn(CodegenLegoCast.castSafeFromObjectType(EventBean.EPTYPE, arrayAtIndex(ref("array"), cast(EPTypePremade.INTEGERPRIMITIVE.getEPType(), ref("index")))));
        return localMethod(method);
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence, ExprNodeRenderableFlags flags) {
        writer.append(this.getClass().getSimpleName());
    }
}
