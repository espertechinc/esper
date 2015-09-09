/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.expression;

import com.espertech.esper.epl.expression.core.ExprConstantNodeImpl;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.ops.ExprConcatNode;
import com.espertech.esper.epl.expression.ops.ExprMathNode;
import com.espertech.esper.support.epl.SupportExprNodeUtil;
import junit.framework.TestCase;
import com.espertech.esper.support.epl.SupportExprNode;
import com.espertech.esper.type.MathArithTypeEnum;

import java.util.ArrayList;
import java.util.List;

public class TestExprConcatNode extends TestCase
{
    private ExprConcatNode concatNode;

    public void setUp()
    {
        concatNode = new ExprConcatNode();
    }

    public void testGetType() throws Exception
    {
        assertEquals(String.class, concatNode.getType());
    }

    public void testToExpressionString() throws Exception
    {
        concatNode = new ExprConcatNode();
        concatNode.addChildNode(new SupportExprNode("a"));
        concatNode.addChildNode(new SupportExprNode("b"));
        assertEquals("\"a\"||\"b\"", ExprNodeUtility.toExpressionStringMinPrecedenceSafe(concatNode));
        concatNode.addChildNode(new SupportExprNode("c"));
        assertEquals("\"a\"||\"b\"||\"c\"", ExprNodeUtility.toExpressionStringMinPrecedenceSafe(concatNode));
    }

    public void testValidate()
    {
        // Must have 2 or more String subnodes
        try
        {
            concatNode.validate(ExprValidationContextFactory.makeEmpty());
            fail();
        }
        catch (ExprValidationException ex)
        {
            // Expected
        }

        // Must have only string-type subnodes
        concatNode.addChildNode(new SupportExprNode(String.class));
        concatNode.addChildNode(new SupportExprNode(Integer.class));
        try
        {
            concatNode.validate(ExprValidationContextFactory.makeEmpty());
            fail();
        }
        catch (ExprValidationException ex)
        {
            // Expected
        }
    }

    public void testEvaluate() throws Exception
    {
        concatNode.addChildNode(new SupportExprNode("x"));
        concatNode.addChildNode(new SupportExprNode("y"));
        SupportExprNodeUtil.validate(concatNode);
        assertEquals("xy", concatNode.evaluate(null, false, null));

        concatNode.addChildNode(new SupportExprNode("z"));
        SupportExprNodeUtil.validate(concatNode);
        assertEquals("xyz", concatNode.evaluate(null, false, null));
    }

    public void testEqualsNode() throws Exception
    {
        assertTrue(concatNode.equalsNode(concatNode));
        assertFalse(concatNode.equalsNode(new ExprMathNode(MathArithTypeEnum.DIVIDE, false, false)));
    }

    private static final class ConcatThread extends Thread {

        private final ExprConcatNode concatNode;
        private final int numRepetitions;
        private final String expectedValue;
        private String lastValue;

        public ConcatThread(final ExprConcatNode concatNode, int numRepetitions, String expectedValue) {
            this.concatNode = concatNode;
            this.numRepetitions = numRepetitions;
            this.expectedValue = expectedValue;
            this.lastValue = expectedValue;
        }

        public void run() {
            for(int i = 0; i < numRepetitions; i++) {
                final String result = (String) concatNode.evaluate(null, true, null);
                if (!expectedValue.equals(result)) {
                    lastValue = result;
                    break;
                }
            }
        }

    }

    /**
     * Minimal test case for concat node being called from multiple threads - this occurs in the following scenario:
     * 1) An Event pushed into a stream populates a partitioned window with 2 partitions - with one event in each partition,
     * 2) there is a statement on the window that uses a concatenation
     * 3) multiple threads release the events from the window
     */
    public void testEvaluateMultiThreads() throws Exception
    {
        concatNode.addChildNode(new ExprConstantNodeImpl("First"));
        concatNode.addChildNode(new ExprConstantNodeImpl(" and "));
        concatNode.addChildNode(new ExprConstantNodeImpl("Second"));
        SupportExprNodeUtil.validate(concatNode);

        final int numThreads = 2;
        final int numRepetitions = 10000;

        //Construct the threads
        final List<ConcatThread> threads = new ArrayList<ConcatThread>(numThreads);
        for(int i = 0; i < numThreads; i++) {
            final ConcatThread thread = new ConcatThread(concatNode, numRepetitions, "First and Second");
            threads.add(thread);
            thread.start();
        }

        //Wait for all to complete
        for(int i = 0; i < numThreads; i++) {
            threads.get(i).join();
        }

        //Check that the concatenations were all ok
        for(int i = 0; i < numThreads; i++) {
            assertEquals("First and Second", threads.get(i).lastValue);
        }
    }

}
