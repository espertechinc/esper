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

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitorWithParent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery.acceptParams;
import static com.espertech.esper.common.internal.util.JavaClassHelper.isTypeInteger;

public class ChainableArray extends Chainable {
    private List<ExprNode> indexes;

    public ChainableArray(List<ExprNode> indexExpressions) {
        this.indexes = indexExpressions;
    }

    public ChainableArray(boolean distinct, boolean optional, List<ExprNode> indexes) {
        super(distinct, optional);
        this.indexes = indexes;
    }

    public List<ExprNode> getIndexes() {
        return indexes;
    }

    public void accept(ExprNodeVisitor visitor) {
        acceptParams(visitor, indexes);
    }

    public void accept(ExprNodeVisitorWithParent visitor) {
        acceptParams(visitor, indexes);
    }

    public void accept(ExprNodeVisitorWithParent visitor, ExprNode parent) {
        acceptParams(visitor, indexes, parent);
    }

    public List<ExprNode> getParametersOrEmpty() {
        return Collections.emptyList();
    }

    public void validateExpressions(ExprNodeOrigin origin, ExprValidationContext validationContext) throws ExprValidationException {
        validateExpressions(indexes, origin, validationContext);
    }

    public String getRootNameOrEmptyString() {
        return "";
    }

    public void addParametersTo(Collection<ExprNode> result) {
        result.addAll(indexes);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChainableArray that = (ChainableArray) o;
        return super.equalsChainable(that) && ExprNodeUtilityCompare.deepEquals(indexes, that.indexes);
    }

    public int hashCode() {
        return 0;
    }

    public static ExprNode validateSingleIndexExpr(List<ExprNode> indexes, Supplier<String> supplier) throws ExprValidationException {
        if (indexes.size() != 1) {
            throw new ExprValidationException("Incorrect number of index expressions for array operation, expected a single expression returning an integer value but received " + indexes.size() + " expressions for " + supplier.get());
        }
        ExprNode node = indexes.get(0);
        EPType evaluationType = node.getForge().getEvaluationType();
        if (!isTypeInteger(evaluationType)) {
            throw new ExprValidationException("Incorrect index expression for array operation, expected an expression returning an integer value but the expression '" +
                ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(node) +
                "' returns '" +
                (evaluationType == null ? "null" : evaluationType.getTypeName()) + "' for " + supplier.get());
        }
        return node;
    }
}
