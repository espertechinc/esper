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

import com.espertech.esper.common.internal.epl.expression.core.ExprNodeOrigin;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitor;

public abstract class ExprAssignmentLHS {
    protected final String ident;

    public abstract void validate(ExprNodeOrigin origin, ExprValidationContext validationContext) throws ExprValidationException;
    public abstract void accept(ExprNodeVisitor visitor);
    public abstract String getFullIdentifier();

    public ExprAssignmentLHS(String ident) {
        this.ident = ident;
    }

    public String getIdent() {
        return ident;
    }
}
