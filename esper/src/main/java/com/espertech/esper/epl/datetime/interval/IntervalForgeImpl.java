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
package com.espertech.esper.epl.datetime.interval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.core.streamtype.PropertyResolutionDescriptor;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.datetime.eval.DatetimeLongCoercerLocalDateTime;
import com.espertech.esper.epl.datetime.eval.DatetimeLongCoercerZonedDateTime;
import com.espertech.esper.epl.datetime.eval.DatetimeMethodEnum;
import com.espertech.esper.epl.datetime.eval.FilterExprAnalyzerDTIntervalAffector;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.dot.ExprDotNodeFilterAnalyzerInput;
import com.espertech.esper.epl.expression.dot.ExprDotNodeFilterAnalyzerInputProp;
import com.espertech.esper.epl.expression.dot.ExprDotNodeFilterAnalyzerInputStream;
import com.espertech.esper.epl.expression.time.TimeAbacus;
import com.espertech.esper.event.EventPropertyGetterSPI;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.util.JavaClassHelper;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class IntervalForgeImpl implements IntervalForge {

    private ExprForge forgeTimestamp;

    private Integer parameterStreamNum;
    private String parameterPropertyStart;
    private String parameterPropertyEnd;

    private final IntervalOpForge intervalOpForge;

    public IntervalForgeImpl(DatetimeMethodEnum method, String methodNameUse, StreamTypeService streamTypeService, List<ExprNode> expressions, TimeZone timeZone, TimeAbacus timeAbacus)
            throws ExprValidationException {

        ExprForge forgeEndTimestamp = null;
        Class timestampType;

        if (expressions.get(0) instanceof ExprStreamUnderlyingNode) {
            ExprStreamUnderlyingNode und = (ExprStreamUnderlyingNode) expressions.get(0);
            parameterStreamNum = und.getStreamId();
            EventType type = streamTypeService.getEventTypes()[parameterStreamNum];
            parameterPropertyStart = type.getStartTimestampPropertyName();
            if (parameterPropertyStart == null) {
                throw new ExprValidationException("For date-time method '" + methodNameUse + "' the first parameter is event type '" + type.getName() + "', however no timestamp property has been defined for this event type");
            }

            timestampType = type.getPropertyType(parameterPropertyStart);
            EventPropertyGetterSPI getter = ((EventTypeSPI) type).getGetterSPI(parameterPropertyStart);
            Class getterReturnTypeBoxed = JavaClassHelper.getBoxedType(type.getPropertyType(parameterPropertyStart));
            forgeTimestamp = new ExprEvaluatorStreamDTProp(parameterStreamNum, getter, getterReturnTypeBoxed);

            if (type.getEndTimestampPropertyName() != null) {
                parameterPropertyEnd = type.getEndTimestampPropertyName();
                EventPropertyGetterSPI getterEndTimestamp = ((EventTypeSPI) type).getGetterSPI(type.getEndTimestampPropertyName());
                forgeEndTimestamp = new ExprEvaluatorStreamDTProp(parameterStreamNum, getterEndTimestamp, getterReturnTypeBoxed);
            } else {
                parameterPropertyEnd = parameterPropertyStart;
            }
        } else {
            forgeTimestamp = expressions.get(0).getForge();
            timestampType = forgeTimestamp.getEvaluationType();

            String unresolvedPropertyName = null;
            if (expressions.get(0) instanceof ExprIdentNode) {
                ExprIdentNode identNode = (ExprIdentNode) expressions.get(0);
                parameterStreamNum = identNode.getStreamId();
                parameterPropertyStart = identNode.getResolvedPropertyName();
                parameterPropertyEnd = parameterPropertyStart;
                unresolvedPropertyName = identNode.getUnresolvedPropertyName();
            }

            if (!JavaClassHelper.isDatetimeClass(forgeTimestamp.getEvaluationType())) {
                // ident node may represent a fragment
                if (unresolvedPropertyName != null) {
                    Pair<PropertyResolutionDescriptor, String> propertyDesc = ExprIdentNodeUtil.getTypeFromStream(streamTypeService, unresolvedPropertyName, false, true);
                    if (propertyDesc.getFirst().getFragmentEventType() != null) {
                        EventType type = propertyDesc.getFirst().getFragmentEventType().getFragmentType();
                        parameterPropertyStart = type.getStartTimestampPropertyName();
                        if (parameterPropertyStart == null) {
                            throw new ExprValidationException("For date-time method '" + methodNameUse + "' the first parameter is event type '" + type.getName() + "', however no timestamp property has been defined for this event type");
                        }

                        timestampType = type.getPropertyType(parameterPropertyStart);
                        EventPropertyGetterSPI getterFragment = ((EventTypeSPI) streamTypeService.getEventTypes()[parameterStreamNum]).getGetterSPI(unresolvedPropertyName);
                        EventPropertyGetterSPI getterStartTimestamp = ((EventTypeSPI) type).getGetterSPI(parameterPropertyStart);
                        forgeTimestamp = new ExprEvaluatorStreamDTPropFragment(parameterStreamNum, getterFragment, getterStartTimestamp);

                        if (type.getEndTimestampPropertyName() != null) {
                            parameterPropertyEnd = type.getEndTimestampPropertyName();
                            EventPropertyGetterSPI getterEndTimestamp = ((EventTypeSPI) type).getGetterSPI(type.getEndTimestampPropertyName());
                            forgeEndTimestamp = new ExprEvaluatorStreamDTPropFragment(parameterStreamNum, getterFragment, getterEndTimestamp);
                        } else {
                            parameterPropertyEnd = parameterPropertyStart;
                        }
                    }
                } else {
                    throw new ExprValidationException("For date-time method '" + methodNameUse + "' the first parameter expression returns '" + forgeTimestamp.getEvaluationType() + "', however requires a Date, Calendar, Long-type return value or event (with timestamp)");
                }
            }
        }

        IntervalComputerForge intervalComputerForge = IntervalComputerForgeFactory.make(method, expressions, timeAbacus);

        // evaluation without end timestamp
        if (forgeEndTimestamp == null) {
            if (JavaClassHelper.isSubclassOrImplementsInterface(timestampType, Calendar.class)) {
                intervalOpForge = new IntervalOpCalForge(intervalComputerForge);
            } else if (JavaClassHelper.isSubclassOrImplementsInterface(timestampType, Date.class)) {
                intervalOpForge = new IntervalOpDateForge(intervalComputerForge);
            } else if (JavaClassHelper.getBoxedType(timestampType) == Long.class) {
                intervalOpForge = new IntervalOpForgeLong(intervalComputerForge);
            } else if (JavaClassHelper.isSubclassOrImplementsInterface(timestampType, LocalDateTime.class)) {
                intervalOpForge = new IntervalOpLocalDateTimeForge(intervalComputerForge, timeZone);
            } else if (JavaClassHelper.isSubclassOrImplementsInterface(timestampType, ZonedDateTime.class)) {
                intervalOpForge = new IntervalOpZonedDateTimeForge(intervalComputerForge);
            } else {
                throw new IllegalArgumentException("Invalid interval first parameter type '" + timestampType + "'");
            }
        } else {
            if (JavaClassHelper.isSubclassOrImplementsInterface(timestampType, Calendar.class)) {
                intervalOpForge = new IntervalOpCalWithEndForge(intervalComputerForge, forgeEndTimestamp);
            } else if (JavaClassHelper.isSubclassOrImplementsInterface(timestampType, Date.class)) {
                intervalOpForge = new IntervalOpDateWithEndForge(intervalComputerForge, forgeEndTimestamp);
            } else if (JavaClassHelper.getBoxedType(timestampType) == Long.class) {
                intervalOpForge = new IntervalOpLongWithEndForge(intervalComputerForge, forgeEndTimestamp);
            } else if (JavaClassHelper.isSubclassOrImplementsInterface(timestampType, LocalDateTime.class)) {
                intervalOpForge = new IntervalOpLocalDateTimeWithEndForge(intervalComputerForge, forgeEndTimestamp, timeZone);
            } else if (JavaClassHelper.isSubclassOrImplementsInterface(timestampType, ZonedDateTime.class)) {
                intervalOpForge = new IntervalOpZonedDateTimeWithEndForge(intervalComputerForge, forgeEndTimestamp);
            } else {
                throw new IllegalArgumentException("Invalid interval first parameter type '" + timestampType + "'");
            }
        }
    }

    public IntervalOp getOp() {
        return new IntervalForgeOp(forgeTimestamp.getExprEvaluator(), intervalOpForge.makeEval());
    }

    public CodegenExpression codegen(CodegenExpression start, CodegenExpression end, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return IntervalForgeOp.codegen(this, start, end, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public ExprForge getForgeTimestamp() {
        return forgeTimestamp;
    }

    public IntervalOpForge getIntervalOpForge() {
        return intervalOpForge;
    }

    /**
     * Obtain information used by filter analyzer to handle this dot-method invocation as part of query planning/indexing.
     *
     * @param typesPerStream    event types
     * @param currentMethod     current method
     * @param currentParameters current params
     * @param inputDesc         descriptor of what the input to this interval method is
     */
    public FilterExprAnalyzerDTIntervalAffector getFilterDesc(EventType[] typesPerStream, DatetimeMethodEnum currentMethod, List<ExprNode> currentParameters, ExprDotNodeFilterAnalyzerInput inputDesc) {

        // with intervals is not currently query planned
        if (currentParameters.size() > 1) {
            return null;
        }

        // Get input (target)
        int targetStreamNum;
        String targetPropertyStart;
        String targetPropertyEnd;
        if (inputDesc instanceof ExprDotNodeFilterAnalyzerInputStream) {
            ExprDotNodeFilterAnalyzerInputStream targetStream = (ExprDotNodeFilterAnalyzerInputStream) inputDesc;
            targetStreamNum = targetStream.getStreamNum();
            EventType targetType = typesPerStream[targetStreamNum];
            targetPropertyStart = targetType.getStartTimestampPropertyName();
            targetPropertyEnd = targetType.getEndTimestampPropertyName() != null ? targetType.getEndTimestampPropertyName() : targetPropertyStart;
        } else if (inputDesc instanceof ExprDotNodeFilterAnalyzerInputProp) {
            ExprDotNodeFilterAnalyzerInputProp targetStream = (ExprDotNodeFilterAnalyzerInputProp) inputDesc;
            targetStreamNum = targetStream.getStreamNum();
            targetPropertyStart = targetStream.getPropertyName();
            targetPropertyEnd = targetStream.getPropertyName();
        } else {
            return null;
        }

        // check parameter info
        if (parameterPropertyStart == null) {
            return null;
        }

        return new FilterExprAnalyzerDTIntervalAffector(currentMethod, typesPerStream,
                targetStreamNum, targetPropertyStart, targetPropertyEnd,
                parameterStreamNum, parameterPropertyStart, parameterPropertyEnd);
    }

    public static interface IntervalOpForge {
        public IntervalOpEval makeEval();
        public CodegenExpression codegen(CodegenExpression start, CodegenExpression end, CodegenExpression parameter, Class parameterType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope);
    }

    public static interface IntervalOpEval {
        public Object evaluate(long startTs, long endTs, Object parameter, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context);
    }

    public abstract static class IntervalOpForgeBase implements IntervalOpForge {
        protected final IntervalComputerForge intervalComputer;

        public IntervalOpForgeBase(IntervalComputerForge intervalComputer) {
            this.intervalComputer = intervalComputer;
        }
    }

    public abstract static class IntervalOpEvalBase implements IntervalOpEval {
        protected final IntervalComputerEval intervalComputer;

        public IntervalOpEvalBase(IntervalComputerEval intervalComputer) {
            this.intervalComputer = intervalComputer;
        }
    }

    public static class IntervalOpDateForge extends IntervalOpForgeBase {
        public IntervalOpDateForge(IntervalComputerForge intervalComputer) {
            super(intervalComputer);
        }

        public IntervalOpEval makeEval() {
            return new IntervalOpDateEval(intervalComputer.makeComputerEval());
        }

        public CodegenExpression codegen(CodegenExpression start, CodegenExpression end, CodegenExpression parameter, Class parameterType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return IntervalOpDateEval.codegen(this, start, end, parameter, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalOpDateEval extends IntervalOpEvalBase {

        public IntervalOpDateEval(IntervalComputerEval intervalComputer) {
            super(intervalComputer);
        }

        public Object evaluate(long startTs, long endTs, Object parameter, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            long time = ((Date) parameter).getTime();
            return intervalComputer.compute(startTs, endTs, time, time, eventsPerStream, isNewData, context);
        }

        public static CodegenExpression codegen(IntervalOpDateForge forge, CodegenExpression start, CodegenExpression end, CodegenExpression parameter, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, IntervalOpDateEval.class, codegenClassScope).addParam(long.class, "startTs").addParam(long.class, "endTs").addParam(Date.class, "parameter");
            methodNode.getBlock()
                    .declareVar(long.class, "time", exprDotMethod(ref("parameter"), "getTime"))
                    .methodReturn(forge.intervalComputer.codegen(ref("startTs"), ref("endTs"), ref("time"), ref("time"), methodNode, exprSymbol, codegenClassScope));
            return localMethod(methodNode, start, end, parameter);
        }
    }

    public static class IntervalOpForgeLong extends IntervalOpForgeBase {

        public IntervalOpForgeLong(IntervalComputerForge intervalComputer) {
            super(intervalComputer);
        }

        public IntervalOpEval makeEval() {
            return new IntervalOpEvalLong(intervalComputer.makeComputerEval());
        }

        public CodegenExpression codegen(CodegenExpression startTs, CodegenExpression endTs, CodegenExpression parameter, Class parameterType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return IntervalOpEvalLong.codegen(intervalComputer, startTs, endTs, parameter, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalOpEvalLong extends IntervalOpEvalBase {

        public IntervalOpEvalLong(IntervalComputerEval intervalComputer) {
            super(intervalComputer);
        }

        public Object evaluate(long startTs, long endTs, Object parameter, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            long time = (Long) parameter;
            return intervalComputer.compute(startTs, endTs, time, time, eventsPerStream, isNewData, context);
        }

        public static CodegenExpression codegen(IntervalComputerForge intervalComputer, CodegenExpression startTs, CodegenExpression endTs, CodegenExpression parameter, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return intervalComputer.codegen(startTs, endTs, parameter, parameter, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalOpCalForge extends IntervalOpForgeBase {

        public IntervalOpCalForge(IntervalComputerForge intervalComputer) {
            super(intervalComputer);
        }

        public IntervalOpEval makeEval() {
            return new IntervalOpCalEval(intervalComputer.makeComputerEval());
        }

        public CodegenExpression codegen(CodegenExpression start, CodegenExpression end, CodegenExpression parameter, Class parameterType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return IntervalOpCalEval.codegen(this, start, end, parameter, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalOpCalEval extends IntervalOpEvalBase {

        public IntervalOpCalEval(IntervalComputerEval intervalComputer) {
            super(intervalComputer);
        }

        public Object evaluate(long startTs, long endTs, Object parameter, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            long time = ((Calendar) parameter).getTimeInMillis();
            return intervalComputer.compute(startTs, endTs, time, time, eventsPerStream, isNewData, context);
        }

        public static CodegenExpression codegen(IntervalOpCalForge forge, CodegenExpression start, CodegenExpression end, CodegenExpression parameter, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, IntervalOpDateEval.class, codegenClassScope)
                    .addParam(long.class, "startTs").addParam(long.class, "endTs").addParam(Calendar.class, "parameter");

            methodNode.getBlock()
                    .declareVar(long.class, "time", exprDotMethod(ref("parameter"), "getTimeInMillis"))
                    .methodReturn(forge.intervalComputer.codegen(ref("startTs"), ref("endTs"), ref("time"), ref("time"), methodNode, exprSymbol, codegenClassScope));
            return localMethod(methodNode, start, end, parameter);
        }
    }

    public static class IntervalOpLocalDateTimeForge extends IntervalOpForgeBase {

        private final TimeZone timeZone;

        public IntervalOpLocalDateTimeForge(IntervalComputerForge intervalComputer, TimeZone timeZone) {
            super(intervalComputer);
            this.timeZone = timeZone;
        }

        public IntervalOpEval makeEval() {
            return new IntervalOpLocalDateTimeEval(intervalComputer.makeComputerEval(), timeZone);
        }

        public CodegenExpression codegen(CodegenExpression start, CodegenExpression end, CodegenExpression parameter, Class parameterType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return IntervalOpLocalDateTimeEval.codegen(this, start, end, parameter, parameterType, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalOpLocalDateTimeEval extends IntervalOpEvalBase {

        private final TimeZone timeZone;

        public IntervalOpLocalDateTimeEval(IntervalComputerEval intervalComputer, TimeZone timeZone) {
            super(intervalComputer);
            this.timeZone = timeZone;
        }

        public Object evaluate(long startTs, long endTs, Object parameter, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            long time = DatetimeLongCoercerLocalDateTime.coerceLDTToMilliWTimezone((LocalDateTime) parameter, timeZone);
            return intervalComputer.compute(startTs, endTs, time, time, eventsPerStream, isNewData, context);
        }

        public static CodegenExpression codegen(IntervalOpLocalDateTimeForge forge, CodegenExpression start, CodegenExpression end, CodegenExpression parameter, Class parameterType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMember tz = codegenClassScope.makeAddMember(TimeZone.class, forge.timeZone);
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, IntervalOpLocalDateTimeEval.class, codegenClassScope).addParam(long.class, "startTs").addParam(long.class, "endTs").addParam(LocalDateTime.class, "parameter");

            methodNode.getBlock()
                    .declareVar(long.class, "time", staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", ref("parameter"), member(tz.getMemberId())))
                    .methodReturn(forge.intervalComputer.codegen(ref("startTs"), ref("endTs"), ref("time"), ref("time"), methodNode, exprSymbol, codegenClassScope));
            return localMethod(methodNode, start, end, parameter);
        }
    }

    public static class IntervalOpZonedDateTimeForge extends IntervalOpForgeBase {

        public IntervalOpZonedDateTimeForge(IntervalComputerForge intervalComputer) {
            super(intervalComputer);
        }

        public IntervalOpEval makeEval() {
            return new IntervalOpZonedDateTimeEval(intervalComputer.makeComputerEval());
        }

        public CodegenExpression codegen(CodegenExpression start, CodegenExpression end, CodegenExpression parameter, Class parameterType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return IntervalOpZonedDateTimeEval.codegen(this, start, end, parameter, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalOpZonedDateTimeEval extends IntervalOpEvalBase {

        public IntervalOpZonedDateTimeEval(IntervalComputerEval intervalComputer) {
            super(intervalComputer);
        }

        public Object evaluate(long startTs, long endTs, Object parameter, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            long time = DatetimeLongCoercerZonedDateTime.coerceZDTToMillis((ZonedDateTime) parameter);
            return intervalComputer.compute(startTs, endTs, time, time, eventsPerStream, isNewData, context);
        }

        public static CodegenExpression codegen(IntervalOpZonedDateTimeForge forge, CodegenExpression start, CodegenExpression end, CodegenExpression parameter, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, IntervalOpZonedDateTimeEval.class, codegenClassScope).addParam(long.class, "startTs").addParam(long.class, "endTs").addParam(ZonedDateTime.class, "parameter");

            methodNode.getBlock()
                    .declareVar(long.class, "time", staticMethod(DatetimeLongCoercerZonedDateTime.class, "coerceZDTToMillis", ref("parameter")))
                    .methodReturn(forge.intervalComputer.codegen(ref("startTs"), ref("endTs"), ref("time"), ref("time"), methodNode, exprSymbol, codegenClassScope));
            return localMethod(methodNode, start, end, parameter);
        }
    }

    public abstract static class IntervalOpForgeDateWithEndBase implements IntervalOpForge {
        protected final IntervalComputerForge intervalComputer;
        protected final ExprForge forgeEndTimestamp;

        public IntervalOpForgeDateWithEndBase(IntervalComputerForge intervalComputer, ExprForge forgeEndTimestamp) {
            this.intervalComputer = intervalComputer;
            this.forgeEndTimestamp = forgeEndTimestamp;
        }

        protected abstract CodegenExpression codegenEvaluate(CodegenExpressionRef startTs, CodegenExpressionRef endTs, CodegenExpressionRef paramStartTs, CodegenExpressionRef paramEndTs, CodegenMethodNode parentNode, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope);

        public CodegenExpression codegen(CodegenExpression start, CodegenExpression end, CodegenExpression parameter, Class parameterType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, IntervalOpForgeDateWithEndBase.class, codegenClassScope).addParam(long.class, "startTs").addParam(long.class, "endTs").addParam(parameterType, "paramStartTs");

            Class evaluationType = forgeEndTimestamp.getEvaluationType();
            methodNode.getBlock().declareVar(evaluationType, "paramEndTs", forgeEndTimestamp.evaluateCodegen(evaluationType, methodNode, exprSymbol, codegenClassScope));
            if (!evaluationType.isPrimitive()) {
                methodNode.getBlock().ifRefNullReturnNull("paramEndTs");
            }
            CodegenExpression expression = codegenEvaluate(ref("startTs"), ref("endTs"), ref("paramStartTs"), ref("paramEndTs"), methodNode, exprSymbol, codegenClassScope);
            methodNode.getBlock().methodReturn(expression);
            return localMethod(methodNode, start, end, parameter);
        }
    }

    public abstract static class IntervalOpEvalDateWithEndBase implements IntervalOpEval {
        protected final IntervalComputerEval intervalComputer;
        private final ExprEvaluator evaluatorEndTimestamp;

        protected IntervalOpEvalDateWithEndBase(IntervalComputerEval intervalComputer, ExprEvaluator evaluatorEndTimestamp) {
            this.intervalComputer = intervalComputer;
            this.evaluatorEndTimestamp = evaluatorEndTimestamp;
        }

        public abstract Object evaluate(long startTs, long endTs, Object parameterStartTs, Object parameterEndTs, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context);

        public Object evaluate(long startTs, long endTs, Object parameterStartTs, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            Object paramEndTs = evaluatorEndTimestamp.evaluate(eventsPerStream, isNewData, context);
            if (paramEndTs == null) {
                return null;
            }
            return evaluate(startTs, endTs, parameterStartTs, paramEndTs, eventsPerStream, isNewData, context);
        }
    }

    public static class IntervalOpDateWithEndForge extends IntervalOpForgeDateWithEndBase {

        public IntervalOpDateWithEndForge(IntervalComputerForge intervalComputer, ExprForge evaluatorEndTimestamp) {
            super(intervalComputer, evaluatorEndTimestamp);
        }

        public IntervalOpEval makeEval() {
            return new IntervalOpDateWithEndEval(intervalComputer.makeComputerEval(), forgeEndTimestamp.getExprEvaluator());
        }

        protected CodegenExpression codegenEvaluate(CodegenExpressionRef startTs, CodegenExpressionRef endTs, CodegenExpressionRef paramStartTs, CodegenExpressionRef paramEndTs, CodegenMethodNode parentNode, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return intervalComputer.codegen(startTs, endTs, exprDotMethod(paramStartTs, "getTime"), exprDotMethod(paramEndTs, "getTime"), parentNode, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalOpDateWithEndEval extends IntervalOpEvalDateWithEndBase {

        public IntervalOpDateWithEndEval(IntervalComputerEval intervalComputer, ExprEvaluator evaluatorEndTimestamp) {
            super(intervalComputer, evaluatorEndTimestamp);
        }

        public Object evaluate(long startTs, long endTs, Object parameterStartTs, Object parameterEndTs, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            return intervalComputer.compute(startTs, endTs, ((Date) parameterStartTs).getTime(), ((Date) parameterEndTs).getTime(), eventsPerStream, isNewData, context);
        }
    }

    public static class IntervalOpLongWithEndForge extends IntervalOpForgeDateWithEndBase {

        public IntervalOpLongWithEndForge(IntervalComputerForge intervalComputer, ExprForge evaluatorEndTimestamp) {
            super(intervalComputer, evaluatorEndTimestamp);
        }

        public IntervalOpEval makeEval() {
            return new IntervalOpLongWithEndEval(intervalComputer.makeComputerEval(), forgeEndTimestamp.getExprEvaluator());
        }

        protected CodegenExpression codegenEvaluate(CodegenExpressionRef startTs, CodegenExpressionRef endTs, CodegenExpressionRef paramStartTs, CodegenExpressionRef paramEndTs, CodegenMethodNode parentNode, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return intervalComputer.codegen(startTs, endTs, paramStartTs, paramEndTs, parentNode, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalOpLongWithEndEval extends IntervalOpEvalDateWithEndBase {

        public IntervalOpLongWithEndEval(IntervalComputerEval intervalComputer, ExprEvaluator evaluatorEndTimestamp) {
            super(intervalComputer, evaluatorEndTimestamp);
        }

        public Object evaluate(long startTs, long endTs, Object parameterStartTs, Object parameterEndTs, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            return intervalComputer.compute(startTs, endTs, (Long) parameterStartTs, (Long) parameterEndTs, eventsPerStream, isNewData, context);
        }
    }

    public static class IntervalOpCalWithEndForge extends IntervalOpForgeDateWithEndBase {

        public IntervalOpCalWithEndForge(IntervalComputerForge intervalComputer, ExprForge forgeEndTimestamp) {
            super(intervalComputer, forgeEndTimestamp);
        }

        public IntervalOpEval makeEval() {
            return new IntervalOpCalWithEndEval(intervalComputer.makeComputerEval(), forgeEndTimestamp.getExprEvaluator());
        }

        protected CodegenExpression codegenEvaluate(CodegenExpressionRef startTs, CodegenExpressionRef endTs, CodegenExpressionRef paramStartTs, CodegenExpressionRef paramEndTs, CodegenMethodNode parentNode, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return intervalComputer.codegen(startTs, endTs, exprDotMethod(paramStartTs, "getTimeInMillis"), exprDotMethod(paramEndTs, "getTimeInMillis"), parentNode, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalOpCalWithEndEval extends IntervalOpEvalDateWithEndBase {

        public IntervalOpCalWithEndEval(IntervalComputerEval intervalComputer, ExprEvaluator evaluatorEndTimestamp) {
            super(intervalComputer, evaluatorEndTimestamp);
        }

        public Object evaluate(long startTs, long endTs, Object parameterStartTs, Object parameterEndTs, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            return intervalComputer.compute(startTs, endTs, ((Calendar) parameterStartTs).getTimeInMillis(), ((Calendar) parameterEndTs).getTimeInMillis(), eventsPerStream, isNewData, context);
        }
    }

    public static class IntervalOpLocalDateTimeWithEndForge extends IntervalOpForgeDateWithEndBase {

        private final TimeZone timeZone;

        public IntervalOpLocalDateTimeWithEndForge(IntervalComputerForge intervalComputer, ExprForge evaluatorEndTimestamp, TimeZone timeZone) {
            super(intervalComputer, evaluatorEndTimestamp);
            this.timeZone = timeZone;
        }

        public IntervalOpEval makeEval() {
            return new IntervalOpLocalDateTimeWithEndEval(intervalComputer.makeComputerEval(), forgeEndTimestamp.getExprEvaluator(), timeZone);
        }

        protected CodegenExpression codegenEvaluate(CodegenExpressionRef startTs, CodegenExpressionRef endTs, CodegenExpressionRef paramStartTs, CodegenExpressionRef paramEndTs, CodegenMethodNode parentNode, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMember tz = codegenClassScope.makeAddMember(TimeZone.class, timeZone);
            return intervalComputer.codegen(startTs, endTs,
                            staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", paramStartTs, member(tz.getMemberId())),
                            staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", paramEndTs, member(tz.getMemberId())),
                    parentNode, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalOpLocalDateTimeWithEndEval extends IntervalOpEvalDateWithEndBase {

        private final TimeZone timeZone;

        public IntervalOpLocalDateTimeWithEndEval(IntervalComputerEval intervalComputer, ExprEvaluator evaluatorEndTimestamp, TimeZone timeZone) {
            super(intervalComputer, evaluatorEndTimestamp);
            this.timeZone = timeZone;
        }

        public Object evaluate(long startTs, long endTs, Object parameterStartTs, Object parameterEndTs, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            return intervalComputer.compute(startTs, endTs, DatetimeLongCoercerLocalDateTime.coerceLDTToMilliWTimezone((LocalDateTime) parameterStartTs, timeZone), DatetimeLongCoercerLocalDateTime.coerceLDTToMilliWTimezone((LocalDateTime) parameterEndTs, timeZone), eventsPerStream, isNewData, context);
        }
    }

    public static class IntervalOpZonedDateTimeWithEndForge extends IntervalOpForgeDateWithEndBase {

        public IntervalOpZonedDateTimeWithEndForge(IntervalComputerForge intervalComputer, ExprForge evaluatorEndTimestamp) {
            super(intervalComputer, evaluatorEndTimestamp);
        }

        public IntervalOpEval makeEval() {
            return new IntervalOpZonedDateTimeWithEndEval(intervalComputer.makeComputerEval(), forgeEndTimestamp.getExprEvaluator());
        }

        protected CodegenExpression codegenEvaluate(CodegenExpressionRef startTs, CodegenExpressionRef endTs, CodegenExpressionRef paramStartTs, CodegenExpressionRef paramEndTs, CodegenMethodNode parentNode, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return intervalComputer.codegen(startTs, endTs,
                    staticMethod(DatetimeLongCoercerZonedDateTime.class, "coerceZDTToMillis", paramStartTs),
                    staticMethod(DatetimeLongCoercerZonedDateTime.class, "coerceZDTToMillis", paramEndTs), parentNode, exprSymbol, codegenClassScope);
        }
    }

    public static class IntervalOpZonedDateTimeWithEndEval extends IntervalOpEvalDateWithEndBase {

        public IntervalOpZonedDateTimeWithEndEval(IntervalComputerEval intervalComputer, ExprEvaluator evaluatorEndTimestamp) {
            super(intervalComputer, evaluatorEndTimestamp);
        }

        public Object evaluate(long startTs, long endTs, Object parameterStartTs, Object parameterEndTs, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            return intervalComputer.compute(startTs, endTs, DatetimeLongCoercerZonedDateTime.coerceZDTToMillis((ZonedDateTime) parameterStartTs), DatetimeLongCoercerZonedDateTime.coerceZDTToMillis((ZonedDateTime) parameterEndTs), eventsPerStream, isNewData, context);
        }
    }
}
