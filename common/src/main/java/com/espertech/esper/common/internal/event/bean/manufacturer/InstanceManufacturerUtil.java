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
package com.espertech.esper.common.internal.event.bean.manufacturer;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.settings.ClasspathImportException;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class InstanceManufacturerUtil {

    public static Pair<Constructor, ExprForge[]> getManufacturer(EPTypeClass targetClass, ClasspathImportServiceCompileTime classpathImportService, ExprForge[] exprForges, Object[] expressionReturnTypes)
            throws ExprValidationException {
        EPType[] ctorTypes = new EPType[expressionReturnTypes.length];
        ExprForge[] forges = new ExprForge[exprForges.length];

        for (int i = 0; i < expressionReturnTypes.length; i++) {
            Object columnType = expressionReturnTypes[i];

            if (columnType == null) {
                forges[i] = exprForges[i];
                continue;
            }

            if (columnType instanceof EPType) {
                ctorTypes[i] = (EPType) columnType;
                forges[i] = exprForges[i];
                continue;
            }

            if (columnType instanceof EventType) {
                EventType columnEventType = (EventType) columnType;
                final EPTypeClass returnType = columnEventType.getUnderlyingEPType();
                final ExprForge inner = exprForges[i];
                forges[i] = new InstanceManufacturerForgeNonArray(returnType, inner);
                ctorTypes[i] = returnType;
                continue;
            }

            // handle case where the select-clause contains an fragment array
            if (columnType instanceof EventType[]) {
                EventType columnEventType = ((EventType[]) columnType)[0];
                EPTypeClass componentReturnType = columnEventType.getUnderlyingEPType();
                ExprForge inner = exprForges[i];
                forges[i] = new InstanceManufacturerForgeArray(componentReturnType, inner);
                continue;
            }

            String message = "Invalid assignment of expression " + i + " returning type '" + columnType +
                    "', column and parameter types mismatch";
            throw new ExprValidationException(message);
        }

        try {
            Constructor ctor = classpathImportService.resolveCtor(targetClass.getType(), ctorTypes);
            return new Pair<>(ctor, forges);
        } catch (ClasspathImportException ex) {
            throw new ExprValidationException("Failed to find a suitable constructor for class '" + targetClass.getTypeName() + "': " + ex.getMessage(), ex);
        }
    }

    public static class InstanceManufacturerForgeNonArray implements ExprForge {
        private final EPTypeClass returnType;
        private final ExprForge innerForge;

        InstanceManufacturerForgeNonArray(EPTypeClass returnType, ExprForge innerForge) {
            this.returnType = returnType;
            this.innerForge = innerForge;
        }

        public ExprEvaluator getExprEvaluator() {
            final ExprEvaluator inner = innerForge.getExprEvaluator();
            return new ExprEvaluator() {
                public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                    EventBean event = (EventBean) inner.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                    if (event != null) {
                        return event.getUnderlying();
                    }
                    return null;
                }
            };
        }

        public ExprForgeConstantType getForgeConstantType() {
            return ExprForgeConstantType.NONCONST;
        }

        public CodegenExpression evaluateCodegen(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(returnType, InstanceManufacturerForgeNonArray.class, codegenClassScope);

            methodNode.getBlock()
                    .declareVar(EventBean.EPTYPE, "event", cast(EventBean.EPTYPE, innerForge.evaluateCodegen(requiredType, methodNode, exprSymbol, codegenClassScope)))
                    .ifRefNullReturnNull("event")
                    .methodReturn(cast(returnType, exprDotUnderlying(ref("event"))));
            return localMethod(methodNode);
        }

        public EPTypeClass getEvaluationType() {
            return returnType;
        }

        public ExprNodeRenderable getForgeRenderable() {
            return innerForge.getForgeRenderable();
        }
    }

    public static class InstanceManufacturerForgeArray implements ExprForge, ExprNodeRenderable {
        private final EPTypeClass componentReturnType;
        private final ExprForge innerForge;

        InstanceManufacturerForgeArray(EPTypeClass componentReturnType, ExprForge innerForge) {
            this.componentReturnType = componentReturnType;
            this.innerForge = innerForge;
        }

        public ExprEvaluator getExprEvaluator() {
            final ExprEvaluator inner = innerForge.getExprEvaluator();
            return new ExprEvaluator() {
                public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                    Object result = inner.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                    if (!(result instanceof EventBean[])) {
                        return null;
                    }
                    EventBean[] events = (EventBean[]) result;
                    Object values = Array.newInstance(componentReturnType.getType(), events.length);
                    for (int i = 0; i < events.length; i++) {
                        Array.set(values, i, events[i].getUnderlying());
                    }
                    return values;
                }
            };
        }

        public ExprForgeConstantType getForgeConstantType() {
            return ExprForgeConstantType.NONCONST;
        }

        public CodegenExpression evaluateCodegen(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            EPTypeClass arrayType = JavaClassHelper.getArrayType(componentReturnType);
            CodegenMethod methodNode = codegenMethodScope.makeChild(arrayType, InstanceManufacturerForgeArray.class, codegenClassScope);

            methodNode.getBlock()
                    .declareVar(EPTypePremade.OBJECT.getEPType(), "result", innerForge.evaluateCodegen(requiredType, methodNode, exprSymbol, codegenClassScope))
                    .ifCondition(not(instanceOf(ref("result"), EventBean.EPTYPEARRAY)))
                    .blockReturn(constantNull())
                    .declareVar(EventBean.EPTYPEARRAY, "events", cast(EventBean.EPTYPEARRAY, ref("result")))
                    .declareVar(arrayType, "values", newArrayByLength(componentReturnType, arrayLength(ref("events"))))
                    .forLoopIntSimple("i", arrayLength(ref("events")))
                    .assignArrayElement("values", ref("i"), cast(componentReturnType, exprDotMethod(arrayAtIndex(ref("events"), ref("i")), "getUnderlying")))
                    .blockEnd()
                    .methodReturn(ref("values"));
            return localMethod(methodNode);
        }

        public EPTypeClass getEvaluationType() {
            return JavaClassHelper.getArrayType(componentReturnType);
        }

        public ExprNodeRenderable getForgeRenderable() {
            return this;
        }

        public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence, ExprNodeRenderableFlags flags) {
            writer.append(this.getClass().getSimpleName());
        }
    }
}
