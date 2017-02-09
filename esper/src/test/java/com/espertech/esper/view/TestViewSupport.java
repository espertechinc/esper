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
package com.espertech.esper.view;

import com.espertech.esper.epl.expression.core.ExprConstantNodeImpl;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import com.espertech.esper.supportunit.view.SupportSchemaNeutralView;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class TestViewSupport extends TestCase {
    private SupportSchemaNeutralView top;

    private SupportSchemaNeutralView child_1;
    private SupportSchemaNeutralView child_2;

    private SupportSchemaNeutralView child_2_1;
    private SupportSchemaNeutralView child_2_2;

    private SupportSchemaNeutralView child_2_1_1;
    private SupportSchemaNeutralView child_2_2_1;
    private SupportSchemaNeutralView child_2_2_2;

    public void setUp() {
        top = new SupportSchemaNeutralView("top");

        child_1 = new SupportSchemaNeutralView("1");
        child_2 = new SupportSchemaNeutralView("2");
        top.addView(child_1);
        top.addView(child_2);

        child_2_1 = new SupportSchemaNeutralView("2_1");
        child_2_2 = new SupportSchemaNeutralView("2_2");
        child_2.addView(child_2_1);
        child_2.addView(child_2_2);

        child_2_1_1 = new SupportSchemaNeutralView("2_1_1");
        child_2_2_1 = new SupportSchemaNeutralView("2_2_1");
        child_2_2_2 = new SupportSchemaNeutralView("2_2_2");
        child_2_1.addView(child_2_1_1);
        child_2_2.addView(child_2_2_1);
        child_2_2.addView(child_2_2_2);
    }

    public void testFindDescendent() {
        // Test a deep find
        List<View> descendents = ViewSupport.findDescendent(top, child_2_2_1);
        assertEquals(2, descendents.size());
        assertEquals(child_2, descendents.get(0));
        assertEquals(child_2_2, descendents.get(1));

        descendents = ViewSupport.findDescendent(top, child_2_1_1);
        assertEquals(2, descendents.size());
        assertEquals(child_2, descendents.get(0));
        assertEquals(child_2_1, descendents.get(1));

        descendents = ViewSupport.findDescendent(top, child_2_1);
        assertEquals(1, descendents.size());
        assertEquals(child_2, descendents.get(0));

        // Test a shallow find
        descendents = ViewSupport.findDescendent(top, child_2);
        assertEquals(0, descendents.size());

        // Test a no find
        descendents = ViewSupport.findDescendent(top, new SupportSchemaNeutralView());
        assertEquals(null, descendents);
    }

    public static List<ExprNode> toExprListBean(Object[] constants) throws Exception {
        List<ExprNode> expr = new ArrayList<ExprNode>();
        for (int i = 0; i < constants.length; i++) {
            if (constants[i] instanceof String) {
                expr.add(SupportExprNodeFactory.makeIdentNodeBean(constants[i].toString()));
            } else {
                expr.add(new ExprConstantNodeImpl(constants[i]));
            }
        }
        return expr;
    }

    public static List<ExprNode> toExprListMD(Object[] constants) throws Exception {
        List<ExprNode> expr = new ArrayList<ExprNode>();
        for (int i = 0; i < constants.length; i++) {
            if (constants[i] instanceof String) {
                expr.add(SupportExprNodeFactory.makeIdentNodeMD(constants[i].toString()));
            } else {
                expr.add(new ExprConstantNodeImpl(constants[i]));
            }
        }
        return expr;
    }

    public static List<ExprNode> toExprList(Object constant) throws Exception {
        return toExprListBean(new Object[]{constant});
    }
}
