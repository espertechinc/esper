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

import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.LikeUtil;

import java.io.StringWriter;

/**
 * Represents the like-clause in an expression tree.
 */
public class ExprLikeNode extends ExprNodeBase {
    private static final long serialVersionUID = 34888860063217132L;

    private final boolean isNot;

    private transient ExprLikeNodeForge forge;

    /**
     * Ctor.
     *
     * @param not is true if this is a "not like", or false if just a like
     */
    public ExprLikeNode(boolean not) {
        this.isNot = not;
    }

    public Class getEvaluationType() {
        return Boolean.class;
    }

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return forge;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if ((this.getChildNodes().length != 2) && (this.getChildNodes().length != 3)) {
            throw new ExprValidationException("The 'like' operator requires 2 (no escape) or 3 (with escape) child expressions");
        }

        // check eval child node - can be String or numeric
        Class evalChildType = getChildNodes()[0].getForge().getEvaluationType();
        boolean isNumericValue = JavaClassHelper.isNumeric(evalChildType);
        if ((evalChildType != String.class) && (!isNumericValue)) {
            throw new ExprValidationException("The 'like' operator requires a String or numeric type left-hand expression");
        }

        // check pattern child node
        Class patternChildType = getChildNodes()[1].getForge().getEvaluationType();
        if (patternChildType != String.class) {
            throw new ExprValidationException("The 'like' operator requires a String-type pattern expression");
        }
        boolean isConstantPattern = getChildNodes()[1].isConstantResult();

        // check escape character node
        boolean isConstantEscape = true;
        if (this.getChildNodes().length == 3) {
            if (this.getChildNodes()[2].getForge().getEvaluationType() != String.class) {
                throw new ExprValidationException("The 'like' operator escape parameter requires a character-type value");
            }
            isConstantEscape = getChildNodes()[2].isConstantResult();
        }

        if (isConstantPattern && isConstantEscape) {
            String patternVal = (String) getChildNodes()[1].getForge().getExprEvaluator().evaluate(null, true, validationContext.getExprEvaluatorContext());
            if (patternVal == null) {
                throw new ExprValidationException("The 'like' operator pattern returned null");
            }
            String escape = "\\";
            Character escapeCharacter = null;
            if (this.getChildNodes().length == 3) {
                escape = (String) getChildNodes()[2].getForge().getExprEvaluator().evaluate(null, true, validationContext.getExprEvaluatorContext());
            }
            if (escape.length() > 0) {
                escapeCharacter = escape.charAt(0);
            }
            LikeUtil likeUtil = new LikeUtil(patternVal, escapeCharacter, false);
            forge = new ExprLikeNodeForgeConst(this, isNumericValue, likeUtil);
        } else {
            forge = new ExprLikeNodeForgeNonconst(this, isNumericValue);
        }
        return null;
    }

    public Class getType() {
        return Boolean.class;
    }

    public boolean isConstantResult() {
        return false;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
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
