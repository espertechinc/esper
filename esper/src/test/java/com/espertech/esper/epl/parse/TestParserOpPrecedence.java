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
package com.espertech.esper.epl.parse;

import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.spec.PatternStreamSpecRaw;
import com.espertech.esper.pattern.PatternExpressionPrecedenceEnum;
import com.espertech.esper.supportunit.epl.parse.SupportParserHelper;
import junit.framework.TestCase;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;

/**
 * Test operator precedence and on-expression equivalence.
 * Precendences are similar to Java see <a>http://java.sun.com/docs/books/tutorial/java/nutsandbolts/expressions.html</a>)
 * <p>
 * Precedence ordering (highest on top):
 * postfix operators   -   within
 * unary operators     -   not, every
 * AND                 -   and
 * OR                  -   or
 * FOLLOWED BY         -   ->
 */
public class TestParserOpPrecedence extends TestCase {
    public void testEquivalency() throws Exception {
        assertEquivalent("every a",
                "(every a)");

        assertEquivalent("every a() or b()",
                "((every a()) or b())");

        assertEquivalent("every a -> b or c",
                "(every a) -> (b or c)");

        assertEquivalent("every a() -> b() and c()",
                "(every a()) -> (b() and c())");

        assertEquivalent("a() and b() or c()",
                "(a() and b()) or c()");

        assertEquivalent("a() or b() and c() or d()",
                "a() or (b() and c()) or d()");

        assertEquivalent("a() or b() and every e() -> f() -> c() or d()",
                "(a() or (b() and (every (e())))) -> f() -> (c() or d())");

        assertEquivalent("a() -> b() or e() -> f()",
                "a() -> (b() or e()) -> f()");

        String original = "every a() -> every b() and c() or d() and not e() -> f()";
        assertEquivalent(original, "every a() -> (every b()) and c() or d() and (not (e())) -> f()");
        assertEquivalent(original, "(every a()) -> ((every b()) and c()) or (d() and (not (e()))) -> f()");

        assertEquivalent("not a()",
                "(not a())");

        assertEquivalent("every a() where timer:within(5)",
                "every (a() where timer:within(5))");

        original = "every a() where timer:within(5) and not b() where timer:within(3) -> d() where timer:within(4)";
        assertEquivalent(original,
                "every (a() where timer:within(5)) and not (b() where timer:within(3)) -> (d() where timer:within(4))");
        assertEquivalent(original,
                "(every (a() where timer:within(5))) and (not (b() where timer:within(3))) -> (d() where timer:within(4))");
        assertEquivalent(original,
                "((every (a() where timer:within(5))) and (not (b() where timer:within(3)))) -> (d() where timer:within(4))");

        assertEquivalent("((a() where timer:within(10)) or (b() where timer:within(5))) where timer:within(20)",
                "(a() where timer:within(10) or b() where timer:within(5)) where timer:within(20)");

        assertEquivalent("timer:interval(20)", "(timer:interval(20))");
        assertEquivalent("every timer:interval(20)", "every (timer:interval(20))");
        assertEquivalent("timer:interval(20) -> timer:interval(20) or timer:interval(22)", "((timer:interval(20)) -> (timer:interval(20) or timer:interval(22)))");
        assertEquivalent("every a() -> every timer:interval(20) -> every c()", "(every a()) -> (every (timer:interval(20))) -> (every c())");

        original = "timer:at(5,0,[1,2],1:10,* /9,[1,2,5:8]) -> b()";
        assertEquivalent(original, original);
    }

    public void testNotEquivalent() throws Exception {
        assertNotEquivalent("a()", "every a()");
        assertNotEquivalent("a(n=6)", "a(n=7)");
        assertNotEquivalent("a(x=\"a\")", "a(x=\"b\")");
        assertNotEquivalent("a()", "b()");

        assertNotEquivalent("a() where timer:within(20)", "a() where timer:within(30)");
        assertNotEquivalent("a() or b() where timer:within(20)", "(a() or b()) where timer:within(20)");

        assertNotEquivalent("every a() or b()", "every (a() or b())");
        assertNotEquivalent("every a() and b()", "every (a() and b())");

        assertNotEquivalent("a() -> not b()", "not(a() -> b())");

        assertNotEquivalent("a() -> b() or c()", "(a() -> b()) or c()");

        assertNotEquivalent("a() and b() or c()", "a() and (b() or c())");

        assertNotEquivalent("timer:interval(20)", "timer:interval(30)");

        assertNotEquivalent("timer:at(20,*,*,*,*)", "timer:at(21,*,*,*,*)");
        assertNotEquivalent("timer:at([1:10],*,*,*,*)", "timer:at([1:11],*,*,*,*)");
        assertNotEquivalent("timer:at(*,*,3:2,*,*)", "timer:at(*,*,2:3,*,*)");

        assertNotEquivalent("EventA(value in [2:5])", "EventA(value in [3:5])");
        assertNotEquivalent("EventA(value in [2:5])", "EventA(value in [2:6])");
        assertNotEquivalent("EventA(value in [2:5])", "EventA(value in (2:6])");
        assertNotEquivalent("EventA(value in [2:5])", "EventA(value in [2:6))");
        assertNotEquivalent("EventA(value in [2:5])", "EventA(value in (2:6))");
    }

    private void assertEquivalent(String expressionOne, String expressionTwo) throws Exception {
        EPLTreeWalkerListener l1 = SupportParserHelper.parseAndWalkPattern(expressionOne);
        EPLTreeWalkerListener l2 = SupportParserHelper.parseAndWalkPattern(expressionTwo);

        String t1 = toPatternText(l1);
        String t2 = toPatternText(l2);
        assertEquals(t1, t2);
    }

    private String toPatternText(EPLTreeWalkerListener walker) {
        PatternStreamSpecRaw raw = (PatternStreamSpecRaw) walker.getStatementSpec().getStreamSpecs().get(0);
        StringWriter writer = new StringWriter();
        raw.getEvalFactoryNode().toEPL(writer, PatternExpressionPrecedenceEnum.MINIMUM);
        return writer.toString();
    }

    private void assertNotEquivalent(String expressionOne, String expressionTwo) throws Exception {
        log.debug(".assertEquivalent parsing: " + expressionOne);
        Pair<Tree, CommonTokenStream> astOne = parse(expressionOne);

        log.debug(".assertEquivalent parsing: " + expressionTwo);
        Pair<Tree, CommonTokenStream> astTwo = parse(expressionTwo);

        assertFalse(astOne.getFirst().toStringTree().equals(astTwo.getFirst().toStringTree()));
    }

    private Pair<Tree, CommonTokenStream> parse(String expression) throws Exception {
        return SupportParserHelper.parsePattern(expression);
    }

    private static final Logger log = LoggerFactory.getLogger(TestParserOpPrecedence.class);
}
