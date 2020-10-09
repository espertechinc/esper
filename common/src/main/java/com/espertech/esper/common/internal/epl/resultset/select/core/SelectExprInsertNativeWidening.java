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
package com.espertech.esper.common.internal.epl.resultset.select.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenRepetitiveLengthBuilder;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMayVoid;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.event.core.EventBeanManufacturer;
import com.espertech.esper.common.internal.event.core.EventBeanManufacturerForge;
import com.espertech.esper.common.internal.util.TypeWidenerSPI;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SelectExprInsertNativeWidening extends SelectExprInsertNativeBase {

    private final TypeWidenerSPI[] wideners;

    public SelectExprInsertNativeWidening(EventType eventType, EventBeanManufacturerForge eventManufacturer, ExprForge[] exprForges, TypeWidenerSPI[] wideners) {
        super(eventType, eventManufacturer, exprForges);
        this.wideners = wideners;
    }

    public CodegenMethod processCodegen(CodegenExpression resultEventType, CodegenExpression eventBeanFactory, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(EventBean.EPTYPE, this.getClass(), codegenClassScope);
        CodegenExpressionField manufacturer = codegenClassScope.addFieldUnshared(true, EventBeanManufacturer.EPTYPE, eventManufacturer.make(codegenMethodScope, codegenClassScope));
        methodNode.getBlock()
                .declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "values", newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(exprForges.length)));
        new CodegenRepetitiveLengthBuilder(exprForges.length, methodNode, codegenClassScope, this.getClass())
                .addParam(EPTypePremade.OBJECTARRAY.getEPType(), "values")
                .setConsumer((index, leaf) -> {
                    EPType evalType = exprForges[index].getEvaluationType();
                    CodegenExpression expression = CodegenLegoMayVoid.expressionMayVoid(evalType, exprForges[index], leaf, exprSymbol, codegenClassScope);
                    if (wideners[index] == null) {
                        leaf.getBlock().assignArrayElement("values", constant(index), expression);
                    } else {
                        String refname = "evalResult" + index;
                        if (evalType == null || evalType == EPTypeNull.INSTANCE) {
                            // no action
                        } else {
                            EPTypeClass evalClass = (EPTypeClass) evalType;
                            leaf.getBlock().declareVar(evalClass, refname, expression);
                            if (!evalClass.getType().isPrimitive()) {
                                leaf.getBlock().ifRefNotNull(refname)
                                        .assignArrayElement("values", constant(index), wideners[index].widenCodegen(ref(refname), leaf, codegenClassScope))
                                        .blockEnd();
                            } else {
                                leaf.getBlock().assignArrayElement("values", constant(index), wideners[index].widenCodegen(ref(refname), leaf, codegenClassScope));
                            }
                        }
                    }
                }).build();
        methodNode.getBlock().methodReturn(exprDotMethod(manufacturer, "make", ref("values")));
        return methodNode;
    }
}
