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
package com.espertech.esper.common.internal.epl.expression.funcs;

import com.espertech.esper.common.internal.epl.expression.core.*;

import java.io.StringWriter;

public class ExprEventIdentityEqualsNode extends ExprNodeBase {
    public final static String NAME = "event_identity_equals";

    private ExprEventIdentityEqualsNodeForge forge;

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length != 2) {
            throw new ExprValidationException(NAME + "requires two parameters");
        }
        ExprStreamUnderlyingNode undOne = checkStreamUnd(this.getChildNodes()[0]);
        ExprStreamUnderlyingNode undTwo = checkStreamUnd(this.getChildNodes()[1]);
        if (undOne.getEventType() != undTwo.getEventType()) {
            throw new ExprValidationException(NAME + " received two different event types as parameter, type '" + undOne.getEventType().getName() + "' is not the same as type '" + undTwo.getEventType().getName() + "'");
        }
        forge = new ExprEventIdentityEqualsNodeForge(this, undOne, undTwo);
        return null;
    }

    private ExprStreamUnderlyingNode checkStreamUnd(ExprNode childNode) throws ExprValidationException {
        if (!(childNode instanceof ExprStreamUnderlyingNode)) {
            throw new ExprValidationException(NAME + " requires a parameter that resolves to an event but received '" + ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(childNode) + "'");
        }
        return (ExprStreamUnderlyingNode) childNode;
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return forge;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        return node instanceof ExprEventIdentityEqualsNode;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append(NAME);
        ExprNodeUtilityPrint.toExpressionStringParams(writer, this.getChildNodes());
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }
}
