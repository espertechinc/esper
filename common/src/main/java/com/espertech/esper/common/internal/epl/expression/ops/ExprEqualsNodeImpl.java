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
import com.espertech.esper.common.internal.util.CoercionException;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import java.io.StringWriter;
import java.util.Map;

/**
 * Represents an equals (=) comparator in a filter expressiun tree.
 */
public class ExprEqualsNodeImpl extends ExprNodeBase implements ExprEqualsNode {
    private final boolean isNotEquals;
    private final boolean isIs;

    private transient ExprEqualsNodeForge forge;

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
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return forge;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        // Must have 2 child nodes
        if (this.getChildNodes().length != 2) {
            throw new ExprValidationException("Invalid use of equals, expecting left-hand side and right-hand side but received " + this.getChildNodes().length + " expressions");
        }

        // Must be the same boxed type returned by expressions under this
        ExprNode lhs = getChildNodes()[0];
        ExprNode rhs = getChildNodes()[1];
        EPType lhsType = lhs.getForge().getEvaluationType();
        EPType rhsType = rhs.getForge().getEvaluationType();

        // Null constants can be compared for any type
        if (lhsType == null || lhsType == EPTypeNull.INSTANCE || rhsType == null || rhsType == EPTypeNull.INSTANCE) {
            forge = new ExprEqualsNodeForgeNC(this);
            return null;
        }

        EPTypeClass lhsClass = (EPTypeClass) lhsType;
        EPTypeClass rhsClass = (EPTypeClass) rhsType;
        lhsClass = JavaClassHelper.getBoxedType(lhsClass);
        rhsClass = JavaClassHelper.getBoxedType(rhsClass);

        if (lhsClass.equals(rhsClass) || lhsClass.getType().isAssignableFrom(rhsClass.getType())) {
            forge = new ExprEqualsNodeForgeNC(this);
            return null;
        }

        // Get the common type such as Bool, String or Double and Long
        EPTypeClass coercionType;
        try {
            coercionType = JavaClassHelper.getCompareToCoercionType(lhsClass, rhsClass);
        } catch (CoercionException ex) {
            throw new ExprValidationException("Implicit conversion from datatype '" +
                rhsClass +
                "' to '" +
                lhsClass +
                "' is not allowed");
        }

        // Check if we need to coerce
        if ((coercionType == JavaClassHelper.getBoxedType(lhsClass)) &&
            (coercionType == JavaClassHelper.getBoxedType(rhsClass))) {
            forge = new ExprEqualsNodeForgeNC(this);
        } else {
            if (!JavaClassHelper.isNumeric(coercionType)) {
                throw new ExprValidationException("Cannot convert datatype '" + coercionType.getTypeName() + "' to a value that fits both type '" + lhsClass.getTypeName() + "' and type '" + rhsClass.getTypeName() + "'");
            }
            SimpleNumberCoercer numberCoercerLHS = SimpleNumberCoercerFactory.getCoercer(lhsClass, coercionType);
            SimpleNumberCoercer numberCoercerRHS = SimpleNumberCoercerFactory.getCoercer(rhsClass, coercionType);
            forge = new ExprEqualsNodeForgeCoercion(this, numberCoercerLHS, numberCoercerRHS, lhsClass, rhsClass);
        }
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Map<String, Object> getEventType() {
        return null;
    }

    public void toPrecedenceFreeEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
        this.getChildNodes()[0].toEPL(writer, getPrecedence(), flags);
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
        this.getChildNodes()[1].toEPL(writer, getPrecedence(), flags);
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.EQUALS;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprEqualsNode)) {
            return false;
        }

        ExprEqualsNode other = (ExprEqualsNode) node;
        return other.isNotEquals() == this.isNotEquals;
    }

    public boolean isNotEquals() {
        return isNotEquals;
    }

    public boolean isIs() {
        return isIs;
    }
}
