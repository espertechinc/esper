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
package com.espertech.esper.common.internal.epl.resultset.select.typable;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorHelper;
import com.espertech.esper.common.internal.event.core.EventBeanManufacturer;
import com.espertech.esper.common.internal.event.core.EventBeanManufacturerForge;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.TypeWidenerSPI;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SelectExprProcessorTypableMultiForge implements SelectExprProcessorTypableForge {

    protected final ExprTypableReturnForge typable;
    protected final boolean hasWideners;
    protected final TypeWidenerSPI[] wideners;
    protected final EventBeanManufacturerForge factory;
    protected final EventType targetType;
    protected final boolean firstRowOnly;

    public SelectExprProcessorTypableMultiForge(ExprTypableReturnForge typable, boolean hasWideners, TypeWidenerSPI[] wideners, EventBeanManufacturerForge factory, EventType targetType, boolean firstRowOnly) {
        this.typable = typable;
        this.hasWideners = hasWideners;
        this.wideners = wideners;
        this.factory = factory;
        this.targetType = targetType;
        this.firstRowOnly = firstRowOnly;
    }

    public ExprEvaluator getExprEvaluator() {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpressionField manufacturer = codegenClassScope.addFieldUnshared(true, EventBeanManufacturer.class, factory.make(codegenMethodScope, codegenClassScope));

        if (firstRowOnly) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.class, SelectExprProcessorTypableMultiForge.class, codegenClassScope);

            CodegenBlock block = methodNode.getBlock()
                    .declareVar(Object[].class, "row", typable.evaluateTypableSingleCodegen(methodNode, exprSymbol, codegenClassScope))
                    .ifRefNullReturnNull("row");
            if (hasWideners) {
                block.expression(SelectExprProcessorHelper.applyWidenersCodegen(ref("row"), wideners, methodNode, codegenClassScope));
            }
            block.methodReturn(exprDotMethod(manufacturer, "make", ref("row")));
            return localMethod(methodNode);
        }

        CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean[].class, SelectExprProcessorTypableMultiForge.class, codegenClassScope);

        CodegenBlock block = methodNode.getBlock()
                .declareVar(Object[][].class, "rows", typable.evaluateTypableMultiCodegen(methodNode, exprSymbol, codegenClassScope))
                .ifRefNullReturnNull("rows")
                .ifCondition(equalsIdentity(arrayLength(ref("rows")), constant(0)))
                .blockReturn(newArrayByLength(EventBean.class, constant(0)));
        if (hasWideners) {
            block.expression(SelectExprProcessorHelper.applyWidenersCodegenMultirow(ref("rows"), wideners, methodNode, codegenClassScope));
        }
        block.declareVar(EventBean[].class, "events", newArrayByLength(EventBean.class, arrayLength(ref("rows"))))
                .forLoopIntSimple("i", arrayLength(ref("events")))
                .assignArrayElement("events", ref("i"), exprDotMethod(manufacturer, "make", arrayAtIndex(ref("rows"), ref("i"))))
                .blockEnd()
                .methodReturn(ref("events"));
        return localMethod(methodNode);
    }

    public Class getUnderlyingEvaluationType() {
        if (firstRowOnly) {
            return targetType.getUnderlyingType();
        }
        return JavaClassHelper.getArrayType(targetType.getUnderlyingType());
    }

    public Class getEvaluationType() {
        if (firstRowOnly) {
            return EventBean.class;
        }
        return EventBean[].class;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return typable.getForgeRenderable();
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }
}
