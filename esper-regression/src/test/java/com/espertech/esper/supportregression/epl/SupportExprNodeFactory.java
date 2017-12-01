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
package com.espertech.esper.supportregression.epl;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.start.EPStatementStartMethod;
import com.espertech.esper.core.support.SupportEngineImportServiceFactory;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.core.viewres.ViewResourceDelegateUnverified;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.funcs.ExprCaseNode;
import com.espertech.esper.epl.expression.methodagg.ExprCountNode;
import com.espertech.esper.epl.expression.methodagg.ExprSumNode;
import com.espertech.esper.epl.expression.ops.*;
import com.espertech.esper.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.epl.expression.prev.ExprPreviousNodePreviousType;
import com.espertech.esper.epl.expression.prior.ExprPriorNode;
import com.espertech.esper.epl.join.plan.*;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.epl.variable.VariableServiceImpl;
import com.espertech.esper.schedule.SchedulingServiceImpl;
import com.espertech.esper.support.SupportExprEvaluatorContext;
import com.espertech.esper.support.SupportExprValidationContextFactory;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.event.SupportEventTypeFactory;
import com.espertech.esper.timer.TimeSourceServiceImpl;
import com.espertech.esper.type.MathArithTypeEnum;
import com.espertech.esper.type.RelationalOpEnum;
import com.espertech.esper.view.ViewFactory;
import com.espertech.esper.view.ViewFactoryChain;
import com.espertech.esper.view.window.LengthWindowViewFactory;

import java.util.LinkedList;
import java.util.List;

public class SupportExprNodeFactory {
    public static QueryGraphValueEntryHashKeyed makeKeyed(String property) {
        return new QueryGraphValueEntryHashKeyedExpr(new ExprIdentNodeImpl(property), false);
    }

    public static QueryGraphValueEntryRange makeRangeLess(String prop) {
        return new QueryGraphValueEntryRangeRelOp(QueryGraphRangeEnum.LESS, new ExprIdentNodeImpl(prop), false);
    }

    public static QueryGraphValueEntryRange makeRangeIn(String start, String end) {
        return new QueryGraphValueEntryRangeIn(QueryGraphRangeEnum.RANGE_OPEN, new ExprIdentNodeImpl(start), new ExprIdentNodeImpl(end), false);
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

    public static ExprNode[] makeIdentNodesBean(String... names) throws Exception {
        ExprNode[] nodes = new ExprNode[names.length];
        for (int i = 0; i < names.length; i++) {
            nodes[i] = new ExprIdentNodeImpl(names[i]);
            validate1StreamBean(nodes[i]);
        }
        return nodes;
    }

    public static ExprNode[] makeIdentNodesMD(String... names) throws Exception {
        ExprNode[] nodes = new ExprNode[names.length];
        for (int i = 0; i < names.length; i++) {
            nodes[i] = new ExprIdentNodeImpl(names[i]);
            validate1StreamMD(nodes[i]);
        }
        return nodes;
    }

    public static ExprNode makeIdentNodeBean(String names) throws Exception {
        ExprNode node = new ExprIdentNodeImpl(names);
        validate1StreamBean(node);
        return node;
    }

    public static ExprNode makeIdentNodeMD(String names) throws Exception {
        ExprNode node = new ExprIdentNodeImpl(names);
        validate1StreamMD(node);
        return node;
    }

    public static ExprNode makeIdentNodeNoValid(String names) throws Exception {
        return new ExprIdentNodeImpl(names);
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

    public static ExprPreviousNode makePreviousNode() throws Exception {
        ExprPreviousNode prevNode = new ExprPreviousNode(ExprPreviousNodePreviousType.PREV);
        ExprNode indexNode = new ExprIdentNodeImpl("intPrimitive", "s1");
        prevNode.addChildNode(indexNode);
        ExprNode propNode = new ExprIdentNodeImpl("doublePrimitive", "s1");
        prevNode.addChildNode(propNode);

        validate3Stream(prevNode);

        return prevNode;
    }

    public static ExprPriorNode makePriorNode() throws Exception {
        ExprPriorNode priorNode = new ExprPriorNode();
        ExprNode indexNode = new ExprConstantNodeImpl(1);
        priorNode.addChildNode(indexNode);
        ExprNode propNode = new ExprIdentNodeImpl("doublePrimitive", "s0");
        priorNode.addChildNode(propNode);

        validate3Stream(priorNode);

        return priorNode;
    }

    public static ExprAndNode make2SubNodeAnd() throws Exception {
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

    public static ExprNode make3SubNodeAnd() throws Exception {
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

    public static ExprNode makeIdentNode(String fieldName, String streamName) throws Exception {
        ExprIdentNode node = new ExprIdentNodeImpl(fieldName, streamName);
        validate3Stream(node);
        return node;
    }

    public static ExprNode makeMathNode() throws Exception {
        ExprIdentNode node1 = new ExprIdentNodeImpl("intBoxed", "s0");
        ExprIdentNode node2 = new ExprIdentNodeImpl("intPrimitive", "s0");
        ExprMathNode mathNode = new ExprMathNode(MathArithTypeEnum.MULTIPLY, false, false);
        mathNode.addChildNode(node1);
        mathNode.addChildNode(node2);

        validate3Stream(mathNode);

        return mathNode;
    }

    public static ExprNode makeMathNode(MathArithTypeEnum operator_, Object valueLeft_, Object valueRight_) throws Exception {
        ExprMathNode mathNode = new ExprMathNode(operator_, false, false);
        mathNode.addChildNode(new SupportExprNode(valueLeft_));
        mathNode.addChildNode(new SupportExprNode(valueRight_));
        validate3Stream(mathNode);
        return mathNode;
    }

    public static ExprNode makeSumAndFactorNode() throws Exception {
        // sum node
        ExprSumNode sum = new ExprSumNode(false);
        ExprIdentNode ident = new ExprIdentNodeImpl("intPrimitive", "s0");
        sum.addChildNode(ident);

        ExprIdentNode node = new ExprIdentNodeImpl("intBoxed", "s0");
        ExprMathNode mathNode = new ExprMathNode(MathArithTypeEnum.MULTIPLY, false, false);
        mathNode.addChildNode(node);
        mathNode.addChildNode(sum);

        validate3Stream(mathNode);

        return mathNode;
    }

    public static ExprAggregateNode makeSumAggregateNode() throws Exception {
        ExprSumNode top = new ExprSumNode(false);
        ExprIdentNode ident = new ExprIdentNodeImpl("intPrimitive", "s0");
        top.addChildNode(ident);

        validate3Stream(top);

        return top;
    }

    public static ExprNode makeCountNode(Object value, Class type) throws Exception {
        ExprCountNode countNode = new ExprCountNode(false);
        countNode.addChildNode(new SupportExprNode(value, type));
        SupportAggregationResultFuture future = new SupportAggregationResultFuture(new Object[]{10, 20});
        countNode.setAggregationResultFuture(future, 1);
        validate3Stream(countNode);
        return countNode;
    }

    public static ExprNode makeRelationalOpNode(RelationalOpEnum operator_, Object valueLeft_, Class typeLeft_, Object valueRight_, Class typeRight_) throws Exception {
        ExprRelationalOpNode opNode = new ExprRelationalOpNodeImpl(operator_);
        opNode.addChildNode(new SupportExprNode(valueLeft_, typeLeft_));
        opNode.addChildNode(new SupportExprNode(valueRight_, typeRight_));
        validate3Stream(opNode);
        return opNode;
    }

    public static ExprNode makeRelationalOpNode(RelationalOpEnum operator_, Class typeLeft_, Class typeRight_) throws Exception {
        ExprRelationalOpNode opNode = new ExprRelationalOpNodeImpl(operator_);
        opNode.addChildNode(new SupportExprNode(typeLeft_));
        opNode.addChildNode(new SupportExprNode(typeRight_));
        validate3Stream(opNode);
        return opNode;
    }

    public static ExprNode makeRelationalOpNode(RelationalOpEnum operator_, ExprNode nodeLeft_, ExprNode nodeRight_) throws Exception {
        ExprRelationalOpNode opNode = new ExprRelationalOpNodeImpl(operator_);
        opNode.addChildNode(nodeLeft_);
        opNode.addChildNode(nodeRight_);
        validate3Stream(opNode);
        return opNode;
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

    public static ExprRegexpNode makeRegexpNode(boolean isNot) throws Exception {
        // Build :      s0.string regexp "[a-z][a-z]"  (with not)
        ExprRegexpNode node = new ExprRegexpNode(isNot);
        node.addChildNode(makeIdentNode("theString", "s0"));
        node.addChildNode(new SupportExprNode("[a-z][a-z]"));
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

    private static ExprEqualsNode makeEqualsNode(String ident1, String stream1, Object value) throws Exception {
        ExprEqualsNode topNode = new ExprEqualsNodeImpl(false, false);
        ExprIdentNode i1_1 = new ExprIdentNodeImpl(ident1, stream1);
        SupportExprNode constantNode = new SupportExprNode(value);
        topNode.addChildNode(i1_1);
        topNode.addChildNode(constantNode);
        return topNode;
    }

    public static void validate3Stream(ExprNode topNode) throws Exception {
        SupportStreamTypeSvc3Stream streamTypeService = new SupportStreamTypeSvc3Stream();

        ViewFactoryChain[] factoriesPerStream = new ViewFactoryChain[3];
        for (int i = 0; i < factoriesPerStream.length; i++) {
            List<ViewFactory> factories = new LinkedList<ViewFactory>();
            factories.add(new LengthWindowViewFactory());
            factoriesPerStream[i] = new ViewFactoryChain(streamTypeService.getEventTypes()[i], factories);
        }
        ViewResourceDelegateUnverified viewResources = new ViewResourceDelegateUnverified();

        VariableService variableService = new VariableServiceImpl(0, new SchedulingServiceImpl(new TimeSourceServiceImpl()), SupportEventAdapterService.getService(), null);
        variableService.createNewVariable(null, "intPrimitive", Integer.class.getName(), false, false, false, 10, SupportEngineImportServiceFactory.make());
        variableService.allocateVariableState("intPrimitive", EPStatementStartMethod.DEFAULT_AGENT_INSTANCE_ID, null, false);
        variableService.createNewVariable(null, "var1", String.class.getName(), false, false, false, "my_variable_value", SupportEngineImportServiceFactory.make());
        variableService.allocateVariableState("var1", EPStatementStartMethod.DEFAULT_AGENT_INSTANCE_ID, null, false);

        ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.SELECT, topNode, new ExprValidationContext(streamTypeService, SupportEngineImportServiceFactory.make(), null, viewResources, null, variableService, null, new SupportExprEvaluatorContext(null), null, null, 1, null, null, false, false, false, false, null, false));
    }

    public static void validate1StreamBean(ExprNode topNode) throws Exception {
        EventType eventType = SupportEventTypeFactory.createBeanType(SupportBean.class);
        StreamTypeService streamTypeService = new StreamTypeServiceImpl(eventType, "s0", false, "uri");
        ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.SELECT, topNode, SupportExprValidationContextFactory.make(streamTypeService));
    }

    public static void validate1StreamMD(ExprNode topNode) throws Exception {
        EventType eventType = SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class);
        StreamTypeService streamTypeService = new StreamTypeServiceImpl(eventType, "s0", false, "uri");
        ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.SELECT, topNode, SupportExprValidationContextFactory.make(streamTypeService));
    }
}
