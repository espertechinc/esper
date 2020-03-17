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

import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeOrigin;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitor;
import com.espertech.esper.common.internal.statement.helper.EPStatementStartMethodHelperValidate;

import static com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityValidate.getValidatedSubtree;

public class ExprAssignmentCurly extends ExprAssignment {

    private ExprNode expression;

    public ExprAssignmentCurly(ExprNode expression) {
        super(expression);
        this.expression = expression;
    }

    public ExprNode getExpression() {
        return expression;
    }

    public void validate(ExprNodeOrigin origin, ExprValidationContext validationContext, boolean allowRHSAggregation) throws ExprValidationException {
        expression = getValidatedSubtree(origin, expression, validationContext);
        EPStatementStartMethodHelperValidate.validateNoAggregations(expression, VALIDATION_AGG_MSG);
    }

    public void accept(ExprNodeVisitor visitor) {
        expression.accept(visitor);
    }
}
