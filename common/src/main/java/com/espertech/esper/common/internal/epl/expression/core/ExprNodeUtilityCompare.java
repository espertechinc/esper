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

import java.util.List;

public class ExprNodeUtilityCompare {
    public static boolean deepEqualsIsSubset(ExprNode[] subset, ExprNode[] superset) {
        for (ExprNode subsetNode : subset) {
            boolean found = false;
            for (ExprNode supersetNode : superset) {
                if (deepEquals(subsetNode, supersetNode, false)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    public static boolean deepEqualsIgnoreDupAndOrder(ExprNode[] setOne, ExprNode[] setTwo) {
        if ((setOne.length == 0 && setTwo.length != 0) || (setOne.length != 0 && setTwo.length == 0)) {
            return false;
        }

        // find set-one expressions in set two
        boolean[] foundTwo = new boolean[setTwo.length];
        for (ExprNode one : setOne) {
            boolean found = false;
            for (int i = 0; i < setTwo.length; i++) {
                if (deepEquals(one, setTwo[i], false)) {
                    found = true;
                    foundTwo[i] = true;
                }
            }
            if (!found) {
                return false;
            }
        }

        // find any remaining set-two expressions in set one
        for (int i = 0; i < foundTwo.length; i++) {
            if (foundTwo[i]) {
                continue;
            }
            for (ExprNode one : setOne) {
                if (deepEquals(one, setTwo[i], false)) {
                    break;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Compare two expression nodes and their children in exact child-node sequence,
     * returning true if the 2 expression nodes trees are equals, or false if they are not equals.
     * <p>
     * Recursive call since it uses this method to compare child nodes in the same exact sequence.
     * Nodes are compared using the equalsNode method.
     *
     * @param nodeOne            - first expression top node of the tree to compare
     * @param nodeTwo            - second expression top node of the tree to compare
     * @param ignoreStreamPrefix when the equals-comparison can ignore prefix of event properties
     * @return false if this or all child nodes are not equal, true if equal
     */
    public static boolean deepEquals(ExprNode nodeOne, ExprNode nodeTwo, boolean ignoreStreamPrefix) {
        if (nodeOne.getChildNodes().length != nodeTwo.getChildNodes().length) {
            return false;
        }
        if (!nodeOne.equalsNode(nodeTwo, ignoreStreamPrefix)) {
            return false;
        }
        for (int i = 0; i < nodeOne.getChildNodes().length; i++) {
            ExprNode childNodeOne = nodeOne.getChildNodes()[i];
            ExprNode childNodeTwo = nodeTwo.getChildNodes()[i];

            if (!deepEquals(childNodeOne, childNodeTwo, ignoreStreamPrefix)) {
                return false;
            }
        }
        return true;
    }

    public static boolean deepEqualsNullChecked(ExprNode nodeOne, ExprNode nodeTwo, boolean ignoreStreamPrefix) {
        if (nodeOne == null) {
            return nodeTwo == null;
        }
        return nodeTwo != null && deepEquals(nodeOne, nodeTwo, ignoreStreamPrefix);
    }

    /**
     * Compares two expression nodes via deep comparison, considering all
     * child nodes of either side.
     *
     * @param one                array of expressions
     * @param two                array of expressions
     * @param ignoreStreamPrefix indicator whether we ignore stream prefixes and instead use resolved property name
     * @return true if the expressions are equal, false if not
     */
    public static boolean deepEquals(ExprNode[] one, ExprNode[] two, boolean ignoreStreamPrefix) {
        if (one.length != two.length) {
            return false;
        }
        for (int i = 0; i < one.length; i++) {
            if (!deepEquals(one[i], two[i], ignoreStreamPrefix)) {
                return false;
            }
        }
        return true;
    }

    public static boolean deepEquals(List<ExprNode> one, List<ExprNode> two) {
        if (one.size() != two.size()) {
            return false;
        }
        for (int i = 0; i < one.size(); i++) {
            if (!deepEquals(one.get(i), two.get(i), false)) {
                return false;
            }
        }
        return true;
    }
}
