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
package com.espertech.esper.regressionlib.suite.epl.other;

import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EPLOtherInvalid {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherInvalidFuncParams());
        execs.add(new EPLOtherInvalidSyntax());
        execs.add(new EPLOtherLongTypeConstant());
        execs.add(new EPLOtherDifferentJoins());
        return execs;
    }

    private static class EPLOtherInvalidFuncParams implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidCompile(env, "select count(theString, theString, theString) from SupportBean",
                "Failed to validate select-clause expression 'count(theString,theString,theString)': The 'count' function expects at least 1 and up to 2 parameters");

            tryInvalidCompile(env, "select leaving(theString) from SupportBean",
                "Failed to validate select-clause expression 'leaving(theString)': The 'leaving' function expects no parameters");
        }
    }

    private static class EPLOtherInvalidSyntax implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String exceptionText = getSyntaxExceptionEPL(env, "select * from *");
            assertEquals("Incorrect syntax near '*' at line 1 column 14, please check the from clause [select * from *]", exceptionText);

            exceptionText = getSyntaxExceptionEPL(env, "select * from SupportBean a where a.intPrimitive between r.start and r.end");
            assertEquals("Incorrect syntax near 'start' (a reserved keyword) at line 1 column 59, please check the where clause [select * from SupportBean a where a.intPrimitive between r.start and r.end]", exceptionText);

            tryInvalidCompile(env, "select * from SupportBean(1=2=3)",
                "Failed to validate filter expression '1=2': Invalid use of equals, expecting left-hand side and right-hand side but received 3 expressions");
        }
    }

    private static class EPLOtherLongTypeConstant implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select 2512570244 as value from SupportBean";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(new SupportBean());
            Assert.assertEquals(2512570244L, env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.undeployAll();
        }
    }

    private static class EPLOtherDifferentJoins implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidCompile(env, "select *", "The from-clause is required but has not been specified");

            String streamDef = "select * from " +
                "SupportBean#length(3) as sa," +
                "SupportBean#length(3) as sb" +
                " where ";

            String streamDefTwo = "select * from " +
                "SupportBean#length(3)," +
                "SupportMarketDataBean#length(3)" +
                " where ";

            tryInvalid(env, streamDef + "sa.intPrimitive = sb.theString");
            tryValid(env, streamDef + "sa.intPrimitive = sb.intBoxed");
            tryValid(env, streamDef + "sa.intPrimitive = sb.intPrimitive");
            tryValid(env, streamDef + "sa.intPrimitive = sb.longBoxed");

            tryInvalid(env, streamDef + "sa.intPrimitive = sb.intPrimitive and sb.intBoxed = sa.boolPrimitive");
            tryValid(env, streamDef + "sa.intPrimitive = sb.intPrimitive and sb.boolBoxed = sa.boolPrimitive");

            tryInvalid(env, streamDef + "sa.intPrimitive = sb.intPrimitive and sb.intBoxed = sa.intPrimitive and sa.theString=sX.theString");
            tryValid(env, streamDef + "sa.intPrimitive = sb.intPrimitive and sb.intBoxed = sa.intPrimitive and sa.theString=sb.theString");

            tryInvalid(env, streamDef + "sa.intPrimitive = sb.intPrimitive or sa.theString=sX.theString");
            tryValid(env, streamDef + "sa.intPrimitive = sb.intPrimitive or sb.intBoxed = sa.intPrimitive");

            // try constants
            tryValid(env, streamDef + "sa.intPrimitive=5");
            tryValid(env, streamDef + "sa.theString='4'");
            tryValid(env, streamDef + "sa.theString=\"4\"");
            tryValid(env, streamDef + "sa.boolPrimitive=false");
            tryValid(env, streamDef + "sa.longPrimitive=-5L");
            tryValid(env, streamDef + "sa.doubleBoxed=5.6d");
            tryValid(env, streamDef + "sa.floatPrimitive=-5.6f");

            tryInvalid(env, streamDef + "sa.intPrimitive='5'");
            tryInvalid(env, streamDef + "sa.theString=5");
            tryInvalid(env, streamDef + "sa.boolBoxed=f");
            tryInvalid(env, streamDef + "sa.intPrimitive=x");
            tryValid(env, streamDef + "sa.intPrimitive=5.5");

            // try addition and subtraction
            tryValid(env, streamDef + "sa.intPrimitive=sa.intBoxed + 5");
            tryValid(env, streamDef + "sa.intPrimitive=2*sa.intBoxed - sa.intPrimitive/10 + 1");
            tryValid(env, streamDef + "sa.intPrimitive=2*(sa.intBoxed - sa.intPrimitive)/(10 + 1)");
            tryInvalid(env, streamDef + "sa.intPrimitive=2*(sa.intBoxed");

            // try comparison
            tryValid(env, streamDef + "sa.intPrimitive > sa.intBoxed and sb.doublePrimitive < sb.doubleBoxed");
            tryValid(env, streamDef + "sa.intPrimitive >= sa.intBoxed and sa.doublePrimitive <= sa.doubleBoxed");
            tryValid(env, streamDef + "sa.intPrimitive > (sa.intBoxed + sb.doublePrimitive)");
            tryInvalid(env, streamDef + "sa.intPrimitive >= sa.theString");
            tryInvalid(env, streamDef + "sa.boolBoxed >= sa.boolPrimitive");

            // Try some nested
            tryValid(env, streamDef + "(sa.intPrimitive=3) or (sa.intBoxed=3 and sa.intPrimitive=1)");
            tryValid(env, streamDef + "((sa.intPrimitive>3) or (sa.intBoxed<3)) and sa.boolBoxed=false");
            tryValid(env, streamDef + "(sa.intPrimitive<=3 and sa.intPrimitive>=1) or (sa.boolBoxed=false and sa.boolPrimitive=true)");
            tryInvalid(env, streamDef + "sa.intPrimitive=3 or (sa.intBoxed=2");
            tryInvalid(env, streamDef + "sa.intPrimitive=3 or sa.intBoxed=2)");
            tryInvalid(env, streamDef + "sa.intPrimitive=3 or ((sa.intBoxed=2)");

            // Try some without stream name
            tryInvalid(env, streamDef + "intPrimitive=3");
            tryValid(env, streamDefTwo + "intPrimitive=3");

            // Try invalid outer join criteria
            String outerJoinDef = "select * from " +
                "SupportBean#length(3) as sa " +
                "left outer join " +
                "SupportBean#length(3) as sb ";
            tryValid(env, outerJoinDef + "on sa.intPrimitive = sb.intBoxed");
            tryInvalid(env, outerJoinDef + "on sa.intPrimitive = sb.XX");
            tryInvalid(env, outerJoinDef + "on sa.XX = sb.XX");
            tryInvalid(env, outerJoinDef + "on sa.XX = sb.intBoxed");
            tryInvalid(env, outerJoinDef + "on sa.boolBoxed = sb.intBoxed");
            tryValid(env, outerJoinDef + "on sa.boolPrimitive = sb.boolBoxed");
            tryInvalid(env, outerJoinDef + "on sa.boolPrimitive = sb.theString");
            tryInvalid(env, outerJoinDef + "on sa.intPrimitive <= sb.intBoxed");
            tryInvalid(env, outerJoinDef + "on sa.intPrimitive = sa.intBoxed");
            tryInvalid(env, outerJoinDef + "on sb.intPrimitive = sb.intBoxed");
            tryValid(env, outerJoinDef + "on sb.intPrimitive = sa.intBoxed");

            env.undeployAll();
        }
    }

    private static void tryInvalid(RegressionEnvironment env, String eplInvalidEPL) {
        try {
            env.compileWCheckedEx(eplInvalidEPL);
            fail();
        } catch (EPCompileException ex) {
            // Expected exception
        }
    }

    private static void tryValid(RegressionEnvironment env, String invalidEPL) {
        env.compileDeploy(invalidEPL);
    }

    private static String getSyntaxExceptionEPL(RegressionEnvironment env, String expression) {
        String exceptionText = null;
        try {
            env.compileWCheckedEx(expression);
            fail();
        } catch (EPCompileException ex) {
            exceptionText = ex.getMessage();
            log.debug(".getSyntaxExceptionEPL epl=" + expression, ex);
            // Expected exception
        }

        return exceptionText;
    }

    private final static Logger log = LoggerFactory.getLogger(EPLOtherInvalid.class);
}
