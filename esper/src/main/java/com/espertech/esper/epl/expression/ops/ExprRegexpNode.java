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

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Represents the regexp-clause in an expression tree.
 */
public class ExprRegexpNode extends ExprNodeBase implements ExprEvaluator {
    private final boolean isNot;

    private Pattern pattern;
    private boolean isNumericValue;
    private boolean isConstantPattern;
    private transient ExprEvaluator[] evaluators;
    private static final long serialVersionUID = -837177267278295664L;

    /**
     * Ctor.
     *
     * @param not is true if the it's a "not regexp" expression, of false for regular regexp
     */
    public ExprRegexpNode(boolean not) {
        this.isNot = not;
    }

    @Override
    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length != 2) {
            throw new ExprValidationException("The regexp operator requires 2 child expressions");
        }
        evaluators = ExprNodeUtility.getEvaluators(this.getChildNodes());

        // check pattern child node
        Class patternChildType = evaluators[1].getType();
        if (patternChildType != String.class) {
            throw new ExprValidationException("The regexp operator requires a String-type pattern expression");
        }
        if (this.getChildNodes()[1].isConstantResult()) {
            isConstantPattern = true;
        }

        // check eval child node - can be String or numeric
        Class evalChildType = evaluators[0].getType();
        isNumericValue = JavaClassHelper.isNumeric(evalChildType);
        if ((evalChildType != String.class) && (!isNumericValue)) {
            throw new ExprValidationException("The regexp operator requires a String or numeric type left-hand expression");
        }
        return null;
    }

    public Class getType() {
        return Boolean.class;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprRegexp(this);
        }
        if (pattern == null) {
            String patternText = (String) evaluators[1].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (patternText == null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aExprRegexp(null);
                }
                return null;
            }
            try {
                pattern = Pattern.compile(patternText);
            } catch (PatternSyntaxException ex) {
                throw new EPException("Error compiling regex pattern '" + patternText + '\'', ex);
            }
        } else {
            if (!isConstantPattern) {
                String patternText = (String) evaluators[1].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                if (patternText == null) {
                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().aExprRegexp(null);
                    }
                    return null;
                }
                try {
                    pattern = Pattern.compile(patternText);
                } catch (PatternSyntaxException ex) {
                    throw new EPException("Error compiling regex pattern '" + patternText + '\'', ex);
                }
            }
        }

        Object evalValue = evaluators[0].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (evalValue == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprRegexp(null);
            }
            return null;
        }

        if (isNumericValue) {
            evalValue = evalValue.toString();
        }

        Boolean result = pattern.matcher((CharSequence) evalValue).matches();

        if (isNot) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprRegexp(!result);
            }
            return !result;
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprRegexp(result);
        }
        return result;
    }

    public boolean equalsNode(ExprNode node) {
        if (!(node instanceof ExprRegexpNode)) {
            return false;
        }

        ExprRegexpNode other = (ExprRegexpNode) node;
        if (this.isNot != other.isNot) {
            return false;
        }
        return true;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        this.getChildNodes()[0].toEPL(writer, getPrecedence());
        if (isNot) {
            writer.append(" not");
        }
        writer.append(" regexp ");
        this.getChildNodes()[1].toEPL(writer, getPrecedence());
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.RELATIONAL_BETWEEN_IN;
    }

    /**
     * Returns true if this is a "not regexp", or false if just a regexp
     *
     * @return indicator whether negated or not
     */
    public boolean isNot() {
        return isNot;
    }
}
