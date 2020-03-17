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

import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitor;
import com.espertech.esper.common.internal.statement.helper.EPStatementStartMethodHelperValidate;

public class ExprAssignmentLHSArrayElement extends ExprAssignmentLHS {

    private ExprNode indexExpression;
    private ExprArrayElement arrayElement;

    public ExprAssignmentLHSArrayElement(ExprArrayElement arrayElement, ExprNode indexExpression) {
        super(arrayElement.getArrayPropName());
        this.arrayElement = arrayElement;
        this.indexExpression = indexExpression;
    }

    public void accept(ExprNodeVisitor visitor) {
        indexExpression.accept(visitor);
        arrayElement.accept(visitor);
    }

    public void validate(ExprNodeOrigin origin, ExprValidationContext validationContext) throws ExprValidationException {
        indexExpression = ExprNodeUtilityValidate.getValidatedSubtree(origin, indexExpression, validationContext);
        arrayElement = (ExprArrayElement) ExprNodeUtilityValidate.getValidatedSubtree(origin, arrayElement, validationContext);
        EPStatementStartMethodHelperValidate.validateNoAggregations(indexExpression, ExprAssignment.VALIDATION_AGG_MSG);
        EPStatementStartMethodHelperValidate.validateNoAggregations(arrayElement, ExprAssignment.VALIDATION_AGG_MSG);
    }

    public String getFullIdentifier() {
        return arrayElement.getArrayPropName();
    }

    public ExprNode getIndexExpression() {
        return indexExpression;
    }

    public ExprArrayElement getArrayElement() {
        return arrayElement;
    }
}

