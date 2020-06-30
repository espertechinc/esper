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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.type.RelationalOpEnum;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;

/**
 * Represents a lesser or greater then (&lt;/&lt;=/&gt;/&gt;=) expression in a filter expression tree.
 */
public class ExprRelationalOpNodeImpl extends ExprNodeBase implements ExprRelationalOpNode {
    private final RelationalOpEnum relationalOpEnum;

    private transient ExprRelationalOpNodeForge forge;

    /**
     * Ctor.
     *
     * @param relationalOpEnum - type of compare, ie. lt, gt, le, ge
     */
    public ExprRelationalOpNodeImpl(RelationalOpEnum relationalOpEnum) {
        this.relationalOpEnum = relationalOpEnum;
    }

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return forge;
    }

    public boolean isConstantResult() {
        return false;
    }

    /**
     * Returns the type of relational op used.
     *
     * @return enum with relational op type
     */
    public RelationalOpEnum getRelationalOpEnum() {
        return relationalOpEnum;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        // Must have 2 child nodes
        if (this.getChildNodes().length != 2) {
            throw new IllegalStateException("Relational op node does not have exactly 2 parameters");
        }

        // Must be either numeric or string
        ExprForge lhsForge = getChildNodes()[0].getForge();
        ExprForge rhsForge = getChildNodes()[1].getForge();

        EPTypeClass lhsClass = validateStringOrNumeric(lhsForge);
        EPTypeClass rhsClass = validateStringOrNumeric(rhsForge);

        if ((lhsClass.getType() != String.class) || (rhsClass.getType() != String.class)) {
            if (!JavaClassHelper.isNumeric(lhsClass)) {
                throw new ExprValidationException("Implicit conversion from datatype '" +
                    lhsClass + "' to numeric is not allowed");
            }
            if (!JavaClassHelper.isNumeric(rhsClass)) {
                throw new ExprValidationException("Implicit conversion from datatype '" +
                    rhsClass + "' to numeric is not allowed");
            }
        }

        EPTypeClass compareType = JavaClassHelper.getCompareToCoercionType(lhsClass, rhsClass);
        RelationalOpEnum.Computer computer = relationalOpEnum.getComputer(compareType, lhsClass, rhsClass);
        forge = new ExprRelationalOpNodeForge(this, computer);
        return null;
    }

    public void toPrecedenceFreeEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
        this.getChildNodes()[0].toEPL(writer, getPrecedence(), flags);
        writer.append(relationalOpEnum.getExpressionText());
        this.getChildNodes()[1].toEPL(writer, getPrecedence(), flags);
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.RELATIONAL_BETWEEN_IN;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprRelationalOpNodeImpl)) {
            return false;
        }

        ExprRelationalOpNodeImpl other = (ExprRelationalOpNodeImpl) node;

        if (other.relationalOpEnum != this.relationalOpEnum) {
            return false;
        }

        return true;
    }

    private EPTypeClass validateStringOrNumeric(ExprForge forge) throws ExprValidationException {
        EPType type = forge.getEvaluationType();
        if (type == null || type == EPTypeNull.INSTANCE) {
            throw new ExprValidationException("Null-type value is not allow for relational operator");
        }
        EPTypeClass typeClass = (EPTypeClass) type;
        if (typeClass.getType() == String.class) {
            return typeClass;
        }
        return JavaClassHelper.getBoxedType(ExprNodeUtilityValidate.validateReturnsNumeric(forge));
    }
}
