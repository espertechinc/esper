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

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SelectExprProcessorTypableSingleForge implements SelectExprProcessorTypableForge, ExprNodeRenderable {
    protected final ExprTypableReturnForge typable;
    protected final boolean hasWideners;
    protected final TypeWidenerSPI[] wideners;
    protected final EventBeanManufacturerForge factory;
    protected final EventType targetType;
    protected final boolean singleRowOnly;

    public SelectExprProcessorTypableSingleForge(ExprTypableReturnForge typable, boolean hasWideners, TypeWidenerSPI[] wideners, EventBeanManufacturerForge factory, EventType targetType, boolean singleRowOnly) {
        this.typable = typable;
        this.hasWideners = hasWideners;
        this.wideners = wideners;
        this.factory = factory;
        this.targetType = targetType;
        this.singleRowOnly = singleRowOnly;
    }

    public ExprEvaluator getExprEvaluator() {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpressionField manufacturer = codegenClassScope.addFieldUnshared(true, EventBeanManufacturer.class, factory.make(codegenMethodScope, codegenClassScope));

        if (singleRowOnly) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.class, SelectExprProcessorTypableSingleForge.class, codegenClassScope);

            CodegenBlock block = methodNode.getBlock()
                    .declareVar(Object[].class, "row", typable.evaluateTypableSingleCodegen(methodNode, exprSymbol, codegenClassScope))
                    .ifRefNullReturnNull("row");
            if (hasWideners) {
                block.expression(SelectExprProcessorHelper.applyWidenersCodegen(ref("row"), wideners, methodNode, codegenClassScope));
            }
            block.methodReturn(exprDotMethod(manufacturer, "make", ref("row")));
            return localMethod(methodNode);
        }

        CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean[].class, SelectExprProcessorTypableSingleForge.class, codegenClassScope);

        CodegenBlock block = methodNode.getBlock()
                .declareVar(Object[].class, "row", typable.evaluateTypableSingleCodegen(methodNode, exprSymbol, codegenClassScope))
                .ifRefNullReturnNull("row");
        if (hasWideners) {
            block.expression(SelectExprProcessorHelper.applyWidenersCodegen(ref("row"), wideners, methodNode, codegenClassScope));
        }
        block.declareVar(EventBean[].class, "events", newArrayByLength(EventBean.class, constant(1)))
                .assignArrayElement("events", constant(0), exprDotMethod(manufacturer, "make", ref("row")))
                .methodReturn(ref("events"));
        return localMethod(methodNode);
    }

    public Class getUnderlyingEvaluationType() {
        if (singleRowOnly) {
            return targetType.getUnderlyingType();
        }
        return JavaClassHelper.getArrayType(targetType.getUnderlyingType());
    }

    public Class getEvaluationType() {
        if (singleRowOnly) {
            return EventBean.class;
        }
        return EventBean[].class;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
        typable.getForgeRenderable().toEPL(writer, parentPrecedence);
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }
}
