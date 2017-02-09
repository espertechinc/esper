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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.CoercionException;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.SimpleNumberCoercer;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.io.StringWriter;
import java.util.Map;

/**
 * Represents an equals (=) comparator in a filter expressiun tree.
 */
public class ExprEqualsNodeImpl extends ExprNodeBase implements ExprEqualsNode {
    private final boolean isNotEquals;
    private final boolean isIs;
    private transient ExprEvaluator evaluator;

    private static final long serialVersionUID = 5504809379222369952L;

    /**
     * Ctor.
     *
     * @param isNotEquals - true if this is a (!=) not equals rather then equals, false if its a '=' equals
     * @param isIs        - true when "is" or "is not" (instead of = or &lt;&gt;)
     */
    public ExprEqualsNodeImpl(boolean isNotEquals, boolean isIs) {
        this.isNotEquals = isNotEquals;
        this.isIs = isIs;
    }

    public ExprEvaluator getExprEvaluator() {
        return evaluator;
    }

    public boolean isNotEquals() {
        return isNotEquals;
    }

    public boolean isIs() {
        return isIs;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        // Must have 2 child nodes
        if (this.getChildNodes().length != 2) {
            throw new ExprValidationException("Invalid use of equals, expecting left-hand side and right-hand side but received " + this.getChildNodes().length + " expressions");
        }
        ExprEvaluator[] evaluators = ExprNodeUtility.getEvaluators(this.getChildNodes());

        // Must be the same boxed type returned by expressions under this
        Class typeOne = JavaClassHelper.getBoxedType(evaluators[0].getType());
        Class typeTwo = JavaClassHelper.getBoxedType(evaluators[1].getType());

        // Null constants can be compared for any type
        if ((typeOne == null) || (typeTwo == null)) {
            evaluator = getEvaluator(evaluators[0], evaluators[1]);
            return null;
        }

        if (typeOne.equals(typeTwo) || typeOne.isAssignableFrom(typeTwo)) {
            evaluator = getEvaluator(evaluators[0], evaluators[1]);
            return null;
        }

        // Get the common type such as Bool, String or Double and Long
        Class coercionType;
        try {
            coercionType = JavaClassHelper.getCompareToCoercionType(typeOne, typeTwo);
        } catch (CoercionException ex) {
            throw new ExprValidationException("Implicit conversion from datatype '" +
                    typeTwo.getSimpleName() +
                    "' to '" +
                    typeOne.getSimpleName() +
                    "' is not allowed");
        }

        // Check if we need to coerce
        if ((coercionType == JavaClassHelper.getBoxedType(typeOne)) &&
                (coercionType == JavaClassHelper.getBoxedType(typeTwo))) {
            evaluator = getEvaluator(evaluators[0], evaluators[1]);
        } else {
            if (!JavaClassHelper.isNumeric(coercionType)) {
                throw new ExprValidationException("Cannot convert datatype '" + coercionType.getName() + "' to a numeric value");
            }
            SimpleNumberCoercer numberCoercerLHS = SimpleNumberCoercerFactory.getCoercer(typeOne, coercionType);
            SimpleNumberCoercer numberCoercerRHS = SimpleNumberCoercerFactory.getCoercer(typeTwo, coercionType);
            evaluator = new ExprEqualsEvaluatorCoercing(this, evaluators[0], evaluators[1], numberCoercerLHS, numberCoercerRHS);
        }
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Map<String, Object> getEventType() {
        return null;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        this.getChildNodes()[0].toEPL(writer, getPrecedence());
        if (isIs) {
            writer.append(" is ");
            if (isNotEquals) {
                writer.append("not ");
            }
        } else {
            if (!isNotEquals) {
                writer.append("=");
            } else {
                writer.append("!=");
            }
        }
        this.getChildNodes()[1].toEPL(writer, getPrecedence());
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.EQUALS;
    }

    public boolean equalsNode(ExprNode node) {
        if (!(node instanceof ExprEqualsNodeImpl)) {
            return false;
        }

        ExprEqualsNodeImpl other = (ExprEqualsNodeImpl) node;
        return other.isNotEquals == this.isNotEquals;
    }

    private ExprEvaluator getEvaluator(ExprEvaluator lhs, ExprEvaluator rhs) {
        if (isIs) {
            return new ExprEqualsEvaluatorIs(this, lhs, rhs);
        } else {
            return new ExprEqualsEvaluatorEquals(this, lhs, rhs);
        }
    }

    public static class ExprEqualsEvaluatorCoercing implements ExprEvaluator {
        private transient ExprEqualsNodeImpl parent;
        private transient ExprEvaluator lhs;
        private transient ExprEvaluator rhs;
        private transient SimpleNumberCoercer numberCoercerLHS;
        private transient SimpleNumberCoercer numberCoercerRHS;

        public ExprEqualsEvaluatorCoercing(ExprEqualsNodeImpl parent, ExprEvaluator lhs, ExprEvaluator rhs, SimpleNumberCoercer numberCoercerLHS, SimpleNumberCoercer numberCoercerRHS) {
            this.parent = parent;
            this.lhs = lhs;
            this.rhs = rhs;
            this.numberCoercerLHS = numberCoercerLHS;
            this.numberCoercerRHS = numberCoercerRHS;
        }

        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qExprEquals(parent);
            }
            Boolean result = evaluateInternal(eventsPerStream, isNewData, context);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprEquals(result);
            }
            return result;
        }

        private Boolean evaluateInternal(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            Object leftResult = lhs.evaluate(eventsPerStream, isNewData, context);
            Object rightResult = rhs.evaluate(eventsPerStream, isNewData, context);

            if (!parent.isIs) {
                if (leftResult == null || rightResult == null) {
                    // null comparison
                    return null;
                }
            } else {
                if (leftResult == null) {
                    return rightResult == null;
                }
                if (rightResult == null) {
                    return false;
                }
            }

            Number left = numberCoercerLHS.coerceBoxed((Number) leftResult);
            Number right = numberCoercerRHS.coerceBoxed((Number) rightResult);
            return left.equals(right) ^ parent.isNotEquals;
        }

        public Class getType() {
            return Boolean.class;
        }
    }

    public static class ExprEqualsEvaluatorEquals implements ExprEvaluator {
        private transient ExprEqualsNodeImpl parent;
        private transient ExprEvaluator lhs;
        private transient ExprEvaluator rhs;

        public ExprEqualsEvaluatorEquals(ExprEqualsNodeImpl parent, ExprEvaluator lhs, ExprEvaluator rhs) {
            this.parent = parent;
            this.lhs = lhs;
            this.rhs = rhs;
        }

        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qExprEquals(parent);
                Object leftResult = lhs.evaluate(eventsPerStream, isNewData, context);
                Object rightResult = rhs.evaluate(eventsPerStream, isNewData, context);
                if (leftResult == null || rightResult == null) { // null comparison
                    InstrumentationHelper.get().aExprEquals(null);
                    return null;
                }
                boolean result = leftResult.equals(rightResult) ^ parent.isNotEquals();
                InstrumentationHelper.get().aExprEquals(result);
                return result;
            }

            Object leftResult = lhs.evaluate(eventsPerStream, isNewData, context);
            Object rightResult = rhs.evaluate(eventsPerStream, isNewData, context);
            if (leftResult == null || rightResult == null) { // null comparison
                return null;
            }
            return leftResult.equals(rightResult) ^ parent.isNotEquals();
        }

        public Class getType() {
            return Boolean.class;
        }
    }

    public static class ExprEqualsEvaluatorIs implements ExprEvaluator {
        private transient ExprEqualsNodeImpl parent;
        private transient ExprEvaluator lhs;
        private transient ExprEvaluator rhs;

        public ExprEqualsEvaluatorIs(ExprEqualsNodeImpl parent, ExprEvaluator lhs, ExprEvaluator rhs) {
            this.parent = parent;
            this.lhs = lhs;
            this.rhs = rhs;
        }

        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qExprIs(parent);
                Object leftResult = lhs.evaluate(eventsPerStream, isNewData, context);
                Object rightResult = rhs.evaluate(eventsPerStream, isNewData, context);

                boolean result;
                if (leftResult == null) {
                    result = rightResult == null ^ parent.isNotEquals;
                } else {
                    result = (rightResult != null && leftResult.equals(rightResult)) ^ parent.isNotEquals;
                }
                InstrumentationHelper.get().aExprIs(result);
                return result;
            }

            Object leftResult = lhs.evaluate(eventsPerStream, isNewData, context);
            Object rightResult = rhs.evaluate(eventsPerStream, isNewData, context);

            if (leftResult == null) {
                return rightResult == null ^ parent.isNotEquals;
            }
            return (rightResult != null && leftResult.equals(rightResult)) ^ parent.isNotEquals;
        }

        public Class getType() {
            return Boolean.class;
        }
    }
}
