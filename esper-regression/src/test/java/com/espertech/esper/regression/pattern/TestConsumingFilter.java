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

package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanConstants;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestConsumingFilter extends TestCase implements SupportBeanConstants
{
    private EPServiceProvider engine;
    private SupportUpdateListener listener;

    public void setUp() throws Exception {
        engine = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        listener = new SupportUpdateListener();
        engine.initialize();
        engine.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
    }

    protected void tearDown() throws Exception {
        listener = null;
    }

    public void testFollowedBy() {
        String[] fields = "a,b".split(",");
        String pattern = "select a.theString as a, b.theString as b from pattern[every a=SupportBean -> b=SupportBean@consume]";
        engine.getEPAdministrator().createEPL(pattern).addListener(listener);

        engine.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        engine.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E2"});

        engine.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        engine.getEPRuntime().sendEvent(new SupportBean("E4", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", "E4"});

        engine.getEPRuntime().sendEvent(new SupportBean("E5", 0));
        engine.getEPRuntime().sendEvent(new SupportBean("E6", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E5", "E6"});
    }

    public void testAnd() {
        String[] fields = "a,b".split(",");
        String pattern = "select a.theString as a, b.theString as b from pattern[every (a=SupportBean and b=SupportBean)]";
        engine.getEPAdministrator().createEPL(pattern).addListener(listener);

        engine.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1"});
        engine.getEPAdministrator().destroyAllStatements();

        pattern = "select a.theString as a, b.theString as b from pattern [every (a=SupportBean and b=SupportBean(intPrimitive=10)@consume(2))]";
        engine.getEPAdministrator().createEPL(pattern).addListener(listener);

        engine.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        engine.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", "E1"});

        engine.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        engine.getEPRuntime().sendEvent(new SupportBean("E4", 1));
        engine.getEPRuntime().sendEvent(new SupportBean("E5", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", "E5"});
        engine.getEPAdministrator().destroyAllStatements();
        
        // test SODA
        EPStatementObjectModel model = engine.getEPAdministrator().compileEPL(pattern);
        assertEquals(pattern, model.toEPL());
        EPStatement stmt = engine.getEPAdministrator().create(model);
        assertEquals(pattern, stmt.getText());
        stmt.addListener(listener);

        engine.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        engine.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", "E1"});
    }

    public void testOr() {
        String[] fields = "a,b".split(",");
        runAssertion(fields, "select a.theString as a, b.theString as b from pattern[every a=SupportBean or b=SupportBean] order by a asc",
                new Object[][]{{null, "E1"}, {"E1", null}});

        runAssertion(fields, "select a.theString as a, b.theString as b from pattern[every a=SupportBean@consume(1) or every b=SupportBean@consume(1)] order by a asc",
                new Object[][]{{null, "E1"}, {"E1", null}});

        runAssertion(fields, "select a.theString as a, b.theString as b from pattern[every a=SupportBean@consume(2) or b=SupportBean@consume(1)] order by a asc",
                new Object[]{"E1", null});

        runAssertion(fields, "select a.theString as a, b.theString as b from pattern[every a=SupportBean@consume(1) or b=SupportBean@consume(2)] order by a asc",
                new Object[]{null, "E1"});

        runAssertion(fields, "select a.theString as a, b.theString as b from pattern[every a=SupportBean or b=SupportBean@consume(2)] order by a asc",
                new Object[]{null, "E1"});

        runAssertion(fields, "select a.theString as a, b.theString as b from pattern[every a=SupportBean@consume(1) or b=SupportBean] order by a asc",
                new Object[]{"E1", null});

        runAssertion(fields, "select a.theString as a, b.theString as b from pattern[every a=SupportBean(intPrimitive=11)@consume(1) or b=SupportBean] order by a asc",
                new Object[]{null, "E1"});

        runAssertion(fields, "select a.theString as a, b.theString as b from pattern[every a=SupportBean(intPrimitive=10)@consume(1) or b=SupportBean] order by a asc",
                new Object[]{"E1", null});

        fields = "a,b,c".split(",");
        runAssertion(fields, "select a.theString as a, b.theString as b, c.theString as c from pattern[every a=SupportBean@consume(1) or b=SupportBean@consume(2) or c=SupportBean@consume(3)] order by a,b,c",
                new Object[][]{{null, null, "E1"}});

        runAssertion(fields, "select a.theString as a, b.theString as b, c.theString as c from pattern[every a=SupportBean@consume(1) or every b=SupportBean@consume(2) or every c=SupportBean@consume(2)] order by a,b,c",
                new Object[][]{{null, null, "E1"}, {null, "E1", null}});

        runAssertion(fields, "select a.theString as a, b.theString as b, c.theString as c from pattern[every a=SupportBean@consume(2) or every b=SupportBean@consume(2) or every c=SupportBean@consume(2)] order by a,b,c",
                new Object[][]{{null, null, "E1"}, {null, "E1", null}, {"E1", null, null}});

        runAssertion(fields, "select a.theString as a, b.theString as b, c.theString as c from pattern[every a=SupportBean@consume(2) or every b=SupportBean@consume(2) or every c=SupportBean@consume(1)] order by a,b,c",
                new Object[][]{{null, "E1", null}, {"E1", null, null}});

        runAssertion(fields, "select a.theString as a, b.theString as b, c.theString as c from pattern[every a=SupportBean@consume(2) or every b=SupportBean@consume(1) or every c=SupportBean@consume(2)] order by a,b,c",
                new Object[][]{{null, null, "E1"}, {"E1", null, null}});

        runAssertion(fields, "select a.theString as a, b.theString as b, c.theString as c from pattern[every a=SupportBean@consume(0) or every b=SupportBean or every c=SupportBean] order by a,b,c",
                new Object[][]{{null, null, "E1"}, {null, "E1", null}, {"E1", null, null}});
    }

    public void testInvalid() {
        tryInvalid("select * from pattern[every a=SupportBean@consume()]",
                "Incorrect syntax near ')' expecting any of the following tokens {IntegerLiteral, FloatingPointLiteral} but found a closing parenthesis ')' at line 1 column 50, please check the filter specification within the pattern expression within the from clause [select * from pattern[every a=SupportBean@consume()]]");
        tryInvalid("select * from pattern[every a=SupportBean@consume(-1)]",
                "Incorrect syntax near '-' expecting any of the following tokens {IntegerLiteral, FloatingPointLiteral} but found a minus '-' at line 1 column 50, please check the filter specification within the pattern expression within the from clause [select * from pattern[every a=SupportBean@consume(-1)]]");
        tryInvalid("select * from pattern[every a=SupportBean@xx]",
                "Error in expression: Unexpected pattern filter @ annotation, expecting 'consume' but received 'xx' [select * from pattern[every a=SupportBean@xx]]");
    }

    private void tryInvalid(String epl, String message) {
        try {
            engine.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex) {
            // expected
            assertEquals(message, ex.getMessage());
        }
    }
    
    private void runAssertion(String[] fields, String pattern, Object expected) {
        engine.getEPAdministrator().destroyAllStatements();
        engine.getEPAdministrator().createEPL(pattern).addListener(listener);
        engine.getEPRuntime().sendEvent(new SupportBean("E1", 10));

        if (expected instanceof Object[][]) {
            EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, (Object[][]) expected);
        }
        else {
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, (Object[]) expected);
        }
    }
}


