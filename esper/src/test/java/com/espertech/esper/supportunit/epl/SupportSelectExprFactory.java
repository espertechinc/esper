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
package com.espertech.esper.supportunit.epl;

import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.methodagg.ExprSumNode;
import com.espertech.esper.epl.expression.ops.ExprMathNode;
import com.espertech.esper.epl.spec.SelectClauseElementCompiled;
import com.espertech.esper.epl.spec.SelectClauseExprCompiledSpec;
import com.espertech.esper.epl.spec.SelectClauseExprRawSpec;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.type.MathArithTypeEnum;
import com.espertech.esper.support.SupportExprValidationContextFactory;

import java.util.LinkedList;
import java.util.List;

public class SupportSelectExprFactory {
    public static SelectClauseElementCompiled[] makeInvalidSelectList() throws Exception {
        ExprIdentNode node = new ExprIdentNodeImpl("xxxx", "s0");
        return new SelectClauseElementCompiled[]{new SelectClauseExprCompiledSpec(node, null, null, false)};
    }

    public static List<SelectClauseExprCompiledSpec> makeSelectListFromIdent(String propertyName, String streamName) throws Exception {
        List<SelectClauseExprCompiledSpec> selectionList = new LinkedList<SelectClauseExprCompiledSpec>();
        ExprNode identNode = SupportExprNodeFactory.makeIdentNode(propertyName, streamName);
        selectionList.add(new SelectClauseExprCompiledSpec(identNode, "propertyName", null, false));
        return selectionList;
    }

    public static List<SelectClauseExprCompiledSpec> makeNoAggregateSelectList() throws Exception {
        List<SelectClauseExprCompiledSpec> selectionList = new LinkedList<SelectClauseExprCompiledSpec>();
        ExprNode identNode = SupportExprNodeFactory.makeIdentNode("doubleBoxed", "s0");
        ExprNode mathNode = SupportExprNodeFactory.makeMathNode();
        selectionList.add(new SelectClauseExprCompiledSpec(identNode, "resultOne", null, false));
        selectionList.add(new SelectClauseExprCompiledSpec(mathNode, "resultTwo", null, false));
        return selectionList;
    }

    public static SelectClauseElementCompiled[] makeNoAggregateSelectListUnnamed() throws Exception {
        List<SelectClauseElementCompiled> selectionList = new LinkedList<SelectClauseElementCompiled>();
        ExprNode identNode = SupportExprNodeFactory.makeIdentNode("doubleBoxed", "s0");
        ExprNode mathNode = SupportExprNodeFactory.makeMathNode();
        selectionList.add(new SelectClauseExprCompiledSpec(identNode, null, null, false));
        selectionList.add(new SelectClauseExprCompiledSpec(mathNode, "result", null, false));
        return selectionList.toArray(new SelectClauseElementCompiled[selectionList.size()]);
    }

    public static SelectClauseElementCompiled[] makeAggregateSelectListWithProps() throws Exception {
        ExprNode top = new ExprSumNode(false);
        ExprNode identNode = SupportExprNodeFactory.makeIdentNode("doubleBoxed", "s0");
        top.addChildNode(identNode);

        SelectClauseElementCompiled[] selectionList = new SelectClauseElementCompiled[]{
                new SelectClauseExprCompiledSpec(top, null, null, false)};
        return selectionList;
    }

    public static SelectClauseElementCompiled[] makeAggregatePlusNoAggregate() throws Exception {
        ExprNode top = new ExprSumNode(false);
        ExprNode identNode = SupportExprNodeFactory.makeIdentNode("doubleBoxed", "s0");
        top.addChildNode(identNode);

        ExprNode identNode2 = SupportExprNodeFactory.makeIdentNode("doubleBoxed", "s0");

        List<SelectClauseElementCompiled> selectionList = new LinkedList<SelectClauseElementCompiled>();
        selectionList.add(new SelectClauseExprCompiledSpec(top, null, null, false));
        selectionList.add(new SelectClauseExprCompiledSpec(identNode2, null, null, false));
        return selectionList.toArray(new SelectClauseElementCompiled[selectionList.size()]);
    }

    public static SelectClauseElementCompiled[] makeAggregateMixed() throws Exception {
        // make a "select doubleBoxed, sum(intPrimitive)" -equivalent
        List<SelectClauseElementCompiled> selectionList = new LinkedList<SelectClauseElementCompiled>();

        ExprNode identNode = SupportExprNodeFactory.makeIdentNode("doubleBoxed", "s0");
        selectionList.add(new SelectClauseExprCompiledSpec(identNode, null, null, false));

        ExprNode top = new ExprSumNode(false);
        identNode = SupportExprNodeFactory.makeIdentNode("intPrimitive", "s0");
        top.addChildNode(identNode);
        selectionList.add(new SelectClauseExprCompiledSpec(top, null, null, false));

        return selectionList.toArray(new SelectClauseElementCompiled[selectionList.size()]);
    }

    public static List<SelectClauseExprRawSpec> makeAggregateSelectListNoProps() throws Exception {
        /*
                                    top (*)
                  c1 (sum)                            c2 (10)
                  c1_1 (5)
        */

        ExprNode top = new ExprMathNode(MathArithTypeEnum.MULTIPLY, false, false);
        ExprNode c1 = new ExprSumNode(false);
        ExprNode c1_1 = new SupportExprNode(5);
        ExprNode c2 = new SupportExprNode(10);

        top.addChildNode(c1);
        top.addChildNode(c2);
        c1.addChildNode(c1_1);

        ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.SELECT, top, SupportExprValidationContextFactory.makeEmpty());

        List<SelectClauseExprRawSpec> selectionList = new LinkedList<SelectClauseExprRawSpec>();
        selectionList.add(new SelectClauseExprRawSpec(top, null, false));
        return selectionList;
    }
}
