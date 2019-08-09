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
package com.espertech.esper.common.internal.epl.datetime.reformatop;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeLongCoercerFactory;
import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeLongCoercerLocalDateTime;
import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeLongCoercerZonedDateTime;
import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeMethodDesc;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeFilterAnalyzerInput;
import com.espertech.esper.common.internal.epl.join.analyze.FilterExprAnalyzerAffector;
import com.espertech.esper.common.internal.settings.RuntimeSettingsTimeZoneField;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LE;

public class ReformatBetweenConstantParamsForge implements ReformatForge, ReformatOp {

    private long first;
    private long second;

    public ReformatBetweenConstantParamsForge(List<ExprNode> parameters) throws ExprValidationException {

        long paramFirst = getLongValue(parameters.get(0));
        long paramSecond = getLongValue(parameters.get(1));

        if (paramFirst > paramSecond) {
            this.second = paramFirst;
            this.first = paramSecond;
        } else {
            this.first = paramFirst;
            this.second = paramSecond;
        }
        if (parameters.size() > 2) {
            if (!getBooleanValue(parameters.get(2))) {
                first = first + 1;
            }
            if (!getBooleanValue(parameters.get(3))) {
                second = second - 1;
            }
        }
    }

    public ReformatOp getOp() {
        return this;
    }

    private long getLongValue(ExprNode exprNode)
            throws ExprValidationException {
        Object value = exprNode.getForge().getExprEvaluator().evaluate(null, true, null);
        if (value == null) {
            throw new ExprValidationException("Date-time method 'between' requires non-null parameter values");
        }
        return DatetimeLongCoercerFactory.getCoercer(value.getClass()).coerce(value);
    }

    private boolean getBooleanValue(ExprNode exprNode)
            throws ExprValidationException {
        Object value = exprNode.getForge().getExprEvaluator().evaluate(null, true, null);
        if (value == null) {
            throw new ExprValidationException("Date-time method 'between' requires non-null parameter values");
        }
        return (Boolean) value;
    }

    public Object evaluate(Long ts, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (ts == null) {
            return null;
        }
        return evaluateInternal(ts);
    }

    public Object evaluate(Date d, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (d == null) {
            return null;
        }
        return evaluateInternal(d.getTime());
    }

    public CodegenExpression codegenDate(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return codegenLong(exprDotMethod(inner, "getTime"), codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public Object evaluate(Calendar cal, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (cal == null) {
            return null;
        }
        return evaluateInternal(cal.getTimeInMillis());
    }

    public CodegenExpression codegenCal(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return codegenLong(exprDotMethod(inner, "getTimeInMillis"), codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public Object evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (ldt == null) {
            return null;
        }
        return evaluateInternal(DatetimeLongCoercerLocalDateTime.coerceLDTToMilliWTimezone(ldt, TimeZone.getDefault()));
    }

    public CodegenExpression codegenLDT(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression tz = codegenClassScope.addOrGetFieldSharable(RuntimeSettingsTimeZoneField.INSTANCE);
        return codegenLong(staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", inner, tz), codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public Object evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
        if (zdt == null) {
            return null;
        }
        return evaluateInternal(DatetimeLongCoercerZonedDateTime.coerceZDTToMillis(zdt));
    }

    public CodegenExpression codegenZDT(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return codegenLong(staticMethod(DatetimeLongCoercerZonedDateTime.class, "coerceZDTToMillis", inner), codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public Object evaluateInternal(long ts) {
        return first <= ts && ts <= second;
    }

    public CodegenExpression codegenLong(CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return and(relational(constant(first), LE, inner), relational(inner, LE, constant(second)));
    }

    public Class getReturnType() {
        return Boolean.class;
    }

    public FilterExprAnalyzerAffector getFilterDesc(EventType[] typesPerStream, DatetimeMethodDesc currentMethod, List<ExprNode> currentParameters, ExprDotNodeFilterAnalyzerInput inputDesc) {
        return null;
    }
}
