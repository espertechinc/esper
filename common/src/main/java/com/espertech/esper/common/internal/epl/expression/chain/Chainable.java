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
package com.espertech.esper.common.internal.epl.expression.chain;

import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitorWithParent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityValidate.getValidatedSubtree;

public abstract class Chainable {
    private final boolean distinct;
    private final boolean optional;

    public abstract void addParametersTo(Collection<ExprNode> result);

    public abstract void accept(ExprNodeVisitor visitor);

    public abstract void accept(ExprNodeVisitorWithParent visitor);

    public abstract void accept(ExprNodeVisitorWithParent visitor, ExprNode parent);

    public abstract String getRootNameOrEmptyString();

    public abstract List<ExprNode> getParametersOrEmpty();

    public abstract void validateExpressions(ExprNodeOrigin origin, ExprValidationContext validationContext) throws ExprValidationException;

    public Chainable() {
        this(false, false);
    }

    public Chainable(boolean distinct, boolean optional) {
        this.distinct = distinct;
        this.optional = optional;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public boolean isOptional() {
        return optional;
    }

    public static boolean isPlainPropertyChain(Chainable chainable) {
        return chainable instanceof ChainableName && chainable.getRootNameOrEmptyString().contains(".");
    }

    public void validate(ExprNodeOrigin origin, ExprValidationContext validationContext) throws ExprValidationException {
        for (ExprNode node : getParametersOrEmpty()) {
            if (node instanceof ExprNamedParameterNode) {
                throw new ExprValidationException("Named parameters are not allowed");
            }
        }
        validateExpressions(origin, validationContext);
    }

    public static List<Chainable> chainForDot(Chainable chainable) {
        if (!(chainable instanceof ChainableName)) {
            return new ArrayList<>(Collections.singletonList(chainable));
        }
        String[] values = chainable.getRootNameOrEmptyString().split("\\.");
        List<Chainable> chain = new ArrayList<>(values.length + 1);
        for (String value : values) {
            chain.add(new ChainableName(value));
        }
        return chain;
    }

    protected static void validateExpressions(List<ExprNode> expressions, ExprNodeOrigin origin, ExprValidationContext validationContext) throws ExprValidationException {
        for (int i = 0; i < expressions.size(); i++) {
            ExprNode node = expressions.get(i);
            ExprNode validated = getValidatedSubtree(origin, node, validationContext);
            if (node != validated) {
                expressions.set(i, validated);
            }
        }
    }

    protected boolean equalsChainable(Chainable that) {
        return that.distinct == distinct && that.optional == optional;
    }
}
