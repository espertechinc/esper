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
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.LikeUtil;

import java.io.StringWriter;

/**
 * Represents the like-clause in an expression tree.
 */
public class ExprLikeNode extends ExprNodeBase implements ExprEvaluator {
    private final boolean isNot;

    private boolean isNumericValue;
    private boolean isConstantPattern;

    private transient LikeUtil likeUtil;
    private transient ExprEvaluator[] evaluators;

    private static final long serialVersionUID = 34888860063217132L;

    /**
     * Ctor.
     *
     * @param not is true if this is a "not like", or false if just a like
     */
    public ExprLikeNode(boolean not) {
        this.isNot = not;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if ((this.getChildNodes().length != 2) && (this.getChildNodes().length != 3)) {
            throw new ExprValidationException("The 'like' operator requires 2 (no escape) or 3 (with escape) child expressions");
        }
        evaluators = ExprNodeUtility.getEvaluators(this.getChildNodes());

        // check eval child node - can be String or numeric
        Class evalChildType = evaluators[0].getType();
        isNumericValue = JavaClassHelper.isNumeric(evalChildType);
        if ((evalChildType != String.class) && (!isNumericValue)) {
            throw new ExprValidationException("The 'like' operator requires a String or numeric type left-hand expression");
        }

        // check pattern child node
        ExprEvaluator patternChildNode = evaluators[1];
        Class patternChildType = patternChildNode.getType();
        if (patternChildType != String.class) {
            throw new ExprValidationException("The 'like' operator requires a String-type pattern expression");
        }
        if (getChildNodes()[1].isConstantResult()) {
            isConstantPattern = true;
        }

        // check escape character node
        if (this.getChildNodes().length == 3) {
            ExprEvaluator escapeChildNode = evaluators[2];
            if (escapeChildNode.getType() != String.class) {
                throw new ExprValidationException("The 'like' operator escape parameter requires a character-type value");
            }
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
            InstrumentationHelper.get().qExprLike(this);
        }
        if (likeUtil == null) {
            String patternVal = (String) evaluators[1].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (patternVal == null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aExprLike(null);
                }
                return null;
            }
            String escape = "\\";
            Character escapeCharacter = null;
            if (this.getChildNodes().length == 3) {
                escape = (String) evaluators[2].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            }
            if (escape.length() > 0) {
                escapeCharacter = escape.charAt(0);
            }
            likeUtil = new LikeUtil(patternVal, escapeCharacter, false);
        } else {
            if (!isConstantPattern) {
                String patternVal = (String) evaluators[1].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
                if (patternVal == null) {
                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().aExprLike(null);
                    }
                    return null;
                }
                likeUtil.resetPattern(patternVal);
            }
        }

        Object evalValue = evaluators[0].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (evalValue == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprLike(null);
            }
            return null;
        }

        if (isNumericValue) {
            evalValue = evalValue.toString();
        }

        Boolean result = likeUtil.compare((String) evalValue);

        if (isNot) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprLike(!result);
            }
            return !result;
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprLike(result);
        }
        return result;
    }

    public boolean equalsNode(ExprNode node) {
        if (!(node instanceof ExprLikeNode)) {
            return false;
        }

        ExprLikeNode other = (ExprLikeNode) node;
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

        writer.append(" like ");
        this.getChildNodes()[1].toEPL(writer, getPrecedence());

        if (this.getChildNodes().length == 3) {
            writer.append(" escape ");
            this.getChildNodes()[2].toEPL(writer, getPrecedence());
        }

    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.RELATIONAL_BETWEEN_IN;
    }

    /**
     * Returns true if this is a "not like", or false if just a like
     *
     * @return indicator whether negated or not
     */
    public boolean isNot() {
        return isNot;
    }
}
