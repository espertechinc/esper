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
package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.epl.spec.PatternStreamSpecCompiled;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.pattern.PatternExpressionPrecedenceEnum;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class ExecPatternExpressionText implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class.getName());
        configuration.addEventType("A", SupportBean_A.class.getName());
        configuration.addEventType("B", SupportBean_B.class.getName());
        configuration.addEventType("C", SupportBean_C.class.getName());
        configuration.addEventType("D", SupportBean_D.class.getName());
        configuration.addEventType("E", SupportBean_E.class.getName());
        configuration.addEventType("F", SupportBean_F.class.getName());
        configuration.addEventType("G", SupportBean_G.class.getName());
    }

    public void run(EPServiceProvider epService) throws Exception {
        tryAssertion(epService, "every a=SupportBean -> b=SupportBean@consume", null);
        tryAssertion(epService, "every a=SupportBean -> b=SupportBean@consume", null);
        tryAssertion(epService, "every a=SupportBean -> b=SupportBean@consume(2)", null);
        tryAssertion(epService, "a=A -> b=B", null);
        tryAssertion(epService, "b=B and every d=D", null);
        tryAssertion(epService, "every b=B and d=B", null);
        tryAssertion(epService, "b=B and d=D", null);
        tryAssertion(epService, "every (b=B and d=D)", null);
        tryAssertion(epService, "every (b=B and every d=D)", null);
        tryAssertion(epService, "every b=B and every d=D", null);
        tryAssertion(epService, "every (every b=B and d=D)", null);
        tryAssertion(epService, "every a=A and d=D and b=B", null);
        tryAssertion(epService, "every (every b=B and every d=D)", null);
        tryAssertion(epService, "a=A and d=D and b=B", null);
        tryAssertion(epService, "every a=A and every d=D and b=B", null);
        tryAssertion(epService, "b=B and b=B", null);
        tryAssertion(epService, "every a=A and every d=D and every b=B", null);
        tryAssertion(epService, "every (a=A and every d=D and b=B)", null);
        tryAssertion(epService, "every (b=B and b=B)", null);
        tryAssertion(epService, "every b=B", null);
        tryAssertion(epService, "b=B", null);
        tryAssertion(epService, "every (every (every b=B))", "every every every b=B");
        tryAssertion(epService, "every (every b=B())", "every every b=B");
        tryAssertion(epService, "b=B -> d=D or not d=D", null);
        tryAssertion(epService, "b=B -> (d=D or not d=D)", "b=B -> d=D or not d=D");
        tryAssertion(epService, "b=B -[1000]> d=D or not d=D", null);
        tryAssertion(epService, "b=B -> every d=D", null);
        tryAssertion(epService, "b=B -> d=D", null);
        tryAssertion(epService, "b=B -> not d=D", null);
        tryAssertion(epService, "b=B -[1000]> not d=D", null);
        tryAssertion(epService, "every b=B -> every d=D", null);
        tryAssertion(epService, "every b=B -> d=D", null);
        tryAssertion(epService, "every b=B -[10]> d=D", null);
        tryAssertion(epService, "every (b=B -> every d=D)", null);
        tryAssertion(epService, "every (a_1=A -> b=B -> a_2=A)", null);
        tryAssertion(epService, "c=C -> d=D -> a=A", null);
        tryAssertion(epService, "every (a_1=A -> b=B -> a_2=A)", null);
        tryAssertion(epService, "every (a_1=A -[10]> b=B -[10]> a_2=A)", null);
        tryAssertion(epService, "every (every a=A -> every b=B)", null);
        tryAssertion(epService, "every (a=A -> every b=B)", null);
        tryAssertion(epService, "a=A(id='A2') until D", "a=A(id=\"A2\") until D");
        tryAssertion(epService, "b=B until a=A", null);
        tryAssertion(epService, "b=B until D", null);
        tryAssertion(epService, "(a=A or b=B) until d=D", null);
        tryAssertion(epService, "(a=A or b=B) until (g=G or d=D)", null);
        tryAssertion(epService, "a=A until G", null);
        tryAssertion(epService, "[2] a=A", null);
        tryAssertion(epService, "[1:1] a=A", null);
        tryAssertion(epService, "[4] (a=A or b=B)", null);
        tryAssertion(epService, "[2] b=B until a=A", null);
        tryAssertion(epService, "[2:2] b=B until g=G", null);
        tryAssertion(epService, "[:4] b=B until g=G", null);
        tryAssertion(epService, "[1:] b=B until g=G", null);
        tryAssertion(epService, "[1:2] b=B until a=A", null);
        tryAssertion(epService, "c=C -> [2] b=B -> d=D", null);
        tryAssertion(epService, "d=D until timer:interval(7 sec)", "d=D until timer:interval(7 seconds)");
        tryAssertion(epService, "every (d=D until b=B)", null);
        tryAssertion(epService, "every d=D until b=B", null);
        tryAssertion(epService, "(every d=D) until b=B", "every d=D until b=B");
        tryAssertion(epService, "a=A until (every (timer:interval(6 sec) and not A))", "a=A until every (timer:interval(6 seconds) and not A)");
        tryAssertion(epService, "[2] (a=A or b=B)", null);
        tryAssertion(epService, "every [2] a=A", "every ([2] a=A)");
        tryAssertion(epService, "every [2] a=A until d=D", "every ([2] a=A) until d=D");  // every has precedence; ESPER-339
        tryAssertion(epService, "[3] (a=A or b=B)", null);
        tryAssertion(epService, "[4] (a=A or b=B)", null);
        tryAssertion(epService, "(a=A until b=B) until c=C", "a=A until b=B until c=C");
        tryAssertion(epService, "b=B and not d=D", null);
        tryAssertion(epService, "every b=B and not g=G", null);
        tryAssertion(epService, "every b=B and not g=G", null);
        tryAssertion(epService, "b=B and not a=A(id=\"A1\")", null);
        tryAssertion(epService, "every (b=B and not b3=B(id=\"B3\"))", null);
        tryAssertion(epService, "every (b=B or not D)", null);
        tryAssertion(epService, "every (every b=B and not B)", null);
        tryAssertion(epService, "every (b=B and not B)", null);
        tryAssertion(epService, "(b=B -> d=D) and G", null);
        tryAssertion(epService, "(b=B -> d=D) and (a=A -> e=E)", null);
        tryAssertion(epService, "b=B -> (d=D() or a=A)", "b=B -> d=D or a=A");
        tryAssertion(epService, "b=B -> ((d=D -> a=A) or (a=A -> e=E))", "b=B -> (d=D -> a=A) or (a=A -> e=E)");
        tryAssertion(epService, "(b=B -> d=D) or a=A", null);
        tryAssertion(epService, "(b=B and d=D) or a=A", "b=B and d=D or a=A");
        tryAssertion(epService, "a=A or a=A", null);
        tryAssertion(epService, "a=A or b=B or c=C", null);
        tryAssertion(epService, "every b=B or every d=D", null);
        tryAssertion(epService, "a=A or b=B", null);
        tryAssertion(epService, "a=A or every b=B", null);
        tryAssertion(epService, "every a=A or d=D", null);
        tryAssertion(epService, "every (every b=B or d=D)", null);
        tryAssertion(epService, "every (b=B or every d=D)", null);
        tryAssertion(epService, "every (every d=D or every b=B)", null);
        tryAssertion(epService, "timer:at(10,8,*,*,*)", null);
        tryAssertion(epService, "every timer:at(*/5,*,*,*,*,*)", null);
        tryAssertion(epService, "timer:at(10,9,*,*,*,10) or timer:at(30,9,*,*,*,*)", null);
        tryAssertion(epService, "b=B(id=\"B3\") -> timer:at(20,9,*,*,*,*)", null);
        tryAssertion(epService, "timer:at(59,8,*,*,*,59) -> d=D", null);
        tryAssertion(epService, "timer:at(22,8,*,*,*) -> b=B -> timer:at(55,*,*,*,*)", null);
        tryAssertion(epService, "timer:at(40,*,*,*,*,1) and b=B", null);
        tryAssertion(epService, "timer:at(40,9,*,*,*,1) or d=D", null);
        tryAssertion(epService, "timer:at(22,8,*,*,*) -> b=B -> timer:at(55,8,*,*,*)", null);
        tryAssertion(epService, "timer:at(22,8,*,*,*,1) where timer:within(30 minutes)", null);
        tryAssertion(epService, "timer:at(*,9,*,*,*) and timer:at(55,*,*,*,*)", null);
        tryAssertion(epService, "timer:at(40,8,*,*,*,1) and b=B", null);
        tryAssertion(epService, "timer:interval(2 seconds)", null);
        tryAssertion(epService, "timer:interval(2.001)", null);
        tryAssertion(epService, "timer:interval(2999 milliseconds)", null);
        tryAssertion(epService, "timer:interval(4 seconds) -> b=B", null);
        tryAssertion(epService, "b=B -> timer:interval(0)", null);
        tryAssertion(epService, "b=B -> timer:interval(6.0) -> d=D", null);
        tryAssertion(epService, "every (b=B -> timer:interval(2.0) -> d=D)", null);
        tryAssertion(epService, "b=B or timer:interval(2.001)", null);
        tryAssertion(epService, "b=B or timer:interval(8.5)", null);
        tryAssertion(epService, "timer:interval(8.5) or timer:interval(7.5)", null);
        tryAssertion(epService, "timer:interval(999999 milliseconds) or g=G", null);
        tryAssertion(epService, "b=B and timer:interval(4000 milliseconds)", null);
        tryAssertion(epService, "b=B(id=\"B1\") where timer:within(2 seconds)", null);
        tryAssertion(epService, "(every b=B) where timer:within(2.001)", null);
        tryAssertion(epService, "every (b=B) where timer:within(6.001)", "every b=B where timer:within(6.001)");
        tryAssertion(epService, "b=B -> d=D where timer:within(4001 milliseconds)", null);
        tryAssertion(epService, "b=B -> d=D where timer:within(4 seconds)", null);
        tryAssertion(epService, "every (b=B where timer:within(4.001) and d=D where timer:within(6.001))", null);
        tryAssertion(epService, "every b=B -> d=D where timer:within(4000 seconds)", null);
        tryAssertion(epService, "every b=B -> every d=D where timer:within(4000 seconds)", null);
        tryAssertion(epService, "b=B -> d=D where timer:within(3999 seconds)", null);
        tryAssertion(epService, "every b=B -> (every d=D) where timer:within(2001)", null);
        tryAssertion(epService, "every (b=B -> d=D) where timer:within(6001)", null);
        tryAssertion(epService, "b=B where timer:within(2000) or d=D where timer:within(6000)", null);
        tryAssertion(epService, "(b=B where timer:within(2000) or d=D where timer:within(6000)) where timer:within(1999)", null);
        tryAssertion(epService, "every (b=B where timer:within(2001) and d=D where timer:within(6001))", null);
        tryAssertion(epService, "b=B where timer:within(2001) or d=D where timer:within(6001)", null);
        tryAssertion(epService, "B where timer:within(2000) or d=D where timer:within(6001)", null);
        tryAssertion(epService, "every b=B where timer:within(2001) and every d=D where timer:within(6001)", null);
        tryAssertion(epService, "(every b=B) where timer:within(2000) and every d=D where timer:within(6001)", null);
        tryAssertion(epService, "b=B(id=\"B1\") where timer:withinmax(2 seconds,100)", null);
        tryAssertion(epService, "(every b=B) where timer:withinmax(4.001,2)", null);
        tryAssertion(epService, "every b=B where timer:withinmax(2.001,4)", null);
        tryAssertion(epService, "every (b=B where timer:withinmax(2001,0))", "every b=B where timer:withinmax(2001,0)");
        tryAssertion(epService, "(every b=B) where timer:withinmax(4.001,2)", null);
        tryAssertion(epService, "every b=B -> d=D where timer:withinmax(4000 milliseconds,1)", null);
        tryAssertion(epService, "every b=B -> every d=D where timer:withinmax(4000,1)", null);
        tryAssertion(epService, "every b=B -> (every d=D) where timer:withinmax(1 days,3)", null);
        tryAssertion(epService, "a=A -> (every b=B) while (b.id!=\"B3\")", null);
        tryAssertion(epService, "(every b=B) while (b.id!=\"B1\")", null);
        tryAssertion(epService, "every-distinct(a.intPrimitive,1) a=SupportBean(theString like \"A%\")", null);
        tryAssertion(epService, "every-distinct(a.intPrimitive,1 seconds) a=SupportBean(theString like \"A%\")", null);
        tryAssertion(epService, "every-distinct(intPrimitive) a=SupportBean", null);
        tryAssertion(epService, "[2] every-distinct(a.intPrimitive) a=SupportBean", null);
        tryAssertion(epService, "every-distinct(a[0].intPrimitive) ([2] a=SupportBean)", null);
        tryAssertion(epService, "every-distinct(a[0].intPrimitive,a[0].intPrimitive,1 hours) ([2] a=SupportBean)", null);
        tryAssertion(epService, "(every-distinct(a.intPrimitive) a=SupportBean) where timer:within(10 seconds)", null);
        tryAssertion(epService, "every-distinct(a.intPrimitive) a=SupportBean where timer:within(10)", null);
        tryAssertion(epService, "every-distinct(a.intPrimitive,1 hours) a=SupportBean where timer:within(10)", null);
        tryAssertion(epService, "every-distinct(a.intPrimitive,b.intPrimitive) (a=SupportBean(theString like \"A%\") and b=SupportBean(theString like \"B%\"))", null);
        tryAssertion(epService, "every-distinct(a.intPrimitive) (a=SupportBean and not SupportBean)", null);
        tryAssertion(epService, "every-distinct(a.intPrimitive,1 hours) (a=SupportBean and not SupportBean)", null);
        tryAssertion(epService, "every-distinct(a.intPrimitive+b.intPrimitive,1 hours) (a=SupportBean -> b=SupportBean)", null);
        tryAssertion(epService, "every-distinct(a.intPrimitive) a=SupportBean -> b=SupportBean(intPrimitive=a.intPrimitive)", null);
        tryAssertion(epService, "every-distinct(a.intPrimitive) a=SupportBean -> every-distinct(b.intPrimitive) b=SupportBean(theString like \"B%\")", null);
    }

    private void tryAssertion(EPServiceProvider epService, String patternText, String expectedIfDifferent) {
        String epl = "@Name('A') select * from pattern [" + patternText + "]";
        tryAssertionEPL(epService, epl, patternText, expectedIfDifferent);

        epl = "@Audit @Name('A') select * from pattern [" + patternText + "]";
        tryAssertionEPL(epService, epl, patternText, expectedIfDifferent);
    }

    private void tryAssertionEPL(EPServiceProvider epService, String epl, String patternText, String expectedIfDifferent) {
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        EPStatementSPI spi = (EPStatementSPI) epService.getEPAdministrator().create(model);
        StatementSpecCompiled spec = ((EPServiceProviderSPI) epService).getStatementLifecycleSvc().getStatementSpec(spi.getStatementId());
        PatternStreamSpecCompiled pattern = (PatternStreamSpecCompiled) spec.getStreamSpecs()[0];
        StringWriter writer = new StringWriter();
        pattern.getEvalFactoryNode().toEPL(writer, PatternExpressionPrecedenceEnum.MINIMUM);
        if (expectedIfDifferent == null) {
            assertEquals(patternText, writer.toString());
        } else {
            assertEquals(expectedIfDifferent, writer.toString());
        }
        spi.destroy();
    }
}