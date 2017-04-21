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
package com.espertech.esper.epl.expression.dot;

import com.espertech.esper.epl.core.EngineImportApplicationDotMethod;
import com.espertech.esper.epl.datetime.eval.ExprDotNodeFilterAnalyzerDesc;
import com.espertech.esper.epl.expression.core.*;

import java.io.StringWriter;

public class ExprAppDotMethod extends ExprNodeBase implements ExprQueryFilterAnalyzerNode {

    private final EngineImportApplicationDotMethod desc;

    public ExprAppDotMethod(EngineImportApplicationDotMethod desc) {
        this.desc = desc;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        desc.validate(validationContext);
        return null;
    }

    public EngineImportApplicationDotMethod getDesc() {
        return desc;
    }

    public ExprEvaluator getExprEvaluator() {
        return desc.getExprEvaluator();
    }

    public ExprDotNodeFilterAnalyzerDesc getExprDotNodeFilterAnalyzerDesc(boolean isOuterJoin) {
        return isOuterJoin ? null : desc.getExprDotNodeFilterAnalyzerDesc();
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append(desc.getLhsName());
        writer.append("(");
        ExprNodeUtility.toExpressionStringMinPrecedenceAsList(desc.getLhs(), writer);
        writer.append(").");
        writer.append(desc.getDotMethodName());
        writer.append("(");
        writer.append(desc.getRhsName());
        writer.append("(");
        ExprNodeUtility.toExpressionStringMinPrecedenceAsList(desc.getRhs(), writer);
        writer.append("))");
    }

    public boolean isConstantResult() {
        return false;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprAppDotMethod)) {
            return false;
        }
        ExprAppDotMethod other = (ExprAppDotMethod) node;
        if (!desc.getLhsName().equals(other.getDesc().getLhsName())) return false;
        if (!desc.getDotMethodName().equals(other.getDesc().getDotMethodName())) return false;
        if (!desc.getRhsName().equals(other.getDesc().getRhsName())) return false;
        if (!ExprNodeUtility.deepEquals(desc.getLhs(), other.getDesc().getLhs(), false)) return false;
        return ExprNodeUtility.deepEquals(desc.getRhs(), other.getDesc().getRhs(), false);
    }
}
