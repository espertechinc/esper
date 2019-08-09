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
package com.espertech.esper.common.internal.epl.datetime.eval;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.datetime.calop.CalendarForge;
import com.espertech.esper.common.internal.epl.datetime.dtlocal.*;
import com.espertech.esper.common.internal.epl.datetime.interval.IntervalForge;
import com.espertech.esper.common.internal.epl.datetime.reformatop.ReformatForge;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEval;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEvalVisitor;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotForge;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.rettype.ClassEPType;
import com.espertech.esper.common.internal.rettype.ClassMultiValuedEPType;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class ExprDotDTForge implements ExprDotForge {
    private final EPType returnType;
    private final DTLocalForge forge;

    ExprDotDTForge(List<CalendarForge> calendarForges, TimeAbacus timeAbacus, ReformatForge reformatForge, IntervalForge intervalForge, Class inputType, EventType inputEventType) {
        if (intervalForge != null) {
            returnType = EPTypeHelper.singleValue(Boolean.class);
        } else if (reformatForge != null) {
            returnType = EPTypeHelper.singleValue(reformatForge.getReturnType());
        } else {  // only calendar op
            if (inputEventType != null) {
                returnType = EPTypeHelper.singleValue(inputEventType.getPropertyType(inputEventType.getStartTimestampPropertyName()));
            } else {
                returnType = EPTypeHelper.singleValue(inputType == java.sql.Date.class ? Date.class : inputType);
            }
        }

        this.forge = getForge(calendarForges, timeAbacus, inputType, inputEventType, reformatForge, intervalForge);
    }

    public ExprDotEval getDotEvaluator() {
        final DTLocalEvaluator evaluator = forge.getDTEvaluator();
        final ExprDotForge exprDotForge = this;
        return new ExprDotEval() {
            public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                if (target == null) {
                    return null;
                }
                return evaluator.evaluate(target, eventsPerStream, isNewData, exprEvaluatorContext);
            }

            public ExprDotForge getDotForge() {
                return exprDotForge;
            }
        };
    }

    public CodegenExpression codegen(CodegenExpression inner, Class innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        Class methodReturnType = returnType instanceof ClassEPType ? ((ClassEPType) returnType).getType() : ((ClassMultiValuedEPType) returnType).getContainer();
        CodegenMethod methodNode = codegenMethodScope.makeChild(methodReturnType, ExprDotDTForge.class, codegenClassScope).addParam(innerType, "target");

        CodegenBlock block = methodNode.getBlock();
        if (!innerType.isPrimitive()) {
            block.ifRefNullReturnNull("target");
        }
        block.methodReturn(forge.codegen(ref("target"), innerType, methodNode, exprSymbol, codegenClassScope));
        return localMethod(methodNode, inner);
    }

    public EPType getTypeInfo() {
        return returnType;
    }

    public void visit(ExprDotEvalVisitor visitor) {
        visitor.visitDateTime();
    }

    public DTLocalForge getForge(List<CalendarForge> calendarForges, TimeAbacus timeAbacus, Class inputType, EventType inputEventType, ReformatForge reformatForge, IntervalForge intervalForge) {
        if (inputEventType == null) {
            if (reformatForge != null) {
                if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, Calendar.class)) {
                    if (calendarForges.isEmpty()) {
                        return new DTLocalCalReformatForge(reformatForge);
                    }
                    return new DTLocalCalOpsReformatForge(calendarForges, reformatForge);
                } else if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, Date.class)) {
                    if (calendarForges.isEmpty()) {
                        return new DTLocalDateReformatForge(reformatForge);
                    }
                    return new DTLocalDateOpsReformatForge(calendarForges, reformatForge);
                } else if (JavaClassHelper.getBoxedType(inputType) == Long.class) {
                    if (calendarForges.isEmpty()) {
                        return new DTLocalLongReformatForge(reformatForge);
                    }
                    return new DTLocalLongOpsReformatForge(calendarForges, reformatForge, timeAbacus);
                } else if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, LocalDateTime.class)) {
                    if (calendarForges.isEmpty()) {
                        return new DTLocalLocalDateTimeReformatForge(reformatForge);
                    }
                    return new DTLocalLocalDateTimeOpsReformatForge(calendarForges, reformatForge);
                } else if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, ZonedDateTime.class)) {
                    if (calendarForges.isEmpty()) {
                        return new DTLocalZonedDateTimeReformatForge(reformatForge);
                    }
                    return new DTLocalZonedDateTimeOpsReformatForge(calendarForges, reformatForge);
                }
            } else if (intervalForge != null) {
                if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, Calendar.class)) {
                    if (calendarForges.isEmpty()) {
                        return new DTLocalCalIntervalForge(intervalForge);
                    }
                    return new DTLocalCalOpsIntervalForge(calendarForges, intervalForge);
                } else if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, Date.class)) {
                    if (calendarForges.isEmpty()) {
                        return new DTLocalDateIntervalForge(intervalForge);
                    }
                    return new DTLocalDateOpsIntervalForge(calendarForges, intervalForge);
                } else if (JavaClassHelper.getBoxedType(inputType) == Long.class) {
                    if (calendarForges.isEmpty()) {
                        return new DTLocalLongIntervalForge(intervalForge);
                    }
                    return new DTLocalLongOpsIntervalForge(calendarForges, intervalForge, timeAbacus);
                } else if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, LocalDateTime.class)) {
                    if (calendarForges.isEmpty()) {
                        return new DTLocalLDTIntervalForge(intervalForge);
                    }
                    return new DTLocalLocalDateTimeOpsIntervalForge(calendarForges, intervalForge);
                } else if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, ZonedDateTime.class)) {
                    if (calendarForges.isEmpty()) {
                        return new DTLocalZDTIntervalForge(intervalForge);
                    }
                    return new DTLocalZonedDateTimeOpsIntervalForge(calendarForges, intervalForge);
                }
            } else { // only calendar op, nothing else
                if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, Calendar.class)) {
                    return new DTLocalCalOpsCalForge(calendarForges);
                } else if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, Date.class)) {
                    return new DTLocalCalOpsDateForge(calendarForges);
                } else if (JavaClassHelper.getBoxedType(inputType) == Long.class) {
                    return new DTLocalCalOpsLongForge(calendarForges, timeAbacus);
                } else if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, LocalDateTime.class)) {
                    return new DTLocalCalOpsLocalDateTimeForge(calendarForges);
                } else if (JavaClassHelper.isSubclassOrImplementsInterface(inputType, ZonedDateTime.class)) {
                    return new DTLocalCalOpsZonedDateTimeForge(calendarForges);
                }
            }
            throw new IllegalArgumentException("Invalid input type '" + inputType + "'");
        }

        EventPropertyGetterSPI getter = ((EventTypeSPI) inputEventType).getGetterSPI(inputEventType.getStartTimestampPropertyName());
        Class getterResultType = inputEventType.getPropertyType(inputEventType.getStartTimestampPropertyName());

        if (reformatForge != null) {
            DTLocalForge inner = getForge(calendarForges, timeAbacus, getterResultType, null, reformatForge, null);
            return new DTLocalBeanReformatForge(getter, getterResultType, inner, reformatForge.getReturnType());
        }
        if (intervalForge == null) {   // only calendar op
            DTLocalForge inner = getForge(calendarForges, timeAbacus, getterResultType, null, null, null);
            return new DTLocalBeanCalOpsForge(getter, getterResultType, inner, EPTypeHelper.getNormalizedClass(returnType));
        }

        // have interval op but no end timestamp
        if (inputEventType.getEndTimestampPropertyName() == null) {
            DTLocalForge inner = getForge(calendarForges, timeAbacus, getterResultType, null, null, intervalForge);
            return new DTLocalBeanIntervalNoEndTSForge(getter, getterResultType, inner, EPTypeHelper.getNormalizedClass(returnType));
        }

        // interval op and have end timestamp
        EventPropertyGetterSPI getterEndTimestamp = ((EventTypeSPI) inputEventType).getGetterSPI(inputEventType.getEndTimestampPropertyName());
        Class getterEndType = inputEventType.getPropertyType(inputEventType.getEndTimestampPropertyName());
        DTLocalForgeIntervalComp inner = (DTLocalForgeIntervalComp) getForge(calendarForges, timeAbacus, getterResultType, null, null, intervalForge);
        return new DTLocalBeanIntervalWithEndForge(getter, getterResultType, getterEndTimestamp, getterEndType, inner);
    }
}
