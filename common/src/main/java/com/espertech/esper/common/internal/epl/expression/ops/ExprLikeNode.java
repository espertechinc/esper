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
package com.espertech.esper.common.internal.epl.expression.ops;

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.LikeUtil;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.util.JavaClassHelper.isTypeString;

/**
 * Represents the like-clause in an expression tree.
 */
public class ExprLikeNode extends ExprNodeBase {
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
        EPType evalType = getChildNodes()[0].getForge().getEvaluationType();
        boolean isNumericValue = JavaClassHelper.isNumeric(evalType);
        if (!isTypeString(evalType) && !isNumericValue) {
            throw new ExprValidationException("The 'like' operator requires a String or numeric type left-hand expression");
        }

        // check pattern child node
        ExprForge patternForge = getChildNodes()[1].getForge();
        if (!isTypeString(patternForge.getEvaluationType())) {
            throw new ExprValidationException("The 'like' operator requires a String-type pattern expression");
        }
        boolean isConstantPattern = patternForge.getForgeConstantType().isCompileTimeConstant();

        // check escape character node
        boolean isConstantEscape = true;
        if (this.getChildNodes().length == 3) {
            ExprForge escapeForge = this.getChildNodes()[2].getForge();
            if (!isTypeString(escapeForge.getEvaluationType())) {
                throw new ExprValidationException("The 'like' operator escape parameter requires a character-type value");
            }
            isConstantEscape = escapeForge.getForgeConstantType().isCompileTimeConstant();
        }

        if (isConstantPattern && isConstantEscape) {
            String patternVal = (String) getChildNodes()[1].getForge().getExprEvaluator().evaluate(null, true, null);
            if (patternVal == null) {
                throw new ExprValidationException("The 'like' operator pattern returned null");
            }
            String escape = "\\";
            Character escapeCharacter = null;
            if (this.getChildNodes().length == 3) {
                escape = (String) getChildNodes()[2].getForge().getExprEvaluator().evaluate(null, true, null);
            }
            if (escape.length() > 0) {
                escapeCharacter = escape.charAt(0);
            }
            LikeUtil likeUtil = new LikeUtil(patternVal, escapeCharacter, false);
            CodegenExpression likeUtilInit = newInstance(LikeUtil.EPTYPE, constant(patternVal), constant(escapeCharacter), constantFalse());
            forge = new ExprLikeNodeForgeConst(this, isNumericValue, likeUtil, likeUtilInit);
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

    public void toPrecedenceFreeEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
        this.getChildNodes()[0].toEPL(writer, getPrecedence(), flags);

        if (isNot) {
            writer.append(" not");
        }

        writer.append(" like ");
        this.getChildNodes()[1].toEPL(writer, getPrecedence(), flags);

        if (this.getChildNodes().length == 3) {
            writer.append(" escape ");
            this.getChildNodes()[2].toEPL(writer, getPrecedence(), flags);
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
