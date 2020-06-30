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

import com.espertech.esper.common.client.type.*;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.util.CoercionException;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import java.io.StringWriter;
import java.util.*;

/**
 * Represents the in-clause (set check) function in an expression tree.
 */
public class ExprInNodeImpl extends ExprNodeBase implements ExprInNode {
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
        EPType typeRoot = getChildNodes()[0].getForge().getEvaluationType();
        EPType typeRootBoxed = JavaClassHelper.getBoxedType(typeRoot);
        EPTypeClass typeRootClass = ExprNodeUtilityValidate.validateLHSTypeAnyAllSomeIn(typeRootBoxed);

        List<EPType> comparedTypes = new ArrayList<>();
        comparedTypes.add(typeRootClass);
        boolean hasCollectionOrArray = false;
        for (int i = 0; i < this.getChildNodes().length - 1; i++) {
            EPType childType = getChildNodes()[i + 1].getForge().getEvaluationType();
            if (childType == null || childType == EPTypeNull.INSTANCE) {
                continue;
            }
            EPTypeClass childClass = (EPTypeClass) childType;
            if (childClass.getType().isArray()) {
                hasCollectionOrArray = true;
                if (childClass.getType().getComponentType() != Object.class) {
                    EPTypeClass componentType = JavaClassHelper.getArrayComponentType(childClass);
                    comparedTypes.add(componentType);
                }
            } else if (JavaClassHelper.isImplementsInterface(childClass, Collection.class)) {
                hasCollectionOrArray = true;
            } else if (JavaClassHelper.isImplementsInterface(childClass, Map.class)) {
                hasCollectionOrArray = true;
            } else {
                comparedTypes.add(childType);
            }
        }

        // Determine common denominator type
        EPType coercionType;
        try {
            coercionType = JavaClassHelper.getCommonCoercionType(comparedTypes.toArray(new EPType[comparedTypes.size()]));
        } catch (CoercionException ex) {
            throw new ExprValidationException("Implicit conversion not allowed: " + ex.getMessage());
        }
        if (coercionType == EPTypeNull.INSTANCE) {
            throw new ExprValidationException("Implicit conversion from null-type is not allowed");
        }
        EPTypeClass coercionClass = (EPTypeClass) coercionType;

        // Check if we need to coerce
        boolean mustCoerce = false;
        SimpleNumberCoercer coercer = null;
        if (JavaClassHelper.isNumeric(coercionClass)) {
            for (EPType compareType : comparedTypes) {
                if (!coercionType.equals(JavaClassHelper.getBoxedType(compareType))) {
                    mustCoerce = true;
                }
            }
            if (mustCoerce) {
                coercer = SimpleNumberCoercerFactory.getCoercer(null, JavaClassHelper.getBoxedType(coercionClass));
            }
        }

        forge = new ExprInNodeForge(this, mustCoerce, coercer, coercionClass, hasCollectionOrArray);
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

    public void toPrecedenceFreeEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
        String delimiter = "";
        Iterator<ExprNode> it = Arrays.asList(this.getChildNodes()).iterator();
        it.next().toEPL(writer, getPrecedence(), flags);
        if (isNotIn) {
            writer.append(" not in (");
        } else {
            writer.append(" in (");
        }

        do {
            ExprNode inSetValueExpr = it.next();
            writer.append(delimiter);
            inSetValueExpr.toEPL(writer, getPrecedence(), flags);
            delimiter = ",";
        }
        while (it.hasNext());
        writer.append(')');
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.RELATIONAL_BETWEEN_IN;
    }
}
