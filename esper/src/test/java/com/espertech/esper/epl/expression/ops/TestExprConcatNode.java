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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.client.ConfigurationEngineDefaults;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprConstantNodeImpl;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.support.SupportExprValidationContextFactory;
import com.espertech.esper.supportunit.epl.SupportExprNode;
import com.espertech.esper.supportunit.epl.SupportExprNodeUtil;
import com.espertech.esper.type.MathArithTypeEnum;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestExprConcatNode extends TestCase {
    private ExprConcatNode concatNode;

    public void setUp() {
        concatNode = new ExprConcatNode();
    }

    public void testToExpressionString() throws Exception {
        concatNode = new ExprConcatNode();
        concatNode.addChildNode(new SupportExprNode("a"));
        concatNode.addChildNode(new SupportExprNode("b"));
        assertEquals("\"a\"||\"b\"", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(concatNode));
        concatNode.addChildNode(new SupportExprNode("c"));
        assertEquals("\"a\"||\"b\"||\"c\"", ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(concatNode));
    }

    public void testValidate() {
        // Must have 2 or more String subnodes
        try {
            concatNode.validate(SupportExprValidationContextFactory.makeEmpty());
            fail();
        } catch (ExprValidationException ex) {
            // Expected
        }

        // Must have only string-type subnodes
        concatNode.addChildNode(new SupportExprNode(String.class));
        concatNode.addChildNode(new SupportExprNode(Integer.class));
        try {
            concatNode.validate(SupportExprValidationContextFactory.makeEmpty());
            fail();
        } catch (ExprValidationException ex) {
            // Expected
        }
    }

    public void testEvaluate() throws Exception {
        concatNode.addChildNode(new SupportExprNode("x"));
        concatNode.addChildNode(new SupportExprNode("y"));
        SupportExprNodeUtil.validate(concatNode);
        assertEquals(String.class, concatNode.getForge().getEvaluationType());
        assertEquals("xy", concatNode.getForge().getExprEvaluator().evaluate(null, false, null));

        concatNode.addChildNode(new SupportExprNode("z"));
        SupportExprNodeUtil.validate(concatNode);
        assertEquals("xyz", concatNode.getForge().getExprEvaluator().evaluate(null, false, null));
    }

    public void testEqualsNode() throws Exception {
        assertTrue(concatNode.equalsNode(concatNode, false));
        assertFalse(concatNode.equalsNode(new ExprMathNode(MathArithTypeEnum.DIVIDE, false, false), false));
    }

    public void testThreading() throws Exception {
        runAssertionThreading(ConfigurationEngineDefaults.ThreadingProfile.LARGE);
        runAssertionThreading(ConfigurationEngineDefaults.ThreadingProfile.NORMAL);
    }

    private void runAssertionThreading(ConfigurationEngineDefaults.ThreadingProfile threadingProfile) throws Exception {
        concatNode = new ExprConcatNode();
        String textA = "This is the first text";
        String textB = "Second text";
        String textC = "Third text, some more";
        for (String text : Arrays.asList(textA, textB, textC)) {
            concatNode.addChildNode(new ExprConstantNodeImpl(text));
        }
        concatNode.validate(SupportExprValidationContextFactory.makeEmpty(threadingProfile));

        final int numThreads = 4;
        final int numLoop = 10000;

        List<SupportConcatThread> threads = new ArrayList<SupportConcatThread>(numThreads);
        for (int i = 0; i < numThreads; i++) {
            SupportConcatThread thread = new SupportConcatThread(concatNode, numLoop, textA + textB + textC);
            threads.add(thread);
            thread.start();
        }

        for (SupportConcatThread thread : threads) {
            thread.join();
            assertFalse(thread.isFail());
        }
    }

    private static final class SupportConcatThread extends Thread {

        private final ExprConcatNode node;
        private final int numLoop;
        private final String expectedResult;
        private boolean fail;

        public SupportConcatThread(ExprConcatNode node, int numLoop, String expectedResult) {
            this.node = node;
            this.numLoop = numLoop;
            this.expectedResult = expectedResult;
        }

        public void run() {
            ExprEvaluator eval = node.getForge().getExprEvaluator();
            for (int i = 0; i < numLoop; i++) {
                String result = (String) eval.evaluate(null, true, null);
                if (!expectedResult.equals(result)) {
                    fail = true;
                    break;
                }
            }
        }

        public boolean isFail() {
            return fail;
        }
    }
}
