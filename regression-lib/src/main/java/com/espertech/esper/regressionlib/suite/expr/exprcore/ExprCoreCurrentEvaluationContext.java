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
package com.espertech.esper.regressionlib.suite.expr.exprcore;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.hook.expr.EPLExpressionEvaluationContext;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.support.client.SupportPortableCompileOptionStmtUserObject;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;

public class ExprCoreCurrentEvaluationContext {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprCoreCurrentEvalCtx(false));
        execs.add(new ExprCoreCurrentEvalCtx(true));
        return execs;
    }

    private static class ExprCoreCurrentEvalCtx implements RegressionExecution {
        private final boolean soda;

        public ExprCoreCurrentEvalCtx(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);

            String epl = "@name('s0') select " +
                "current_evaluation_context() as c0, " +
                "current_evaluation_context(), " +
                "current_evaluation_context().getRuntimeURI() as c2 from SupportBean";
            CompilerArguments arguments = new CompilerArguments(new Configuration());
            arguments.getOptions().setStatementUserObject(new SupportPortableCompileOptionStmtUserObject("my_user_object"));
            EPCompiled compiled = env.compile(soda, epl, arguments);
            env.deploy(compiled).addListener("s0").milestone(0);
            env.assertStmtType("s0", "current_evaluation_context()", EPTypePremade.getOrCreate(EPLExpressionEvaluationContext.class));

            env.sendEventBean(new SupportBean());
            env.assertEventNew("s0", event -> {
                EPLExpressionEvaluationContext ctx = (EPLExpressionEvaluationContext) event.get("c0");
                Assert.assertEquals(env.runtimeURI(), ctx.getRuntimeURI());
                Assert.assertEquals(env.statement("s0").getName(), ctx.getStatementName());
                Assert.assertEquals(-1, ctx.getContextPartitionId());
                Assert.assertEquals("my_user_object", ctx.getStatementUserObject());
                Assert.assertEquals(env.runtimeURI(), event.get("c2"));
            });

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.STATICHOOK);
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "soda=" + soda +
                '}';
        }
    }

    private static void sendTimer(RegressionEnvironment env, long timeInMSec) {
        env.advanceTime(timeInMSec);
    }
}
