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
import java.util.*;

/**
 * Represents the in-clause (set check) function in an expression tree.
 */
public class ExprInNodeImpl extends ExprNodeBase implements ExprInNode {
    private static final long serialVersionUID = -601723009914169907L;

    private final boolean isNotIn;

    private transient ExprInNodeForge forge;

    /**
     * Ctor.
     *
     * @param isNotIn is true for "not in" and false for "in"
     */
    public ExprInNodeImpl(boolean isNotIn) {
        this.isNotIn = isNotIn;
    }

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public ExprForge getForge() {
        return forge;
    }

    /**
     * Returns true for not-in, false for regular in
     *
     * @return false for "val in (a,b,c)" or true for "val not in (a,b,c)"
     */
    public boolean isNotIn() {
        return isNotIn;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        validateWithoutContext();
        return null;
    }

    public void validateWithoutContext() throws ExprValidationException {
        if (this.getChildNodes().length < 2) {
            throw new ExprValidationException("The IN operator requires at least 2 child expressions");
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
                continue;
            }
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

        // Check if we need to coerce
        boolean mustCoerce = false;
        SimpleNumberCoercer coercer = null;
        if (JavaClassHelper.isNumeric(coercionType)) {
            for (Class compareType : comparedTypes) {
                if (coercionType != JavaClassHelper.getBoxedType(compareType)) {
                    mustCoerce = true;
                }
            }
            if (mustCoerce) {
                coercer = SimpleNumberCoercerFactory.getCoercer(null, JavaClassHelper.getBoxedType(coercionType));
            }
        }

        forge = new ExprInNodeForge(this, mustCoerce, coercer, coercionType, hasCollectionOrArray);
    }

    public boolean isConstantResult() {
        return false;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprInNodeImpl)) {
            return false;
        }

        ExprInNodeImpl other = (ExprInNodeImpl) node;
        return other.isNotIn == this.isNotIn;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        String delimiter = "";
        Iterator<ExprNode> it = Arrays.asList(this.getChildNodes()).iterator();
        it.next().toEPL(writer, getPrecedence());
        if (isNotIn) {
            writer.append(" not in (");
        } else {
            writer.append(" in (");
        }

        do {
            ExprNode inSetValueExpr = it.next();
            writer.append(delimiter);
            inSetValueExpr.toEPL(writer, getPrecedence());
            delimiter = ",";
        }
        while (it.hasNext());
        writer.append(')');
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.RELATIONAL_BETWEEN_IN;
    }
}
