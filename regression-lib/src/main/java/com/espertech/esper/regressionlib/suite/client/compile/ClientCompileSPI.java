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
package com.espertech.esper.regressionlib.suite.client.compile;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriod;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.compiler.internal.util.EPCompilerSPI;
import com.espertech.esper.compiler.internal.util.EPCompilerSPIExpression;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ClientCompileSPI {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientCompileSPIExpression());
        return execs;
    }

    private static class ClientCompileSPIExpression implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompilerSPI compiler = (EPCompilerSPI) EPCompilerProvider.getCompiler();

            EPCompilerSPIExpression expressionCompiler = null;
            try {
                expressionCompiler = compiler.expressionCompiler(new Configuration());
            } catch (EPCompileException e) {
                fail(e.getMessage());
            }

            compileEvaluate("1*1", 1, expressionCompiler);
            compileEvaluate("'a' || 'y'", "ay", expressionCompiler);

            Collection<Object> list = (Collection<Object>) compileEvaluate("java.util.Arrays.asList({\"a\"})", expressionCompiler);
            EPAssertionUtil.assertEqualsExactOrder(list.toArray(), new Object[] {"a"});

            compileEvaluate("java.util.Arrays.asList({'a', 'b'}).firstOf()", "a", expressionCompiler);

            try {
                ExprTimePeriod timePeriod = (ExprTimePeriod) expressionCompiler.compileValidate("5 seconds");
                assertEquals(5d, timePeriod.evaluateAsSeconds(null, true, null), 0.0001);
            } catch (EPCompileException e) {
                fail(e.getMessage());
            }
        }
    }

    private static void compileEvaluate(String expression, Object expected, EPCompilerSPIExpression expressionCompiler) {
        Object actual = compileEvaluate(expression, expressionCompiler);
        assertEquals(expected, actual);
    }

    private static Object compileEvaluate(String expression, EPCompilerSPIExpression expressionCompiler) {
        Object result = null;
        try {
            result = expressionCompiler.compileValidate(expression).getForge().getExprEvaluator().evaluate(null, true, null);
        } catch (EPCompileException e) {
            fail(e.getMessage());
        }
        return result;
    }
}
