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

import com.espertech.esper.common.internal.collection.PermutationEnumeration;
import com.espertech.esper.common.internal.supportunit.util.SupportExprNode;
import com.espertech.esper.common.internal.supportunit.util.SupportExprNodeFactory;
import junit.framework.TestCase;

import static com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCompare.deepEqualsIgnoreDupAndOrder;
import static com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCompare.deepEqualsIsSubset;

public class TestExprNodeUtilityCompare extends TestCase {

    private SupportExprNode e1 = new SupportExprNode(1);
    private SupportExprNode e2 = new SupportExprNode(2);
    private SupportExprNode e3 = new SupportExprNode(3);
    private SupportExprNode e4 = new SupportExprNode(4);
    private SupportExprNode e1Dup = new SupportExprNode(1);
    private ExprNode[] empty = new ExprNode[0];
    private ExprNode[] justE1 = new ExprNode[]{e1};

    public void testDeepEquals() throws Exception {
        assertFalse(ExprNodeUtilityCompare.deepEquals(SupportExprNodeFactory.make2SubNodeAnd(), SupportExprNodeFactory.make3SubNodeAnd(), false));
        assertFalse(ExprNodeUtilityCompare.deepEquals(SupportExprNodeFactory.makeEqualsNode(), SupportExprNodeFactory.makeMathNode(), false));
        assertTrue(ExprNodeUtilityCompare.deepEquals(SupportExprNodeFactory.makeMathNode(), SupportExprNodeFactory.makeMathNode(), false));
        assertFalse(ExprNodeUtilityCompare.deepEquals(SupportExprNodeFactory.makeMathNode(), SupportExprNodeFactory.make2SubNodeAnd(), false));
        assertTrue(ExprNodeUtilityCompare.deepEquals(SupportExprNodeFactory.make3SubNodeAnd(), SupportExprNodeFactory.make3SubNodeAnd(), false));
    }

    public void testDeepEqualsIsSubset() {
        assertTrue(deepEqualsIsSubset(empty, empty));
        assertTrue(deepEqualsIsSubset(empty, justE1));
        assertTrue(deepEqualsIsSubset(empty, new ExprNode[]{e1, e2}));

        assertTrue(deepEqualsIsSubset(justE1, new ExprNode[]{e1}));
        assertTrue(deepEqualsIsSubset(justE1, new ExprNode[]{e1, e1}));
        assertFalse(deepEqualsIsSubset(justE1, new ExprNode[]{e2}));

        ExprNode[] e1e2 = new ExprNode[]{e1, e2};
        assertFalse(deepEqualsIsSubset(e1e2, justE1));
        assertFalse(deepEqualsIsSubset(e1e2, new ExprNode[]{e2}));
        assertTrue(deepEqualsIsSubset(e1e2, new ExprNode[]{e2, e1}));
        assertTrue(deepEqualsIsSubset(e1e2, new ExprNode[]{e2, e1, e2, e1}));

        ExprNode[] e1e2e3 = new ExprNode[]{e1, e2, e3};
        assertFalse(deepEqualsIsSubset(e1e2e3, justE1));
        assertFalse(deepEqualsIsSubset(e1e2e3, new ExprNode[]{e2, e3}));
        assertTrue(deepEqualsIsSubset(e1e2e3, new ExprNode[]{e2, e3, e1}));
        assertTrue(deepEqualsIsSubset(e1e2e3, e1e2e3));
    }

    public void testDeepEqualsIgnoreOrder() {

        // compare on set being empty
        comparePermutations(true, empty, empty);
        comparePermutations(false, new ExprNode[]{e1}, empty);

        // compare single
        comparePermutations(true, justE1, justE1);
        comparePermutations(true, justE1, new ExprNode[]{e1Dup});
        comparePermutations(false, justE1, new ExprNode[]{e2});
        comparePermutations(false, new ExprNode[]{e2}, new ExprNode[]{e3});

        // compare two (same number of expressions)
        comparePermutations(true, new ExprNode[]{e1, e2}, new ExprNode[]{e1, e2});
        comparePermutations(true, new ExprNode[]{e1, e2}, new ExprNode[]{e2, e1});
        comparePermutations(false, new ExprNode[]{e3, e2}, new ExprNode[]{e2, e1});
        comparePermutations(false, new ExprNode[]{e1, e2}, new ExprNode[]{e1, e3});

        // compare three (same number of expressions)
        comparePermutations(true, new ExprNode[]{e1, e2, e3}, new ExprNode[]{e1, e2, e3});
        comparePermutations(false, new ExprNode[]{e1, e2, e3}, new ExprNode[]{e1, e2, e4});
        comparePermutations(false, new ExprNode[]{e1, e2, e3}, new ExprNode[]{e1, e4, e3});
        comparePermutations(false, new ExprNode[]{e1, e2, e3}, new ExprNode[]{e4, e2, e3});

        // duplicates allowed and ignored
        comparePermutations(true, new ExprNode[]{e1}, new ExprNode[]{e1, e1});
        comparePermutations(false, new ExprNode[]{e1}, new ExprNode[]{e1, e2});
        comparePermutations(true, new ExprNode[]{e1}, new ExprNode[]{e1, e1, e1});
        comparePermutations(false, new ExprNode[]{e2}, new ExprNode[]{e2, e2, e1});
        comparePermutations(true, new ExprNode[]{e1, e1, e2, e2}, new ExprNode[]{e2, e2, e1});
        comparePermutations(false, new ExprNode[]{e1, e1, e2, e2}, new ExprNode[]{e1, e1, e1});
        comparePermutations(true, new ExprNode[]{e2, e1, e2}, new ExprNode[]{e2, e1});
    }

    private void comparePermutations(boolean expected, ExprNode[] setOne, ExprNode[] setTwo) {
        if (setTwo.length == 0) {
            compareSingle(expected, setOne, setTwo);
            return;
        }
        PermutationEnumeration permuter = new PermutationEnumeration(setTwo.length);
        for (; permuter.hasMoreElements(); ) {
            int[] permutation = permuter.nextElement();
            ExprNode[] copy = new ExprNode[setTwo.length];
            for (int i = 0; i < permutation.length; i++) {
                copy[i] = setTwo[permutation[i]];
            }
            compareSingle(expected, setOne, copy);
        }
    }

    private void compareSingle(boolean expected, ExprNode[] setOne, ExprNode[] setTwo) {
        assertEquals(expected, deepEqualsIgnoreDupAndOrder(setOne, setTwo));
        assertEquals(expected, deepEqualsIgnoreDupAndOrder(setTwo, setOne));
    }
}
