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

public class ExprAssignmentLHSIdentWSubprop extends ExprAssignmentLHS {

    private final String subpropertyName;

    public ExprAssignmentLHSIdentWSubprop(String name, String subpropertyName) {
        super(name);
        this.subpropertyName = subpropertyName;
    }

    public String getSubpropertyName() {
        return subpropertyName;
    }

    public void validate(ExprNodeOrigin origin, ExprValidationContext validationContext) throws ExprValidationException {
        // specific validation by assignor
    }

    public void accept(ExprNodeVisitor visitor) {
        // no action
    }

    public String getFullIdentifier() {
        return ident + "." + subpropertyName;
    }
}
