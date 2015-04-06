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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.epl.spec.PatternStreamSpecCompiled;
import com.espertech.esper.epl.spec.StatementSpecCompiled;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.pattern.PatternExpressionPrecedenceEnum;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.io.StringWriter;

public class TestPatternExpressionText extends TestCase {
    private EPServiceProvider epService;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class.getName());
        config.addEventType("A", SupportBean_A.class.getName());
        config.addEventType("B", SupportBean_B.class.getName());
        config.addEventType("C", SupportBean_C.class.getName());
        config.addEventType("D", SupportBean_D.class.getName());
        config.addEventType("E", SupportBean_E.class.getName());
        config.addEventType("F", SupportBean_F.class.getName());
        config.addEventType("G", SupportBean_G.class.getName());

        epService = EPServiceProviderManager.getProvider(TestPatternExpressionText.class.getSimpleName(), config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        epService.destroy();
    }

    public void testOp() throws Exception
    {
        runAssertion("every a=SupportBean -> b=SupportBean@consume", null);
        runAssertion("every a=SupportBean -> b=SupportBean@consume", null);
        runAssertion("every a=SupportBean -> b=SupportBean@consume(2)", null);
        runAssertion("a=A -> b=B", null);
        runAssertion("b=B and every d=D", null);
        runAssertion("every b=B and d=B", null);
        runAssertion("b=B and d=D", null);
        runAssertion("every (b=B and d=D)", null);
        runAssertion("every (b=B and every d=D)", null);
        runAssertion("every b=B and every d=D", null);
        runAssertion("every (every b=B and d=D)", null);
        runAssertion("every a=A and d=D and b=B", null);
        runAssertion("every (every b=B and every d=D)", null);
        runAssertion("a=A and d=D and b=B", null);
        runAssertion("every a=A and every d=D and b=B", null);
        runAssertion("b=B and b=B", null);
        runAssertion("every a=A and every d=D and every b=B", null);
        runAssertion("every (a=A and every d=D and b=B)", null);
        runAssertion("every (b=B and b=B)", null);
        runAssertion("every b=B", null);
        runAssertion("b=B", null);
        runAssertion("every (every (every b=B))", "every every every b=B");
        runAssertion("every (every b=B())", "every every b=B");
        runAssertion("b=B -> d=D or not d=D", null);
        runAssertion("b=B -> (d=D or not d=D)", "b=B -> d=D or not d=D");
        runAssertion("b=B -[1000]> d=D or not d=D", null);
        runAssertion("b=B -> every d=D", null);
        runAssertion("b=B -> d=D", null);
        runAssertion("b=B -> not d=D", null);
        runAssertion("b=B -[1000]> not d=D", null);
        runAssertion("every b=B -> every d=D", null);
        runAssertion("every b=B -> d=D", null);
        runAssertion("every b=B -[10]> d=D", null);
        runAssertion("every (b=B -> every d=D)", null);
        runAssertion("every (a_1=A -> b=B -> a_2=A)", null);
        runAssertion("c=C -> d=D -> a=A", null);
        runAssertion("every (a_1=A -> b=B -> a_2=A)", null);
        runAssertion("every (a_1=A -[10]> b=B -[10]> a_2=A)", null);
        runAssertion("every (every a=A -> every b=B)", null);
        runAssertion("every (a=A -> every b=B)", null);
        runAssertion("a=A(id='A2') until D", "a=A(id=\"A2\") until D");
        runAssertion("b=B until a=A", null);
        runAssertion("b=B until D", null);
        runAssertion("(a=A or b=B) until d=D", null);
        runAssertion("(a=A or b=B) until (g=G or d=D)", null);
        runAssertion("a=A until G", null);
        runAssertion("[2] a=A", null);
        runAssertion("[1:1] a=A", null);
        runAssertion("[4] (a=A or b=B)", null);
        runAssertion("[2] b=B until a=A", null);
        runAssertion("[2:2] b=B until g=G", null);
        runAssertion("[:4] b=B until g=G", null);
        runAssertion("[1:] b=B until g=G", null);
        runAssertion("[1:2] b=B until a=A", null);
        runAssertion("c=C -> [2] b=B -> d=D", null);
        runAssertion("d=D until timer:interval(7 sec)", "d=D until timer:interval(7 seconds)");
        runAssertion("every (d=D until b=B)", null);
        runAssertion("every d=D until b=B", null);
        runAssertion("(every d=D) until b=B", "every d=D until b=B");
        runAssertion("a=A until (every (timer:interval(6 sec) and not A))", "a=A until every (timer:interval(6 seconds) and not A)");
        runAssertion("[2] (a=A or b=B)", null);
        runAssertion("every [2] a=A", "every ([2] a=A)");
        runAssertion("every [2] a=A until d=D", "every ([2] a=A) until d=D");  // every has precedence; ESPER-339
        runAssertion("[3] (a=A or b=B)", null);
        runAssertion("[4] (a=A or b=B)", null);
        runAssertion("(a=A until b=B) until c=C", "a=A until b=B until c=C");
        runAssertion("b=B and not d=D", null);
        runAssertion("every b=B and not g=G", null);
        runAssertion("every b=B and not g=G", null);
        runAssertion("b=B and not a=A(id=\"A1\")", null);
        runAssertion("every (b=B and not b3=B(id=\"B3\"))", null);
        runAssertion("every (b=B or not D)", null);
        runAssertion("every (every b=B and not B)", null);
        runAssertion("every (b=B and not B)", null);
        runAssertion("(b=B -> d=D) and G", null);
        runAssertion("(b=B -> d=D) and (a=A -> e=E)", null);
        runAssertion("b=B -> (d=D() or a=A)", "b=B -> d=D or a=A");
        runAssertion("b=B -> ((d=D -> a=A) or (a=A -> e=E))", "b=B -> (d=D -> a=A) or (a=A -> e=E)");
        runAssertion("(b=B -> d=D) or a=A", null);
        runAssertion("(b=B and d=D) or a=A", "b=B and d=D or a=A");
        runAssertion("a=A or a=A", null);
        runAssertion("a=A or b=B or c=C", null);
        runAssertion("every b=B or every d=D", null);
        runAssertion("a=A or b=B", null);
        runAssertion("a=A or every b=B", null);
        runAssertion("every a=A or d=D", null);
        runAssertion("every (every b=B or d=D)", null);
        runAssertion("every (b=B or every d=D)", null);
        runAssertion("every (every d=D or every b=B)", null);
        runAssertion("timer:at(10,8,*,*,*)", null);
        runAssertion("every timer:at(*/5,*,*,*,*,*)", null);
        runAssertion("timer:at(10,9,*,*,*,10) or timer:at(30,9,*,*,*,*)", null);
        runAssertion("b=B(id=\"B3\") -> timer:at(20,9,*,*,*,*)", null);
        runAssertion("timer:at(59,8,*,*,*,59) -> d=D", null);
        runAssertion("timer:at(22,8,*,*,*) -> b=B -> timer:at(55,*,*,*,*)", null);
        runAssertion("timer:at(40,*,*,*,*,1) and b=B", null);
        runAssertion("timer:at(40,9,*,*,*,1) or d=D", null);
        runAssertion("timer:at(22,8,*,*,*) -> b=B -> timer:at(55,8,*,*,*)", null);
        runAssertion("timer:at(22,8,*,*,*,1) where timer:within(30 minutes)", null);
        runAssertion("timer:at(*,9,*,*,*) and timer:at(55,*,*,*,*)", null);
        runAssertion("timer:at(40,8,*,*,*,1) and b=B", null);
        runAssertion("timer:interval(2 seconds)", null);
        runAssertion("timer:interval(2.001)", null);
        runAssertion("timer:interval(2999 milliseconds)", null);
        runAssertion("timer:interval(4 seconds) -> b=B", null);
        runAssertion("b=B -> timer:interval(0)", null);
        runAssertion("b=B -> timer:interval(6.0) -> d=D", null);
        runAssertion("every (b=B -> timer:interval(2.0) -> d=D)", null);
        runAssertion("b=B or timer:interval(2.001)", null);
        runAssertion("b=B or timer:interval(8.5)", null);
        runAssertion("timer:interval(8.5) or timer:interval(7.5)", null);
        runAssertion("timer:interval(999999 milliseconds) or g=G", null);
        runAssertion("b=B and timer:interval(4000 milliseconds)", null);
        runAssertion("b=B(id=\"B1\") where timer:within(2 seconds)", null);
        runAssertion("(every b=B) where timer:within(2.001)", null);
        runAssertion("every (b=B) where timer:within(6.001)", "every b=B where timer:within(6.001)");
        runAssertion("b=B -> d=D where timer:within(4001 milliseconds)", null);
        runAssertion("b=B -> d=D where timer:within(4 seconds)", null);
        runAssertion("every (b=B where timer:within(4.001) and d=D where timer:within(6.001))", null);
        runAssertion("every b=B -> d=D where timer:within(4000 seconds)", null);
        runAssertion("every b=B -> every d=D where timer:within(4000 seconds)", null);
        runAssertion("b=B -> d=D where timer:within(3999 seconds)", null);
        runAssertion("every b=B -> (every d=D) where timer:within(2001)", null);
        runAssertion("every (b=B -> d=D) where timer:within(6001)", null);
        runAssertion("b=B where timer:within(2000) or d=D where timer:within(6000)", null);
        runAssertion("(b=B where timer:within(2000) or d=D where timer:within(6000)) where timer:within(1999)", null);
        runAssertion("every (b=B where timer:within(2001) and d=D where timer:within(6001))", null);
        runAssertion("b=B where timer:within(2001) or d=D where timer:within(6001)", null);
        runAssertion("B where timer:within(2000) or d=D where timer:within(6001)", null);
        runAssertion("every b=B where timer:within(2001) and every d=D where timer:within(6001)", null);
        runAssertion("(every b=B) where timer:within(2000) and every d=D where timer:within(6001)", null);
        runAssertion("b=B(id=\"B1\") where timer:withinmax(2 seconds,100)", null);
        runAssertion("(every b=B) where timer:withinmax(4.001,2)", null);
        runAssertion("every b=B where timer:withinmax(2.001,4)", null);
        runAssertion("every (b=B where timer:withinmax(2001,0))", "every b=B where timer:withinmax(2001,0)");
        runAssertion("(every b=B) where timer:withinmax(4.001,2)", null);
        runAssertion("every b=B -> d=D where timer:withinmax(4000 milliseconds,1)", null);
        runAssertion("every b=B -> every d=D where timer:withinmax(4000,1)", null);
        runAssertion("every b=B -> (every d=D) where timer:withinmax(1 days,3)", null);
        runAssertion("a=A -> (every b=B) while (b.id!=\"B3\")", null);
        runAssertion("(every b=B) while (b.id!=\"B1\")", null);
        runAssertion("every-distinct(a.intPrimitive,1) a=SupportBean(theString like \"A%\")", null);
        runAssertion("every-distinct(a.intPrimitive,1 seconds) a=SupportBean(theString like \"A%\")", null);
        runAssertion("every-distinct(intPrimitive) a=SupportBean", null);
        runAssertion("[2] every-distinct(a.intPrimitive) a=SupportBean", null);
        runAssertion("every-distinct(a[0].intPrimitive) ([2] a=SupportBean)", null);
        runAssertion("every-distinct(a[0].intPrimitive,a[0].intPrimitive,1 hours) ([2] a=SupportBean)", null);
        runAssertion("(every-distinct(a.intPrimitive) a=SupportBean) where timer:within(10 seconds)", null);
        runAssertion("every-distinct(a.intPrimitive) a=SupportBean where timer:within(10)", null);
        runAssertion("every-distinct(a.intPrimitive,1 hours) a=SupportBean where timer:within(10)", null);
        runAssertion("every-distinct(a.intPrimitive,b.intPrimitive) (a=SupportBean(theString like \"A%\") and b=SupportBean(theString like \"B%\"))", null);
        runAssertion("every-distinct(a.intPrimitive) (a=SupportBean and not SupportBean)", null);
        runAssertion("every-distinct(a.intPrimitive,1 hours) (a=SupportBean and not SupportBean)", null);
        runAssertion("every-distinct(a.intPrimitive+b.intPrimitive,1 hours) (a=SupportBean -> b=SupportBean)", null);
        runAssertion("every-distinct(a.intPrimitive) a=SupportBean -> b=SupportBean(intPrimitive=a.intPrimitive)", null);
        runAssertion("every-distinct(a.intPrimitive) a=SupportBean -> every-distinct(b.intPrimitive) b=SupportBean(theString like \"B%\")", null);
    }

    private void runAssertion(String patternText, String expectedIfDifferent) {
        String epl = "@Name('A') select * from pattern [" + patternText + "]";
        runAssertionEPL(epl, patternText, expectedIfDifferent);

        epl = "@Audit @Name('A') select * from pattern [" + patternText + "]";
        runAssertionEPL(epl, patternText, expectedIfDifferent);
    }

    private void runAssertionEPL(String epl, String patternText, String expectedIfDifferent) {
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        EPStatementSPI spi = (EPStatementSPI) epService.getEPAdministrator().create(model);
        StatementSpecCompiled spec = ((EPServiceProviderSPI) (epService)).getStatementLifecycleSvc().getStatementSpec(spi.getStatementId());
        PatternStreamSpecCompiled pattern = (PatternStreamSpecCompiled) spec.getStreamSpecs()[0];
        StringWriter writer = new StringWriter();
        pattern.getEvalFactoryNode().toEPL(writer, PatternExpressionPrecedenceEnum.MINIMUM);
        if (expectedIfDifferent == null) {
            assertEquals(patternText, writer.toString());
        }
        else {
            assertEquals(expectedIfDifferent, writer.toString());
        }
        spi.destroy();
    }
}