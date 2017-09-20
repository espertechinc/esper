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
package com.espertech.esper.event.bean;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.core.engineimport.EngineImportException;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.util.JavaClassHelper;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastConstructor;

import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class InstanceManufacturerUtil {

    public static Pair<FastConstructor, ExprForge[]> getManufacturer(Class targetClass, EngineImportService engineImportService, ExprForge[] exprForges, Object[] expressionReturnTypes)
            throws ExprValidationException {
        Class[] ctorTypes = new Class[expressionReturnTypes.length];
        ExprForge[] forges = new ExprForge[exprForges.length];

        for (int i = 0; i < expressionReturnTypes.length; i++) {
            Object columnType = expressionReturnTypes[i];

            if (columnType instanceof Class || columnType == null) {
                ctorTypes[i] = (Class) expressionReturnTypes[i];
                forges[i] = exprForges[i];
                continue;
            }

            if (columnType instanceof EventType) {
                EventType columnEventType = (EventType) columnType;
                final Class returnType = columnEventType.getUnderlyingType();
                final ExprForge inner = exprForges[i];
                forges[i] = new InstanceManufacturerForgeNonArray(returnType, inner);
                ctorTypes[i] = returnType;
                continue;
            }

            // handle case where the select-clause contains an fragment array
            if (columnType instanceof EventType[]) {
                EventType columnEventType = ((EventType[]) columnType)[0];
                Class componentReturnType = columnEventType.getUnderlyingType();
                ExprForge inner = exprForges[i];
                forges[i] = new InstanceManufacturerForgeArray(componentReturnType, inner);
                continue;
            }

            String message = "Invalid assignment of expression " + i + " returning type '" + columnType +
                    "', column and parameter types mismatch";
            throw new ExprValidationException(message);
        }

        try {
            Constructor ctor = engineImportService.resolveCtor(targetClass, ctorTypes);
            FastClass fastClass = FastClass.create(engineImportService.getFastClassClassLoader(targetClass), targetClass);
            return new Pair<>(fastClass.getConstructor(ctor), forges);
        } catch (EngineImportException ex) {
            throw new ExprValidationException("Failed to find a suitable constructor for class '" + targetClass.getName() + "': " + ex.getMessage(), ex);
        }
    }

    public static class InstanceManufacturerForgeNonArray implements ExprForge {
        private final Class returnType;
        private final ExprForge innerForge;

        InstanceManufacturerForgeNonArray(Class returnType, ExprForge innerForge) {
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

        public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(returnType, InstanceManufacturerForgeNonArray.class, codegenClassScope);


            methodNode.getBlock()
                    .declareVar(EventBean.class, "event", cast(EventBean.class, innerForge.evaluateCodegen(requiredType, methodNode, exprSymbol, codegenClassScope)))
                    .ifRefNullReturnNull("event")
                    .methodReturn(cast(returnType, exprDotUnderlying(ref("event"))));
            return localMethod(methodNode);
        }

        public ExprForgeComplexityEnum getComplexity() {
            return ExprForgeComplexityEnum.INTER;
        }

        public Class getEvaluationType() {
            return returnType;
        }

        public ExprNodeRenderable getForgeRenderable() {
            return innerForge.getForgeRenderable();
        }
    }

    public static class InstanceManufacturerForgeArray implements ExprForge, ExprNodeRenderable {
        private final Class componentReturnType;
        private final ExprForge innerForge;

        InstanceManufacturerForgeArray(Class componentReturnType, ExprForge innerForge) {
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
                    Object values = Array.newInstance(componentReturnType, events.length);
                    for (int i = 0; i < events.length; i++) {
                        Array.set(values, i, events[i].getUnderlying());
                    }
                    return values;
                }
            };
        }

        public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            Class arrayType = JavaClassHelper.getArrayType(componentReturnType);
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(arrayType, InstanceManufacturerForgeArray.class, codegenClassScope);

            methodNode.getBlock()
                    .declareVar(Object.class, "result", innerForge.evaluateCodegen(requiredType, methodNode, exprSymbol, codegenClassScope))
                    .ifCondition(not(instanceOf(ref("result"), EventBean[].class)))
                    .blockReturn(constantNull())
                    .declareVar(EventBean[].class, "events", cast(EventBean[].class, ref("result")))
                    .declareVar(arrayType, "values", newArrayByLength(componentReturnType, arrayLength(ref("events"))))
                    .forLoopIntSimple("i", arrayLength(ref("events")))
                    .assignArrayElement("values", ref("i"), cast(componentReturnType, exprDotMethod(arrayAtIndex(ref("events"), ref("i")), "getUnderlying")))
                    .blockEnd()
                    .methodReturn(ref("values"));
            return localMethod(methodNode);
        }

        public ExprForgeComplexityEnum getComplexity() {
            return ExprForgeComplexityEnum.INTER;
        }

        public Class getEvaluationType() {
            return JavaClassHelper.getArrayType(componentReturnType);
        }

        public ExprNodeRenderable getForgeRenderable() {
            return this;
        }

        public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
            writer.append(this.getClass().getSimpleName());
        }
    }
}
