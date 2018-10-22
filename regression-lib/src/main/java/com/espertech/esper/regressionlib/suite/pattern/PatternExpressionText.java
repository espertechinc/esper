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
package com.espertech.esper.regressionlib.suite.pattern;

import com.espertech.esper.common.internal.epl.pattern.core.EvalRootForgeNode;
import com.espertech.esper.common.internal.epl.pattern.core.PatternExpressionPrecedenceEnum;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.patternassert.SupportPatternCompileHook;

import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class PatternExpressionText implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        tryAssertion(env, "every a=SupportBean -> b=SupportBean@consume", null);
        tryAssertion(env, "every a=SupportBean -> b=SupportBean@consume", null);
        tryAssertion(env, "every a=SupportBean -> b=SupportBean@consume(2)", null);
        tryAssertion(env, "a=SupportBean_A -> b=SupportBean_B", null);
        tryAssertion(env, "b=SupportBean_B and every d=SupportBean_D", null);
        tryAssertion(env, "every b=SupportBean_B and d=SupportBean_B", null);
        tryAssertion(env, "b=SupportBean_B and d=SupportBean_D", null);
        tryAssertion(env, "every (b=SupportBean_B and d=SupportBean_D)", null);
        tryAssertion(env, "every (b=SupportBean_B and every d=SupportBean_D)", null);
        tryAssertion(env, "every b=SupportBean_B and every d=SupportBean_D", null);
        tryAssertion(env, "every (every b=SupportBean_B and d=SupportBean_D)", null);
        tryAssertion(env, "every a=SupportBean_A and d=SupportBean_D and b=SupportBean_B", null);
        tryAssertion(env, "every (every b=SupportBean_B and every d=SupportBean_D)", null);
        tryAssertion(env, "a=SupportBean_A and d=SupportBean_D and b=SupportBean_B", null);
        tryAssertion(env, "every a=SupportBean_A and every d=SupportBean_D and b=SupportBean_B", null);
        tryAssertion(env, "b=SupportBean_B and b=SupportBean_B", null);
        tryAssertion(env, "every a=SupportBean_A and every d=SupportBean_D and every b=SupportBean_B", null);
        tryAssertion(env, "every (a=SupportBean_A and every d=SupportBean_D and b=SupportBean_B)", null);
        tryAssertion(env, "every (b=SupportBean_B and b=SupportBean_B)", null);
        tryAssertion(env, "every b=SupportBean_B", null);
        tryAssertion(env, "b=SupportBean_B", null);
        tryAssertion(env, "every (every (every b=SupportBean_B))", "every every every b=SupportBean_B");
        tryAssertion(env, "every (every b=SupportBean_B())", "every every b=SupportBean_B");
        tryAssertion(env, "b=SupportBean_B -> d=SupportBean_D or not d=SupportBean_D", null);
        tryAssertion(env, "b=SupportBean_B -> (d=SupportBean_D or not d=SupportBean_D)", "b=SupportBean_B -> d=SupportBean_D or not d=SupportBean_D");
        tryAssertion(env, "b=SupportBean_B -[1000]> d=SupportBean_D or not d=SupportBean_D", null);
        tryAssertion(env, "b=SupportBean_B -> every d=SupportBean_D", null);
        tryAssertion(env, "b=SupportBean_B -> d=SupportBean_D", null);
        tryAssertion(env, "b=SupportBean_B -> not d=SupportBean_D", null);
        tryAssertion(env, "b=SupportBean_B -[1000]> not d=SupportBean_D", null);
        tryAssertion(env, "every b=SupportBean_B -> every d=SupportBean_D", null);
        tryAssertion(env, "every b=SupportBean_B -> d=SupportBean_D", null);
        tryAssertion(env, "every b=SupportBean_B -[10]> d=SupportBean_D", null);
        tryAssertion(env, "every (b=SupportBean_B -> every d=SupportBean_D)", null);
        tryAssertion(env, "every (a_1=SupportBean_A -> b=SupportBean_B -> a_2=SupportBean_A)", null);
        tryAssertion(env, "c=SupportBean_C -> d=SupportBean_D -> a=SupportBean_A", null);
        tryAssertion(env, "every (a_1=SupportBean_A -> b=SupportBean_B -> a_2=SupportBean_A)", null);
        tryAssertion(env, "every (a_1=SupportBean_A -[10]> b=SupportBean_B -[10]> a_2=SupportBean_A)", null);
        tryAssertion(env, "every (every a=SupportBean_A -> every b=SupportBean_B)", null);
        tryAssertion(env, "every (a=SupportBean_A -> every b=SupportBean_B)", null);
        tryAssertion(env, "a=SupportBean_A(id='A2') until SupportBean_D", "a=SupportBean_A(id=\"A2\") until SupportBean_D");
        tryAssertion(env, "b=SupportBean_B until a=SupportBean_A", null);
        tryAssertion(env, "b=SupportBean_B until SupportBean_D", null);
        tryAssertion(env, "(a=SupportBean_A or b=SupportBean_B) until d=SupportBean_D", null);
        tryAssertion(env, "(a=SupportBean_A or b=SupportBean_B) until (g=SupportBean_G or d=SupportBean_D)", null);
        tryAssertion(env, "a=SupportBean_A until SupportBean_G", null);
        tryAssertion(env, "[2] a=SupportBean_A", null);
        tryAssertion(env, "[1:1] a=SupportBean_A", null);
        tryAssertion(env, "[4] (a=SupportBean_A or b=SupportBean_B)", null);
        tryAssertion(env, "[2] b=SupportBean_B until a=SupportBean_A", null);
        tryAssertion(env, "[2:2] b=SupportBean_B until g=SupportBean_G", null);
        tryAssertion(env, "[:4] b=SupportBean_B until g=SupportBean_G", null);
        tryAssertion(env, "[1:] b=SupportBean_B until g=SupportBean_G", null);
        tryAssertion(env, "[1:2] b=SupportBean_B until a=SupportBean_A", null);
        tryAssertion(env, "c=SupportBean_C -> [2] b=SupportBean_B -> d=SupportBean_D", null);
        tryAssertion(env, "d=SupportBean_D until timer:interval(7 sec)", "d=SupportBean_D until timer:interval(7 seconds)");
        tryAssertion(env, "every (d=SupportBean_D until b=SupportBean_B)", null);
        tryAssertion(env, "every d=SupportBean_D until b=SupportBean_B", null);
        tryAssertion(env, "(every d=SupportBean_D) until b=SupportBean_B", "every d=SupportBean_D until b=SupportBean_B");
        tryAssertion(env, "a=SupportBean_A until (every (timer:interval(6 sec) and not SupportBean_A))", "a=SupportBean_A until every (timer:interval(6 seconds) and not SupportBean_A)");
        tryAssertion(env, "[2] (a=SupportBean_A or b=SupportBean_B)", null);
        tryAssertion(env, "every [2] a=SupportBean_A", "every ([2] a=SupportBean_A)");
        tryAssertion(env, "every [2] a=SupportBean_A until d=SupportBean_D", "every ([2] a=SupportBean_A) until d=SupportBean_D");  // every has precedence; ESPER-339
        tryAssertion(env, "[3] (a=SupportBean_A or b=SupportBean_B)", null);
        tryAssertion(env, "[4] (a=SupportBean_A or b=SupportBean_B)", null);
        tryAssertion(env, "(a=SupportBean_A until b=SupportBean_B) until c=SupportBean_C", "a=SupportBean_A until b=SupportBean_B until c=SupportBean_C");
        tryAssertion(env, "b=SupportBean_B and not d=SupportBean_D", null);
        tryAssertion(env, "every b=SupportBean_B and not g=SupportBean_G", null);
        tryAssertion(env, "every b=SupportBean_B and not g=SupportBean_G", null);
        tryAssertion(env, "b=SupportBean_B and not a=SupportBean_A(id=\"A1\")", null);
        tryAssertion(env, "every (b=SupportBean_B and not b3=SupportBean_B(id=\"B3\"))", null);
        tryAssertion(env, "every (b=SupportBean_B or not SupportBean_D)", null);
        tryAssertion(env, "every (every b=SupportBean_B and not SupportBean_B)", null);
        tryAssertion(env, "every (b=SupportBean_B and not SupportBean_B)", null);
        tryAssertion(env, "(b=SupportBean_B -> d=SupportBean_D) and SupportBean_G", null);
        tryAssertion(env, "(b=SupportBean_B -> d=SupportBean_D) and (a=SupportBean_A -> e=SupportBean_E)", null);
        tryAssertion(env, "b=SupportBean_B -> (d=SupportBean_D() or a=SupportBean_A)", "b=SupportBean_B -> d=SupportBean_D or a=SupportBean_A");
        tryAssertion(env, "b=SupportBean_B -> ((d=SupportBean_D -> a=SupportBean_A) or (a=SupportBean_A -> e=SupportBean_E))", "b=SupportBean_B -> (d=SupportBean_D -> a=SupportBean_A) or (a=SupportBean_A -> e=SupportBean_E)");
        tryAssertion(env, "(b=SupportBean_B -> d=SupportBean_D) or a=SupportBean_A", null);
        tryAssertion(env, "(b=SupportBean_B and d=SupportBean_D) or a=SupportBean_A", "b=SupportBean_B and d=SupportBean_D or a=SupportBean_A");
        tryAssertion(env, "a=SupportBean_A or a=SupportBean_A", null);
        tryAssertion(env, "a=SupportBean_A or b=SupportBean_B or c=SupportBean_C", null);
        tryAssertion(env, "every b=SupportBean_B or every d=SupportBean_D", null);
        tryAssertion(env, "a=SupportBean_A or b=SupportBean_B", null);
        tryAssertion(env, "a=SupportBean_A or every b=SupportBean_B", null);
        tryAssertion(env, "every a=SupportBean_A or d=SupportBean_D", null);
        tryAssertion(env, "every (every b=SupportBean_B or d=SupportBean_D)", null);
        tryAssertion(env, "every (b=SupportBean_B or every d=SupportBean_D)", null);
        tryAssertion(env, "every (every d=SupportBean_D or every b=SupportBean_B)", null);
        tryAssertion(env, "timer:at(10,8,*,*,*)", null);
        tryAssertion(env, "every timer:at(*/5,*,*,*,*,*)", null);
        tryAssertion(env, "timer:at(10,9,*,*,*,10) or timer:at(30,9,*,*,*,*)", null);
        tryAssertion(env, "b=SupportBean_B(id=\"B3\") -> timer:at(20,9,*,*,*,*)", null);
        tryAssertion(env, "timer:at(59,8,*,*,*,59) -> d=SupportBean_D", null);
        tryAssertion(env, "timer:at(22,8,*,*,*) -> b=SupportBean_B -> timer:at(55,*,*,*,*)", null);
        tryAssertion(env, "timer:at(40,*,*,*,*,1) and b=SupportBean_B", null);
        tryAssertion(env, "timer:at(40,9,*,*,*,1) or d=SupportBean_D", null);
        tryAssertion(env, "timer:at(22,8,*,*,*) -> b=SupportBean_B -> timer:at(55,8,*,*,*)", null);
        tryAssertion(env, "timer:at(22,8,*,*,*,1) where timer:within(30 minutes)", null);
        tryAssertion(env, "timer:at(*,9,*,*,*) and timer:at(55,*,*,*,*)", null);
        tryAssertion(env, "timer:at(40,8,*,*,*,1) and b=SupportBean_B", null);
        tryAssertion(env, "timer:interval(2 seconds)", null);
        tryAssertion(env, "timer:interval(2.001)", null);
        tryAssertion(env, "timer:interval(2999 milliseconds)", null);
        tryAssertion(env, "timer:interval(4 seconds) -> b=SupportBean_B", null);
        tryAssertion(env, "b=SupportBean_B -> timer:interval(0)", null);
        tryAssertion(env, "b=SupportBean_B -> timer:interval(6.0) -> d=SupportBean_D", null);
        tryAssertion(env, "every (b=SupportBean_B -> timer:interval(2.0) -> d=SupportBean_D)", null);
        tryAssertion(env, "b=SupportBean_B or timer:interval(2.001)", null);
        tryAssertion(env, "b=SupportBean_B or timer:interval(8.5)", null);
        tryAssertion(env, "timer:interval(8.5) or timer:interval(7.5)", null);
        tryAssertion(env, "timer:interval(999999 milliseconds) or g=SupportBean_G", null);
        tryAssertion(env, "b=SupportBean_B and timer:interval(4000 milliseconds)", null);
        tryAssertion(env, "b=SupportBean_B(id=\"B1\") where timer:within(2 seconds)", null);
        tryAssertion(env, "(every b=SupportBean_B) where timer:within(2.001)", null);
        tryAssertion(env, "every (b=SupportBean_B) where timer:within(6.001)", "every b=SupportBean_B where timer:within(6.001)");
        tryAssertion(env, "b=SupportBean_B -> d=SupportBean_D where timer:within(4001 milliseconds)", null);
        tryAssertion(env, "b=SupportBean_B -> d=SupportBean_D where timer:within(4 seconds)", null);
        tryAssertion(env, "every (b=SupportBean_B where timer:within(4.001) and d=SupportBean_D where timer:within(6.001))", null);
        tryAssertion(env, "every b=SupportBean_B -> d=SupportBean_D where timer:within(4000 seconds)", null);
        tryAssertion(env, "every b=SupportBean_B -> every d=SupportBean_D where timer:within(4000 seconds)", null);
        tryAssertion(env, "b=SupportBean_B -> d=SupportBean_D where timer:within(3999 seconds)", null);
        tryAssertion(env, "every b=SupportBean_B -> (every d=SupportBean_D) where timer:within(2001)", null);
        tryAssertion(env, "every (b=SupportBean_B -> d=SupportBean_D) where timer:within(6001)", null);
        tryAssertion(env, "b=SupportBean_B where timer:within(2000) or d=SupportBean_D where timer:within(6000)", null);
        tryAssertion(env, "(b=SupportBean_B where timer:within(2000) or d=SupportBean_D where timer:within(6000)) where timer:within(1999)", null);
        tryAssertion(env, "every (b=SupportBean_B where timer:within(2001) and d=SupportBean_D where timer:within(6001))", null);
        tryAssertion(env, "b=SupportBean_B where timer:within(2001) or d=SupportBean_D where timer:within(6001)", null);
        tryAssertion(env, "SupportBean_B where timer:within(2000) or d=SupportBean_D where timer:within(6001)", null);
        tryAssertion(env, "every b=SupportBean_B where timer:within(2001) and every d=SupportBean_D where timer:within(6001)", null);
        tryAssertion(env, "(every b=SupportBean_B) where timer:within(2000) and every d=SupportBean_D where timer:within(6001)", null);
        tryAssertion(env, "b=SupportBean_B(id=\"B1\") where timer:withinmax(2 seconds,100)", null);
        tryAssertion(env, "(every b=SupportBean_B) where timer:withinmax(4.001,2)", null);
        tryAssertion(env, "every b=SupportBean_B where timer:withinmax(2.001,4)", null);
        tryAssertion(env, "every (b=SupportBean_B where timer:withinmax(2001,0))", "every b=SupportBean_B where timer:withinmax(2001,0)");
        tryAssertion(env, "(every b=SupportBean_B) where timer:withinmax(4.001,2)", null);
        tryAssertion(env, "every b=SupportBean_B -> d=SupportBean_D where timer:withinmax(4000 milliseconds,1)", null);
        tryAssertion(env, "every b=SupportBean_B -> every d=SupportBean_D where timer:withinmax(4000,1)", null);
        tryAssertion(env, "every b=SupportBean_B -> (every d=SupportBean_D) where timer:withinmax(1 days,3)", null);
        tryAssertion(env, "a=SupportBean_A -> (every b=SupportBean_B) while (b.id!=\"B3\")", null);
        tryAssertion(env, "(every b=SupportBean_B) while (b.id!=\"B1\")", null);
        tryAssertion(env, "every-distinct(a.intPrimitive,1) a=SupportBean(theString like \"A%\")", null);
        tryAssertion(env, "every-distinct(a.intPrimitive,1 seconds) a=SupportBean(theString like \"A%\")", null);
        tryAssertion(env, "every-distinct(intPrimitive) a=SupportBean", null);
        tryAssertion(env, "[2] every-distinct(a.intPrimitive) a=SupportBean", null);
        tryAssertion(env, "every-distinct(a[0].intPrimitive) ([2] a=SupportBean)", null);
        tryAssertion(env, "every-distinct(a[0].intPrimitive,a[0].intPrimitive,1 hours) ([2] a=SupportBean)", null);
        tryAssertion(env, "(every-distinct(a.intPrimitive) a=SupportBean) where timer:within(10 seconds)", null);
        tryAssertion(env, "every-distinct(a.intPrimitive) a=SupportBean where timer:within(10)", null);
        tryAssertion(env, "every-distinct(a.intPrimitive,1 hours) a=SupportBean where timer:within(10)", null);
        tryAssertion(env, "every-distinct(a.intPrimitive,b.intPrimitive) (a=SupportBean(theString like \"A%\") and b=SupportBean(theString like \"B%\"))", null);
        tryAssertion(env, "every-distinct(a.intPrimitive) (a=SupportBean and not SupportBean)", null);
        tryAssertion(env, "every-distinct(a.intPrimitive,1 hours) (a=SupportBean and not SupportBean)", null);
        tryAssertion(env, "every-distinct(a.intPrimitive+b.intPrimitive,1 hours) (a=SupportBean -> b=SupportBean)", null);
        tryAssertion(env, "every-distinct(a.intPrimitive) a=SupportBean -> b=SupportBean(intPrimitive=a.intPrimitive)", null);
        tryAssertion(env, "every-distinct(a.intPrimitive) a=SupportBean -> every-distinct(b.intPrimitive) b=SupportBean(theString like \"B%\")", null);

        SupportPatternCompileHook.reset();
    }

    private static void tryAssertion(RegressionEnvironment env, String patternText, String expectedIfDifferent) {
        String epl = "@Name('A') select * from pattern [" + patternText + "]";
        tryAssertionEPL(env, epl, patternText, expectedIfDifferent);

        epl = "@Audit @Name('A') select * from pattern [" + patternText + "]";
        tryAssertionEPL(env, epl, patternText, expectedIfDifferent);
    }

    private static void tryAssertionEPL(RegressionEnvironment env, String epl, String patternText, String expectedIfDifferent) {
        String hook = "@Hook(type=INTERNAL_PATTERNCOMPILE,hook='" + SupportPatternCompileHook.class.getName() + "')";
        epl = hook + epl;
        env.compile(epl);

        EvalRootForgeNode root = SupportPatternCompileHook.getOneAndReset();

        StringWriter writer = new StringWriter();
        root.toEPL(writer, PatternExpressionPrecedenceEnum.MINIMUM);
        if (expectedIfDifferent == null) {
            assertEquals(patternText, writer.toString());
        } else {
            assertEquals(expectedIfDifferent, writer.toString());
        }
    }
}