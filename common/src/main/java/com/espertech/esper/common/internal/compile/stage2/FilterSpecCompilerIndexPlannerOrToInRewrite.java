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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCompare;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.ops.ExprEqualsNode;
import com.espertech.esper.common.internal.epl.expression.ops.ExprInNodeImpl;
import com.espertech.esper.common.internal.epl.expression.ops.ExprOrNode;

public class FilterSpecCompilerIndexPlannerOrToInRewrite {
    public static ExprNode rewriteOrToInIfApplicable(ExprNode constituent, boolean rewriteRegardlessOfLookupable) {
        if (!(constituent instanceof ExprOrNode) || constituent.getChildNodes().length < 2) {
            return constituent;
        }

        // check eligibility
        ExprNode[] childNodes = constituent.getChildNodes();
        for (ExprNode child : childNodes) {
            if (!(child instanceof ExprEqualsNode)) {
                return constituent;
            }
            ExprEqualsNode equalsNode = (ExprEqualsNode) child;
            if (equalsNode.isIs() || equalsNode.isNotEquals()) {
                return constituent;
            }
        }

        // find common-expression node
        ExprNode commonExpressionNode;
        ExprNode lhs = childNodes[0].getChildNodes()[0];
        ExprNode rhs = childNodes[0].getChildNodes()[1];
        if (ExprNodeUtilityCompare.deepEquals(lhs, rhs, false)) {
            return constituent;
        }
        if (isExprExistsInAllEqualsChildNodes(childNodes, lhs)) {
            commonExpressionNode = lhs;
        } else if (isExprExistsInAllEqualsChildNodes(childNodes, rhs)) {
            commonExpressionNode = rhs;
        } else {
            return constituent;
        }

        // if the common expression doesn't reference an event property, no need to rewrite
        if (!rewriteRegardlessOfLookupable) {
            FilterSpecExprNodeVisitorLookupableLimitedExpr lookupableVisitor = new FilterSpecExprNodeVisitorLookupableLimitedExpr();
            commonExpressionNode.accept(lookupableVisitor);
            if (!lookupableVisitor.isHasStreamZeroReference() || !lookupableVisitor.isLimited()) {
                return constituent;
            }
        }

        // build node
        ExprInNodeImpl in = new ExprInNodeImpl(false);
        in.addChildNode(commonExpressionNode);
        for (int i = 0; i < constituent.getChildNodes().length; i++) {
            ExprNode child = constituent.getChildNodes()[i];
            int nodeindex = ExprNodeUtilityCompare.deepEquals(commonExpressionNode, childNodes[i].getChildNodes()[0], false) ? 1 : 0;
            in.addChildNode(child.getChildNodes()[nodeindex]);
        }

        // validate
        try {
            in.validateWithoutContext();
        } catch (ExprValidationException ex) {
            return constituent;
        }

        return in;
    }

    private static boolean isExprExistsInAllEqualsChildNodes(ExprNode[] childNodes, ExprNode search) {
        for (ExprNode child : childNodes) {
            ExprNode lhs = child.getChildNodes()[0];
            ExprNode rhs = child.getChildNodes()[1];
            if (!ExprNodeUtilityCompare.deepEquals(lhs, search, false) && !ExprNodeUtilityCompare.deepEquals(rhs, search, false)) {
                return false;
            }
            if (ExprNodeUtilityCompare.deepEquals(lhs, rhs, false)) {
                return false;
            }
        }
        return true;
    }
}
