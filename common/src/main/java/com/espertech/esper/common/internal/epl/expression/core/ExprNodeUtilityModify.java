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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.internal.epl.expression.chain.Chainable;

import java.util.List;

public class ExprNodeUtilityModify {
    public static void replaceChildNode(ExprNode parentNode, ExprNode nodeToReplace, ExprNode newNode) {
        int index = findChildNode(parentNode, nodeToReplace);
        if (index == -1) {
            parentNode.replaceUnlistedChildNode(nodeToReplace, newNode);
        } else {
            parentNode.setChildNode(index, newNode);
        }
    }

    private static int findChildNode(ExprNode parentNode, ExprNode childNode) {
        for (int i = 0; i < parentNode.getChildNodes().length; i++) {
            if (parentNode.getChildNodes()[i] == childNode) {
                return i;
            }
        }
        return -1;
    }

    public static void replaceChainChildNode(ExprNode nodeToReplace, ExprNode newNode, List<Chainable> chainSpec) {
        for (Chainable chained : chainSpec) {
            int index = chained.getParametersOrEmpty().indexOf(nodeToReplace);
            if (index != -1) {
                chained.getParametersOrEmpty().set(index, newNode);
            }
        }
    }
}
