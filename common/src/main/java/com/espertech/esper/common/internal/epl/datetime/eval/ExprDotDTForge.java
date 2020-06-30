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
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
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
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEval;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEvalVisitor;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotForge;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.rettype.EPChainableType;
import com.espertech.esper.common.internal.rettype.EPChainableTypeClass;
import com.espertech.esper.common.internal.rettype.EPChainableTypeHelper;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;
import static com.espertech.esper.common.internal.rettype.EPChainableTypeHelper.singleValueNonNull;

public class ExprDotDTForge implements ExprDotForge {
    private final EPChainableTypeClass returnType;
    private final DTLocalForge forge;

    ExprDotDTForge(List<CalendarForge> calendarForges, TimeAbacus timeAbacus, ReformatForge reformatForge, IntervalForge intervalForge, EPTypeClass inputType, EventType inputEventType)
            throws ExprValidationException {
        if (intervalForge != null) {
            returnType = singleValueNonNull(EPTypePremade.BOOLEANBOXED.getEPType());
        } else if (reformatForge != null) {
            returnType = singleValueNonNull(reformatForge.getReturnType());
        } else {  // only calendar op
            if (inputEventType != null) {
                returnType = singleValueNonNull(inputEventType.getPropertyEPType(inputEventType.getStartTimestampPropertyName()));
            } else {
                returnType = singleValueNonNull(inputType == EPTypePremade.SQLDATE.getEPType() ? EPTypePremade.DATE.getEPType() : inputType);
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

    public CodegenExpression codegen(CodegenExpression inner, EPTypeClass innerType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod methodNode = parent.makeChild(returnType.getType(), ExprDotDTForge.class, classScope).addParam(innerType, "target");

        CodegenBlock block = methodNode.getBlock();
        if (!innerType.getType().isPrimitive()) {
            block.ifRefNullReturnNull("target");
        }
        block.methodReturn(forge.codegen(ref("target"), innerType, methodNode, symbols, classScope));
        return localMethod(methodNode, inner);
    }

    public EPChainableType getTypeInfo() {
        return returnType;
    }

    public void visit(ExprDotEvalVisitor visitor) {
        visitor.visitDateTime();
    }

    public DTLocalForge getForge(List<CalendarForge> calendarForges, TimeAbacus timeAbacus, EPTypeClass inputTypeClass, EventType inputEventType, ReformatForge reformatForge, IntervalForge intervalForge)
            throws ExprValidationException {
        if (inputEventType == null) {
            Class inputType = inputTypeClass.getType();
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
            throw new ExprValidationException("Invalid input type '" + inputType + "'");
        }

        String propertyNameStart = inputEventType.getStartTimestampPropertyName();
        EventPropertyGetterSPI getter = ((EventTypeSPI) inputEventType).getGetterSPI(propertyNameStart);
        EPType getterResultEPType = inputEventType.getPropertyEPType(propertyNameStart);
        checkNotNull(getterResultEPType, propertyNameStart);
        EPTypeClass getterResultType = (EPTypeClass) getterResultEPType;

        if (reformatForge != null) {
            DTLocalForge inner = getForge(calendarForges, timeAbacus, getterResultType, null, reformatForge, null);
            return new DTLocalBeanReformatForge(getter, getterResultType, inner, reformatForge.getReturnType());
        }
        if (intervalForge == null) {   // only calendar op
            DTLocalForge inner = getForge(calendarForges, timeAbacus, getterResultType, null, null, null);
            return new DTLocalBeanCalOpsForge(getter, getterResultType, inner, (EPTypeClass) EPChainableTypeHelper.getNormalizedEPType(returnType));
        }

        // have interval op but no end timestamp
        if (inputEventType.getEndTimestampPropertyName() == null) {
            DTLocalForge inner = getForge(calendarForges, timeAbacus, getterResultType, null, null, intervalForge);
            return new DTLocalBeanIntervalNoEndTSForge(getter, getterResultType, inner, (EPTypeClass) EPChainableTypeHelper.getNormalizedEPType(returnType));
        }

        // interval op and have end timestamp
        String propertyNameEnd = inputEventType.getEndTimestampPropertyName();
        EventPropertyGetterSPI getterEndTimestamp = ((EventTypeSPI) inputEventType).getGetterSPI(propertyNameEnd);
        EPType getterEndType = inputEventType.getPropertyEPType(propertyNameEnd);
        checkNotNull(getterEndType, propertyNameEnd);
        DTLocalForgeIntervalComp inner = (DTLocalForgeIntervalComp) getForge(calendarForges, timeAbacus, getterResultType, null, null, intervalForge);
        return new DTLocalBeanIntervalWithEndForge(getter, getterResultType, getterEndTimestamp, (EPTypeClass) getterEndType, inner);
    }

    private void checkNotNull(EPType getterResultEPType, String propertyName) throws ExprValidationException {
        if (getterResultEPType == null || getterResultEPType == EPTypeNull.INSTANCE) {
            throw new ExprValidationException("Invalid null-type input for property '" + propertyName + "'");
        }
    }
}
