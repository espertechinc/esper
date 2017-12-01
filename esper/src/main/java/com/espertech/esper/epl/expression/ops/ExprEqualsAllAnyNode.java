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
import com.espertech.esper.util.CoercionException;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.SimpleNumberCoercer;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Represents an equals-for-group (= ANY/ALL/SOME (expression list)) comparator in a expression tree.
 */
public class ExprEqualsAllAnyNode extends ExprNodeBase {
    private static final long serialVersionUID = -2410457251623137179L;

    private final boolean isNot;
    private final boolean isAll;

    private transient ExprEqualsAllAnyNodeForge forge;

    /**
     * Ctor.
     *
     * @param isNotEquals - true if this is a (!=) not equals rather then equals, false if its a '=' equals
     * @param isAll       - true if all, false for any
     */
    public ExprEqualsAllAnyNode(boolean isNotEquals, boolean isAll) {
        this.isNot = isNotEquals;
        this.isAll = isAll;
    }

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return forge;
    }


    /**
     * Returns true if this is a NOT EQUALS node, false if this is a EQUALS node.
     *
     * @return true for !=, false for =
     */
    public boolean isNot() {
        return isNot;
    }

    /**
     * True if all.
     *
     * @return all-flag
     */
    public boolean isAll() {
        return isAll;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        // Must have 2 child nodes
        if (this.getChildNodes().length < 1) {
            throw new IllegalStateException("Equals group node does not have 1 or more parameters");
        }

        // Must be the same boxed type returned by expressions under this
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
            if (propType == null) {
                // no action
            } else if (propType.isArray()) {
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
        Class coercionTypeBoxed;
        try {
            coercionTypeBoxed = JavaClassHelper.getCommonCoercionType(comparedTypes.toArray(new Class[comparedTypes.size()]));
        } catch (CoercionException ex) {
            throw new ExprValidationException("Implicit conversion not allowed: " + ex.getMessage());
        }

        // Check if we need to coerce
        boolean mustCoerce = false;
        SimpleNumberCoercer coercer = null;
        if (JavaClassHelper.isNumeric(coercionTypeBoxed)) {
            for (Class compareType : comparedTypes) {
                if (coercionTypeBoxed != JavaClassHelper.getBoxedType(compareType)) {
                    mustCoerce = true;
                }
            }
            if (mustCoerce) {
                coercer = SimpleNumberCoercerFactory.getCoercer(null, JavaClassHelper.getBoxedType(coercionTypeBoxed));
            }
        }
        forge = new ExprEqualsAllAnyNodeForge(this, mustCoerce, coercer, coercionTypeBoxed, hasCollectionOrArray);
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        this.getChildNodes()[0].toEPL(writer, getPrecedence());
        if (isAll) {
            if (isNot) {
                writer.append("!=all");
            } else {
                writer.append("=all");
            }
        } else {
            if (isNot) {
                writer.append("!=any");
            } else {
                writer.append("=any");
            }
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
        return ExprPrecedenceEnum.EQUALS;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprEqualsAllAnyNode)) {
            return false;
        }

        ExprEqualsAllAnyNode other = (ExprEqualsAllAnyNode) node;
        return (other.isNot == this.isNot) && (other.isAll == this.isAll);
    }
}
