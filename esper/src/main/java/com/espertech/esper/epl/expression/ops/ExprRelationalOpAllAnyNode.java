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
import com.espertech.esper.type.RelationalOpEnum;
import com.espertech.esper.util.CoercionException;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Represents a lesser or greater then (&lt;/&lt;=/&gt;/&gt;=) expression in a filter expression tree.
 */
public class ExprRelationalOpAllAnyNode extends ExprNodeBase {
    private static final long serialVersionUID = -9212002972361997109L;

    private final RelationalOpEnum relationalOpEnum;
    private final boolean isAll;

    private transient ExprRelationalOpAllAnyNodeForge forge;

    /**
     * Ctor.
     *
     * @param relationalOpEnum - type of compare, ie. lt, gt, le, ge
     * @param isAll            - true if all, false for any
     */
    public ExprRelationalOpAllAnyNode(RelationalOpEnum relationalOpEnum, boolean isAll) {
        this.relationalOpEnum = relationalOpEnum;
        this.isAll = isAll;
    }

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public ExprForge getForge() {
        return forge;
    }

    public boolean isConstantResult() {
        return false;
    }

    /**
     * Returns true for ALL, false for ANY.
     *
     * @return indicator all or any
     */
    public boolean isAll() {
        return isAll;
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
        if (this.getChildNodes().length < 1) {
            throw new IllegalStateException("Group relational op node must have 1 or more parameters");
        }
        Class typeOne = JavaClassHelper.getBoxedType(getChildNodes()[0].getForge().getEvaluationType());

        // collections, array or map not supported
        if ((typeOne.isArray()) || (JavaClassHelper.isImplementsInterface(typeOne, Collection.class)) || (JavaClassHelper.isImplementsInterface(typeOne, Map.class))) {
            throw new ExprValidationException("Collection or array comparison is not allowed for the IN, ANY, SOME or ALL keywords");
        }

        List<Class> comparedTypes = new ArrayList<Class>();
        comparedTypes.add(typeOne);
        boolean hasCollectionOrArray = false;
        for (int i = 0; i < this.getChildNodes().length - 1; i++) {
            Class propType = getChildNodes()[i + 1].getForge().getEvaluationType();
            if (propType.isArray()) {
                hasCollectionOrArray = true;
                if (propType.getComponentType() != Object.class) {
                    comparedTypes.add(propType.getComponentType());
                }
            } else if (JavaClassHelper.isImplementsInterface(propType, Collection.class)) {
                hasCollectionOrArray = true;
            } else if (JavaClassHelper.isImplementsInterface(propType, Map.class)) {
                hasCollectionOrArray = true;
            } else {
                comparedTypes.add(propType);
            }
        }

        // Determine common denominator type
        Class coercionType;
        try {
            coercionType = JavaClassHelper.getCommonCoercionType(comparedTypes.toArray(new Class[comparedTypes.size()]));
        } catch (CoercionException ex) {
            throw new ExprValidationException("Implicit conversion not allowed: " + ex.getMessage());
        }

        // Must be either numeric or string
        if (coercionType != String.class) {
            if (!JavaClassHelper.isNumeric(coercionType)) {
                throw new ExprValidationException("Implicit conversion from datatype '" +
                        coercionType.getSimpleName() +
                        "' to numeric is not allowed");
            }
        }

        RelationalOpEnum.Computer computer = relationalOpEnum.getComputer(coercionType, coercionType, coercionType);
        forge = new ExprRelationalOpAllAnyNodeForge(this, computer, hasCollectionOrArray);
        return null;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        this.getChildNodes()[0].toEPL(writer, getPrecedence());
        writer.append(relationalOpEnum.getExpressionText());
        if (isAll) {
            writer.append("all");
        } else {
            writer.append("any");
        }

        writer.append("(");
        String delimiter = "";

        for (int i = 0; i < this.getChildNodes().length - 1; i++) {
            writer.append(delimiter);
            this.getChildNodes()[i + 1].toEPL(writer, getPrecedence());
            delimiter = ",";
        }
        writer.append(")");
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.RELATIONAL_BETWEEN_IN;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprRelationalOpAllAnyNode)) {
            return false;
        }

        ExprRelationalOpAllAnyNode other = (ExprRelationalOpAllAnyNode) node;

        if ((other.relationalOpEnum != this.relationalOpEnum) ||
                (other.isAll != this.isAll)) {
            return false;
        }

        return true;
    }
}
