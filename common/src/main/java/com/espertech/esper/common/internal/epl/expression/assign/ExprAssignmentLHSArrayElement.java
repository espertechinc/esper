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
package com.espertech.esper.common.internal.epl.expression.assign;

import com.espertech.esper.common.internal.epl.expression.chain.ChainableArray;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitor;
import com.espertech.esper.common.internal.statement.helper.EPStatementStartMethodHelperValidate;

import java.util.Collections;
import java.util.List;

public class ExprAssignmentLHSArrayElement extends ExprAssignmentLHS {

    private List<ExprNode> indexExpressions;

    public ExprAssignmentLHSArrayElement(String ident, List<ExprNode> indexExpressions) {
        super(ident);
        this.indexExpressions = indexExpressions;
    }

    public void accept(ExprNodeVisitor visitor) {
        for (ExprNode node : indexExpressions) {
            node.accept(visitor);
        }
    }

    public void validate(ExprNodeOrigin origin, ExprValidationContext validationContext) throws ExprValidationException {
        ExprNode index = indexExpressions.get(0);
        index = ExprNodeUtilityValidate.getValidatedSubtree(origin, index, validationContext);
        indexExpressions = Collections.singletonList(index);
        ChainableArray.validateSingleIndexExpr(indexExpressions, () -> "expression '" + ident + "'");
        EPStatementStartMethodHelperValidate.validateNoAggregations(index, ExprAssignment.VALIDATION_AGG_MSG);
    }

    public ExprNode getIndexExpression() {
        return indexExpressions.get(0);
    }

    public String getFullIdentifier() {
        return ident;
    }
}

