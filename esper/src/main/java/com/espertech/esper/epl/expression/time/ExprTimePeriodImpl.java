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
package com.espertech.esper.epl.expression.time;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.util.TimePeriod;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.TimeZone;

/**
 * Expression representing a time period.
 * <p>
 * Child nodes to this expression carry the actual parts and must return a numeric value.
 */
public class ExprTimePeriodImpl extends ExprNodeBase implements ExprTimePeriod {
    private static final long serialVersionUID = -7229827032500659319L;

    private final TimeZone timeZone;
    private final boolean hasYear;
    private final boolean hasMonth;
    private final boolean hasWeek;
    private final boolean hasDay;
    private final boolean hasHour;
    private final boolean hasMinute;
    private final boolean hasSecond;
    private final boolean hasMillisecond;
    private final boolean hasMicrosecond;
    private final TimeAbacus timeAbacus;

    private transient ExprTimePeriodForge forge;

    public ExprTimePeriodImpl(TimeZone timeZone, boolean hasYear, boolean hasMonth, boolean hasWeek, boolean hasDay, boolean hasHour, boolean hasMinute, boolean hasSecond, boolean hasMillisecond, boolean hasMicrosecond, TimeAbacus timeAbacus) {
        this.timeZone = timeZone;
        this.hasYear = hasYear;
        this.hasMonth = hasMonth;
        this.hasWeek = hasWeek;
        this.hasDay = hasDay;
        this.hasHour = hasHour;
        this.hasMinute = hasMinute;
        this.hasSecond = hasSecond;
        this.hasMillisecond = hasMillisecond;
        this.hasMicrosecond = hasMicrosecond;
        this.timeAbacus = timeAbacus;
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return forge;
    }

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public ExprTimePeriodEvalDeltaConst constEvaluator(ExprEvaluatorContext context) {
        checkValidated(forge);
        return forge.constEvaluator(context);
    }

    public ExprTimePeriodEvalDeltaNonConst nonconstEvaluator() {
        checkValidated(forge);
        return forge.nonconstEvaluator();
    }

    public CodegenExpression evaluateGetTimePeriodCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        checkValidated(forge);
        return forge.evaluateGetTimePeriodCodegen(codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateAsSecondsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        checkValidated(forge);
        return forge.evaluateAsSecondsCodegen(codegenMethodScope, exprSymbol, codegenClassScope);
    }

    /**
     * Indicator whether the time period has a variable in any of the child expressions.
     *
     * @return true for variable present, false for not present
     */
    public boolean hasVariable() {
        checkValidated(forge);
        return forge.isHasVariable();
    }

    public TimePeriod evaluateGetTimePeriod(EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
        checkValidated(forge);
        return forge.evaluateGetTimePeriod(eventsPerStream, newData, context);
    }

    public double evaluateAsSeconds(EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
        checkValidated(forge);
        return forge.evaluateAsSeconds(eventsPerStream, newData, context);
    }

    public TimeAbacus getTimeAbacus() {
        return timeAbacus;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    /**
     * Indicator whether the time period has a day part child expression.
     *
     * @return true for part present, false for not present
     */
    public boolean isHasDay() {
        return hasDay;
    }

    /**
     * Indicator whether the time period has a hour part child expression.
     *
     * @return true for part present, false for not present
     */
    public boolean isHasHour() {
        return hasHour;
    }

    /**
     * Indicator whether the time period has a minute part child expression.
     *
     * @return true for part present, false for not present
     */
    public boolean isHasMinute() {
        return hasMinute;
    }

    /**
     * Indicator whether the time period has a second part child expression.
     *
     * @return true for part present, false for not present
     */
    public boolean isHasSecond() {
        return hasSecond;
    }

    /**
     * Indicator whether the time period has a millisecond part child expression.
     *
     * @return true for part present, false for not present
     */
    public boolean isHasMillisecond() {
        return hasMillisecond;
    }

    public boolean isHasMicrosecond() {
        return hasMicrosecond;
    }

    /**
     * Indicator whether the time period has a year part child expression.
     *
     * @return true for part present, false for not present
     */
    public boolean isHasYear() {
        return hasYear;
    }

    /**
     * Indicator whether the time period has a month part child expression.
     *
     * @return true for part present, false for not present
     */
    public boolean isHasMonth() {
        return hasMonth;
    }

    /**
     * Indicator whether the time period has a week part child expression.
     *
     * @return true for part present, false for not present
     */
    public boolean isHasWeek() {
        return hasWeek;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        boolean hasVariables = false;
        for (ExprNode childNode : this.getChildNodes()) {
            hasVariables |= validate(childNode);
        }

        ArrayDeque<ExprTimePeriodAdder.TimePeriodAdder> list = new ArrayDeque<ExprTimePeriodAdder.TimePeriodAdder>();
        if (hasYear) {
            list.add(new ExprTimePeriodAdder.TimePeriodAdderYear());
        }
        if (hasMonth) {
            list.add(new ExprTimePeriodAdder.TimePeriodAdderMonth());
        }
        if (hasWeek) {
            list.add(new ExprTimePeriodAdder.TimePeriodAdderWeek());
        }
        if (hasDay) {
            list.add(new ExprTimePeriodAdder.TimePeriodAdderDay());
        }
        if (hasHour) {
            list.add(new ExprTimePeriodAdder.TimePeriodAdderHour());
        }
        if (hasMinute) {
            list.add(new ExprTimePeriodAdder.TimePeriodAdderMinute());
        }
        if (hasSecond) {
            list.add(new ExprTimePeriodAdder.TimePeriodAdderSecond());
        }
        if (hasMillisecond) {
            list.add(new ExprTimePeriodAdder.TimePeriodAdderMSec());
        }
        if (hasMicrosecond) {
            list.add(new ExprTimePeriodAdder.TimePeriodAdderUSec());
        }
        ExprTimePeriodAdder.TimePeriodAdder[] adders = list.toArray(new ExprTimePeriodAdder.TimePeriodAdder[list.size()]);
        forge = new ExprTimePeriodForge(this, hasVariables, adders);
        return null;
    }

    public boolean isConstantResult() {
        for (ExprNode child : getChildNodes()) {
            if (!child.isConstantResult()) {
                return false;
            }
        }
        return true;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        int exprCtr = 0;
        String delimiter = "";
        if (hasYear) {
            getChildNodes()[exprCtr++].toEPL(writer, getPrecedence());
            writer.append(" years");
            delimiter = " ";
        }
        if (hasMonth) {
            writer.append(delimiter);
            getChildNodes()[exprCtr++].toEPL(writer, getPrecedence());
            writer.append(" months");
            delimiter = " ";
        }
        if (hasWeek) {
            writer.append(delimiter);
            getChildNodes()[exprCtr++].toEPL(writer, getPrecedence());
            writer.append(" weeks");
            delimiter = " ";
        }
        if (hasDay) {
            writer.append(delimiter);
            getChildNodes()[exprCtr++].toEPL(writer, getPrecedence());
            writer.append(" days");
            delimiter = " ";
        }
        if (hasHour) {
            writer.append(delimiter);
            getChildNodes()[exprCtr++].toEPL(writer, getPrecedence());
            writer.append(" hours");
            delimiter = " ";
        }
        if (hasMinute) {
            writer.append(delimiter);
            getChildNodes()[exprCtr++].toEPL(writer, getPrecedence());
            writer.append(" minutes");
            delimiter = " ";
        }
        if (hasSecond) {
            writer.append(delimiter);
            getChildNodes()[exprCtr++].toEPL(writer, getPrecedence());
            writer.append(" seconds");
            delimiter = " ";
        }
        if (hasMillisecond) {
            writer.append(delimiter);
            getChildNodes()[exprCtr++].toEPL(writer, getPrecedence());
            writer.append(" milliseconds");
            delimiter = " ";
        }
        if (hasMicrosecond) {
            writer.append(delimiter);
            getChildNodes()[exprCtr].toEPL(writer, getPrecedence());
            writer.append(" microseconds");
        }
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprTimePeriodImpl)) {
            return false;
        }
        ExprTimePeriodImpl other = (ExprTimePeriodImpl) node;

        if (hasYear != other.hasYear) {
            return false;
        }
        if (hasMonth != other.hasMonth) {
            return false;
        }
        if (hasWeek != other.hasWeek) {
            return false;
        }
        if (hasDay != other.hasDay) {
            return false;
        }
        if (hasHour != other.hasHour) {
            return false;
        }
        if (hasMinute != other.hasMinute) {
            return false;
        }
        if (hasSecond != other.hasSecond) {
            return false;
        }
        if (hasMillisecond != other.hasMillisecond) {
            return false;
        }
        return hasMicrosecond == other.hasMicrosecond;
    }

    private boolean validate(ExprNode expression) throws ExprValidationException {
        if (expression == null) {
            return false;
        }
        Class returnType = expression.getForge().getEvaluationType();
        if (!JavaClassHelper.isNumeric(returnType)) {
            throw new ExprValidationException("Time period expression requires a numeric parameter type");
        }
        if ((hasMonth || hasYear) && (JavaClassHelper.getBoxedType(returnType) != Integer.class)) {
            throw new ExprValidationException("Time period expressions with month or year component require integer values, received a " + returnType.getSimpleName() + " value");
        }
        return expression instanceof ExprVariableNode;
    }
}
