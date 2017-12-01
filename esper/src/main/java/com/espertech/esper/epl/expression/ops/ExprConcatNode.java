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

import com.espertech.esper.client.ConfigurationEngineDefaults;
import com.espertech.esper.epl.expression.core.*;

import java.io.StringWriter;

/**
 * Represents a string concatenation.
 */
public class ExprConcatNode extends ExprNodeBase {
    private static final long serialVersionUID = 5811427566733004327L;

    private transient ExprConcatNodeForge forge;

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return forge;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length < 2) {
            throw new ExprValidationException("Concat node must have at least 2 parameters");
        }

        for (int i = 0; i < getChildNodes().length; i++) {
            Class childType = getChildNodes()[i].getForge().getEvaluationType();
            String childTypeName = childType == null ? "null" : childType.getSimpleName();
            if (childType != String.class) {
                throw new ExprValidationException("Implicit conversion from datatype '" +
                        childTypeName +
                        "' to string is not allowed");
            }
        }

        ConfigurationEngineDefaults.ThreadingProfile threadingProfile = validationContext.getEngineImportService().getThreadingProfile();
        forge = new ExprConcatNodeForge(this, threadingProfile);
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        String delimiter = "";
        for (ExprNode child : this.getChildNodes()) {
            writer.append(delimiter);
            child.toEPL(writer, getPrecedence());
            delimiter = "||";
        }
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.CONCAT;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprConcatNode)) {
            return false;
        }

        return true;
    }
}
