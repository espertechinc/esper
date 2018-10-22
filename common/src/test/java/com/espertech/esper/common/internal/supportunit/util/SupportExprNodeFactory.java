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
package com.espertech.esper.common.internal.supportunit.util;

import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.funcs.ExprCaseNode;
import com.espertech.esper.common.internal.epl.expression.ops.*;
import com.espertech.esper.common.internal.epl.join.querygraph.*;
import com.espertech.esper.common.internal.support.SupportExprValidationContextFactory;
import com.espertech.esper.common.internal.type.MathArithTypeEnum;

public class SupportExprNodeFactory {
    public static QueryGraphValueEntryHashKeyedForge makeKeyed(String property) {
        return new QueryGraphValueEntryHashKeyedForgeExpr(new ExprIdentNodeImpl(property), false);
    }

    public static QueryGraphValueEntryRangeForge makeRangeLess(String prop) {
        return new QueryGraphValueEntryRangeRelOpForge(QueryGraphRangeEnum.LESS, new ExprIdentNodeImpl(prop), false);
    }

    public static QueryGraphValueEntryRangeInForge makeRangeIn(String start, String end) {
        return new QueryGraphValueEntryRangeInForge(QueryGraphRangeEnum.RANGE_OPEN, new ExprIdentNodeImpl(start), new ExprIdentNodeImpl(end), false);
    }

    public static ExprNode[] makeIdentExprNodes(String... props) {
        ExprNode[] nodes = new ExprNode[props.length];
        for (int i = 0; i < props.length; i++) {
            nodes[i] = new ExprIdentNodeImpl(props[i]);
        }
        return nodes;
    }

    public static ExprNode[] makeConstAndIdentNode(String constant, String property) {
        return new ExprNode[]{new ExprConstantNodeImpl(constant), new ExprIdentNodeImpl(property)};
    }

    public static ExprNode[] makeConstAndConstNode(String constantOne, String constantTwo) {
        return new ExprNode[]{new ExprConstantNodeImpl(constantOne), new ExprConstantNodeImpl(constantTwo)};
    }

    public static ExprNode makeIdentExprNode(String property) {
        return new ExprIdentNodeImpl(property);
    }

    public static ExprNode makeConstExprNode(String constant) {
        return new ExprConstantNodeImpl(constant);
    }

    public static ExprEqualsNode makeEqualsNode() throws Exception {
        ExprEqualsNode topNode = new ExprEqualsNodeImpl(false, false);
        ExprIdentNode i1_1 = new ExprIdentNodeImpl("intPrimitive", "s0");
        ExprIdentNode i1_2 = new ExprIdentNodeImpl("intBoxed", "s1");
        topNode.addChildNode(i1_1);
        topNode.addChildNode(i1_2);

        validate3Stream(topNode);

        return topNode;
    }

    public static ExprInNode makeInSetNode(boolean isNotIn) throws Exception {
        // Build :      s0.intPrimitive in (1, 2)
        ExprInNode inNode = new ExprInNodeImpl(isNotIn);
        inNode.addChildNode(makeIdentNode("intPrimitive", "s0"));
        inNode.addChildNode(new SupportExprNode(1));
        inNode.addChildNode(new SupportExprNode(2));
        validate3Stream(inNode);
        return inNode;
    }

    public static ExprCaseNode makeCaseSyntax1Node() throws Exception {
        // Build (case 1 expression):
        // case when s0.intPrimitive = 1 then "a"
        //      when s0.intPrimitive = 2 then "b"
        //      else "c"
        // end
        ExprCaseNode caseNode = new ExprCaseNode(false);

        ExprNode node = makeEqualsNode("intPrimitive", "s0", 1);
        caseNode.addChildNode(node);
        caseNode.addChildNode(new SupportExprNode("a"));

        node = makeEqualsNode("intPrimitive", "s0", 2);
        caseNode.addChildNode(node);
        caseNode.addChildNode(new SupportExprNode("b"));

        caseNode.addChildNode(new SupportExprNode("c"));

        validate3Stream(caseNode);

        return caseNode;
    }

    public static ExprCaseNode makeCaseSyntax2Node() throws Exception {
        // Build (case 2 expression):
        // case s0.intPrimitive
        //   when 1 then "a"
        //   when 2 then "b"
        //   else "c"
        // end
        ExprCaseNode caseNode = new ExprCaseNode(true);
        caseNode.addChildNode(makeIdentNode("intPrimitive", "s0"));

        caseNode.addChildNode(new SupportExprNode(1));
        caseNode.addChildNode(new SupportExprNode("a"));
        caseNode.addChildNode(new SupportExprNode(2));
        caseNode.addChildNode(new SupportExprNode("b"));
        caseNode.addChildNode(new SupportExprNode("c"));

        validate3Stream(caseNode);

        return (caseNode);
    }

    public static ExprRegexpNode makeRegexpNode(boolean isNot) throws Exception {
        // Build :      s0.string regexp "[a-z][a-z]"  (with not)
        ExprRegexpNode node = new ExprRegexpNode(isNot);
        node.addChildNode(makeIdentNode("theString", "s0"));
        node.addChildNode(new SupportExprNode("[a-z][a-z]"));
        validate3Stream(node);
        return node;
    }

    public static ExprIdentNode makeIdentNode(String fieldName, String streamName) {
        ExprIdentNode node = new ExprIdentNodeImpl(fieldName, streamName);
        validate3Stream(node);
        return node;
    }

    public static ExprLikeNode makeLikeNode(boolean isNot, String optionalEscape) throws Exception {
        // Build :      s0.string like "%abc__"  (with or witout escape)
        ExprLikeNode node = new ExprLikeNode(isNot);
        node.addChildNode(makeIdentNode("theString", "s0"));
        node.addChildNode(new SupportExprNode("%abc__"));
        if (optionalEscape != null) {
            node.addChildNode(new SupportExprNode(optionalEscape));
        }
        validate3Stream(node);
        return node;
    }

    public static ExprNode make2SubNodeAnd() {
        ExprAndNode topNode = new ExprAndNodeImpl();

        ExprEqualsNode e1 = new ExprEqualsNodeImpl(false, false);
        ExprEqualsNode e2 = new ExprEqualsNodeImpl(false, false);

        topNode.addChildNode(e1);
        topNode.addChildNode(e2);

        ExprIdentNode i1_1 = new ExprIdentNodeImpl("intPrimitive", "s0");
        ExprIdentNode i1_2 = new ExprIdentNodeImpl("intBoxed", "s1");
        e1.addChildNode(i1_1);
        e1.addChildNode(i1_2);

        ExprIdentNode i2_1 = new ExprIdentNodeImpl("theString", "s1");
        ExprIdentNode i2_2 = new ExprIdentNodeImpl("theString", "s0");
        e2.addChildNode(i2_1);
        e2.addChildNode(i2_2);

        validate3Stream(topNode);

        return topNode;
    }

    public static ExprNode make3SubNodeAnd() {
        ExprNode topNode = new ExprAndNodeImpl();

        ExprEqualsNode[] equalNodes = new ExprEqualsNode[3];
        for (int i = 0; i < equalNodes.length; i++) {
            equalNodes[i] = new ExprEqualsNodeImpl(false, false);
            topNode.addChildNode(equalNodes[i]);
        }

        ExprIdentNode i1_1 = new ExprIdentNodeImpl("intPrimitive", "s0");
        ExprIdentNode i1_2 = new ExprIdentNodeImpl("intBoxed", "s1");
        equalNodes[0].addChildNode(i1_1);
        equalNodes[0].addChildNode(i1_2);

        ExprIdentNode i2_1 = new ExprIdentNodeImpl("theString", "s1");
        ExprIdentNode i2_2 = new ExprIdentNodeImpl("theString", "s0");
        equalNodes[1].addChildNode(i2_1);
        equalNodes[1].addChildNode(i2_2);

        ExprIdentNode i3_1 = new ExprIdentNodeImpl("boolBoxed", "s0");
        ExprIdentNode i3_2 = new ExprIdentNodeImpl("boolPrimitive", "s1");
        equalNodes[2].addChildNode(i3_1);
        equalNodes[2].addChildNode(i3_2);

        validate3Stream(topNode);

        return topNode;
    }

    public static ExprNode makeMathNode() {
        ExprIdentNode node1 = new ExprIdentNodeImpl("intBoxed", "s0");
        ExprIdentNode node2 = new ExprIdentNodeImpl("intPrimitive", "s0");
        ExprMathNode mathNode = new ExprMathNode(MathArithTypeEnum.MULTIPLY, false, false);
        mathNode.addChildNode(node1);
        mathNode.addChildNode(node2);

        validate3Stream(mathNode);

        return mathNode;
    }

    public static void validate3Stream(ExprNode topNode) {
        SupportStreamTypeSvc3Stream streamTypeService = new SupportStreamTypeSvc3Stream();
        ExprValidationContext validationContext = SupportExprValidationContextFactory.make(streamTypeService);

        try {
            ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.SELECT, topNode, validationContext);
        } catch (ExprValidationException e) {
            throw new RuntimeException(e);
        }
    }

    private static ExprEqualsNode makeEqualsNode(String ident1, String stream1, Object value) {
        ExprEqualsNode topNode = new ExprEqualsNodeImpl(false, false);
        ExprIdentNode i1_1 = new ExprIdentNodeImpl(ident1, stream1);
        SupportExprNode constantNode = new SupportExprNode(value);
        topNode.addChildNode(i1_1);
        topNode.addChildNode(constantNode);
        return topNode;
    }
}
