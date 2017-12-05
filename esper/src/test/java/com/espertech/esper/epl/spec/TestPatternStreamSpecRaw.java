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
package com.espertech.esper.epl.spec;

import com.espertech.esper.core.service.speccompiled.StreamSpecCompiler;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.parse.EPLTreeWalkerListener;
import com.espertech.esper.filter.FilterForEvalConstantAnyType;
import com.espertech.esper.filter.FilterForEvalConstantDouble;
import com.espertech.esper.filter.FilterForEvalEventPropDouble;
import com.espertech.esper.filter.FilterForEvalEventPropMayCoerce;
import com.espertech.esper.filterspec.*;
import com.espertech.esper.pattern.EvalFilterFactoryNode;
import com.espertech.esper.pattern.EvalNodeAnalysisResult;
import com.espertech.esper.pattern.EvalNodeUtil;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.epl.parse.SupportParserHelper;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class TestPatternStreamSpecRaw extends TestCase {
    public void testPatternEquals() throws Exception {
        String text = "select * from pattern [" +
                "s=" + SupportBean.class.getName() + "(intPrimitive=5) -> " +
                "t=" + SupportBean.class.getName() + "(intPrimitive=s.intBoxed)" +
                "]";
        tryPatternEquals(text);

        text = "select * from pattern [" +
                "s=" + SupportBean.class.getName() + "(5=intPrimitive) -> " +
                "t=" + SupportBean.class.getName() + "(s.intBoxed=intPrimitive)" +
                "]";
        tryPatternEquals(text);
    }

    public void testInvalid() throws Exception {
        String text = "select * from pattern [" +
                "s=" + SupportBean.class.getName() + " -> " +
                "t=" + SupportBean.class.getName() + "(intPrimitive=s.doubleBoxed)" +
                "]";
        tryInvalid(text);

        text = "select * from pattern [" +
                "s=" + SupportBean.class.getName() + " -> " +
                "t=" + SupportBean.class.getName() + "(intPrimitive in (s.doubleBoxed))" +
                "]";
        tryInvalid(text);
    }

    private void tryInvalid(String text) throws Exception {
        try {
            PatternStreamSpecRaw raw = makeSpec(text);
            compile(raw);
            fail();
        } catch (ExprValidationException ex) {
            // expected
        }
    }

    public void testPatternExpressions() throws Exception {
        String text = "select * from pattern [" +
                "s=" + SupportBean.class.getName() + "(intPrimitive in (s.intBoxed + 1, 0), intBoxed+1=intPrimitive-1)" +
                "]";

        PatternStreamSpecRaw raw = makeSpec(text);
        PatternStreamSpecCompiled spec = compile(raw);
        assertEquals(1, spec.getTaggedEventTypes().size());
        assertEquals(SupportBean.class, spec.getTaggedEventTypes().get("s").getFirst().getUnderlyingType());

        EvalNodeAnalysisResult evalNodeAnalysisResult = EvalNodeUtil.recursiveAnalyzeChildNodes(spec.getEvalFactoryNode());
        List<EvalFilterFactoryNode> filters = evalNodeAnalysisResult.getFilterNodes();
        assertEquals(1, filters.size());

        // node 0
        EvalFilterFactoryNode filterNode = filters.get(0);
        assertEquals(SupportBean.class, filterNode.getFilterSpec().getFilterForEventType().getUnderlyingType());
        assertEquals(1, filterNode.getFilterSpec().getParameters().length);
        FilterSpecParamExprNode exprParam = (FilterSpecParamExprNode) filterNode.getFilterSpec().getParameters()[0][0];
    }

    public void testPatternInSetOfVal() throws Exception {
        String text = "select * from pattern [" +
                "s=" + SupportBean.class.getName() + " -> " +
                SupportBean.class.getName() + "(intPrimitive in (s.intBoxed, 0))" +
                "]";

        PatternStreamSpecRaw raw = makeSpec(text);
        PatternStreamSpecCompiled spec = compile(raw);
        assertEquals(1, spec.getTaggedEventTypes().size());
        assertEquals(SupportBean.class, spec.getTaggedEventTypes().get("s").getFirst().getUnderlyingType());

        EvalNodeAnalysisResult evalNodeAnalysisResult = EvalNodeUtil.recursiveAnalyzeChildNodes(spec.getEvalFactoryNode());
        List<EvalFilterFactoryNode> filters = evalNodeAnalysisResult.getFilterNodes();
        assertEquals(2, filters.size());

        // node 0
        EvalFilterFactoryNode filterNode = filters.get(0);
        assertEquals(SupportBean.class, filterNode.getFilterSpec().getFilterForEventType().getUnderlyingType());
        assertEquals(0, filterNode.getFilterSpec().getParameters().length);

        // node 1
        filterNode = filters.get(1);
        assertEquals(SupportBean.class, filterNode.getFilterSpec().getFilterForEventType().getUnderlyingType());
        assertEquals(1, filterNode.getFilterSpec().getParameters()[0].length);

        FilterSpecParamIn inlist = (FilterSpecParamIn) filterNode.getFilterSpec().getParameters()[0][0];
        assertEquals(FilterOperator.IN_LIST_OF_VALUES, inlist.getFilterOperator());
        assertEquals(2, inlist.getListOfValues().size());

        // in-value 1
        FilterForEvalEventPropMayCoerce prop = (FilterForEvalEventPropMayCoerce) inlist.getListOfValues().get(0);
        assertEquals("s", prop.getResultEventAsName());
        assertEquals("intBoxed", prop.getResultEventProperty());

        // in-value 1
        FilterForEvalConstantAnyType constant = (FilterForEvalConstantAnyType) inlist.getListOfValues().get(1);
        assertEquals(0, constant.getConstant());
    }

    public void testRange() throws Exception {
        String text = "select * from pattern [" +
                "s=" + SupportBean.class.getName() + " -> " +
                SupportBean.class.getName() + "(intPrimitive between s.intBoxed and 100)" +
                "]";

        PatternStreamSpecRaw raw = makeSpec(text);
        PatternStreamSpecCompiled spec = compile(raw);
        assertEquals(1, spec.getTaggedEventTypes().size());
        assertEquals(SupportBean.class, spec.getTaggedEventTypes().get("s").getFirst().getUnderlyingType());

        EvalNodeAnalysisResult evalNodeAnalysisResult = EvalNodeUtil.recursiveAnalyzeChildNodes(spec.getEvalFactoryNode());
        List<EvalFilterFactoryNode> filters = evalNodeAnalysisResult.getFilterNodes();
        assertEquals(2, filters.size());

        // node 0
        EvalFilterFactoryNode filterNode = filters.get(0);
        assertEquals(SupportBean.class, filterNode.getFilterSpec().getFilterForEventType().getUnderlyingType());
        assertEquals(0, filterNode.getFilterSpec().getParameters().length);

        // node 1
        filterNode = filters.get(1);
        assertEquals(SupportBean.class, filterNode.getFilterSpec().getFilterForEventType().getUnderlyingType());
        assertEquals(1, filterNode.getFilterSpec().getParameters().length);

        FilterSpecParamRange range = (FilterSpecParamRange) filterNode.getFilterSpec().getParameters()[0][0];
        assertEquals(FilterOperator.RANGE_CLOSED, range.getFilterOperator());

        // min-value
        FilterForEvalEventPropDouble prop = (FilterForEvalEventPropDouble) range.getMin();
        assertEquals("s", prop.getResultEventAsName());
        assertEquals("intBoxed", prop.getResultEventProperty());

        // max-value
        FilterForEvalConstantDouble constant = (FilterForEvalConstantDouble) range.getMax();
        assertEquals(100d, constant.getDoubleValue());
    }

    private void tryPatternEquals(String text) throws Exception {
        PatternStreamSpecRaw raw = makeSpec(text);
        PatternStreamSpecCompiled spec = compile(raw);
        assertEquals(2, spec.getTaggedEventTypes().size());
        assertEquals(SupportBean.class, spec.getTaggedEventTypes().get("s").getFirst().getUnderlyingType());
        assertEquals(SupportBean.class, spec.getTaggedEventTypes().get("t").getFirst().getUnderlyingType());

        EvalNodeAnalysisResult evalNodeAnalysisResult = EvalNodeUtil.recursiveAnalyzeChildNodes(spec.getEvalFactoryNode());
        List<EvalFilterFactoryNode> filters = evalNodeAnalysisResult.getFilterNodes();
        assertEquals(2, filters.size());

        // node 0
        EvalFilterFactoryNode filterNode = filters.get(0);
        assertEquals(SupportBean.class, filterNode.getFilterSpec().getFilterForEventType().getUnderlyingType());
        assertEquals(1, filterNode.getFilterSpec().getParameters().length);

        FilterSpecParamConstant constant = (FilterSpecParamConstant) filterNode.getFilterSpec().getParameters()[0][0];
        assertEquals(FilterOperator.EQUAL, constant.getFilterOperator());
        assertEquals("intPrimitive", constant.getLookupable().getExpression());
        assertEquals(5, constant.getFilterConstant());

        // node 1
        filterNode = filters.get(1);
        assertEquals(SupportBean.class, filterNode.getFilterSpec().getFilterForEventType().getUnderlyingType());
        assertEquals(1, filterNode.getFilterSpec().getParameters().length);

        FilterSpecParamEventProp eventprop = (FilterSpecParamEventProp) filterNode.getFilterSpec().getParameters()[0][0];
        assertEquals(FilterOperator.EQUAL, constant.getFilterOperator());
        assertEquals("intPrimitive", constant.getLookupable().getExpression());
        assertEquals("s", eventprop.getResultEventAsName());
        assertEquals("intBoxed", eventprop.getResultEventProperty());
    }

    private PatternStreamSpecCompiled compile(PatternStreamSpecRaw raw) throws Exception {
        return StreamSpecCompiler.compile(raw, SupportStatementContextFactory.makeContext(), new HashSet<String>(), false, Collections.<Integer>emptyList(), false, false, false, null);
    }

    private static PatternStreamSpecRaw makeSpec(String expression) throws Exception {
        EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
        return (PatternStreamSpecRaw) walker.getStatementSpec().getStreamSpecs().get(0);
    }
}
