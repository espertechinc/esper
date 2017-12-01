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
package com.espertech.esper.epl.expression.core;

public class ExprNodePropOrStreamExprDesc implements ExprNodePropOrStreamDesc {

    private final int streamNum;
    private final ExprNode originator;

    public ExprNodePropOrStreamExprDesc(int streamNum, ExprNode originator) {
        this.streamNum = streamNum;
        this.originator = originator;
    }

    public int getStreamNum() {
        return streamNum;
    }

    public ExprNode getOriginator() {
        return originator;
    }

    public String getTextual() {
        return "expression '" + ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(originator) + "' against stream " + streamNum;
    }
}
