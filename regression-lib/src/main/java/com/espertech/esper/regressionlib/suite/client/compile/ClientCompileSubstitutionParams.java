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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.support.SupportJavaVersionUtil;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.runtime.client.DeploymentOptions;
import com.espertech.esper.runtime.client.EPDeployException;
import com.espertech.esper.runtime.client.EPDeploySubstitutionParameterException;
import com.espertech.esper.runtime.client.option.StatementSubstitutionParameterContext;
import com.espertech.esper.runtime.client.option.StatementSubstitutionParameterOption;
import org.junit.Assert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.*;

public class ClientCompileSubstitutionParams {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientCompileSubstParamNamedParameter(false));
        execs.add(new ClientCompileSubstParamNamedParameter(true));
        execs.add(new ClientCompileSubstParamMethodInvocation());
        execs.add(new ClientCompileSubstParamUnnamedParameterWType(false));
        execs.add(new ClientCompileSubstParamUnnamedParameterWType(true));
        execs.add(new ClientCompileSubstParamPattern());
        execs.add(new ClientCompileSubstParamSimpleOneParameterWCast());
        execs.add(new ClientCompileSubstParamWInheritance());
        execs.add(new ClientCompileSubstParamSimpleTwoParameterFilter());
        execs.add(new ClientCompileSubstParamSimpleTwoParameterWhere());
        execs.add(new ClientCompileSubstParamSimpleNoParameter());
        execs.add(new ClientCompileSubstParamPrimitiveVsBoxed());
        execs.add(new ClientCompileSubstParamSubselect());
        execs.add(new ClientCompileSubstParamInvalidUse());
        execs.add(new ClientCompileSubstParamInvalidNoCallback());
        execs.add(new ClientCompileSubstParamInvalidInsufficientValues());
        execs.add(new ClientCompileSubstParamInvalidParametersUntyped());
        execs.add(new ClientCompileSubstParamInvalidParametersTyped());
        execs.add(new ClientCompileSubstParamResolverContext());
        execs.add(new ClientCompileSubstParamMultiStmt());
        execs.add(new ClientCompileSubstParamArray(false));
        execs.add(new ClientCompileSubstParamArray(true));
        return execs;
    }

    private static class ClientCompileSubstParamArray implements RegressionExecution {
        private final boolean soda;

        public ClientCompileSubstParamArray(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "?:a0:int[] as c0, " +
                "?:a1:int[primitive] as c1, " +
                "?:a2:java.lang.Object[] as c2, " +
                "?:a3:string[][] as c3, " +
                "?:a4:java.lang.Object[][] as c4 " +
                "from SupportBean";

            EPCompiled compiled;
            if (soda) {
                EPStatementObjectModel copy = env.eplToModel(epl);
                Assert.assertEquals(epl.trim(), copy.toEPL());
                compiled = env.compile(copy, new CompilerArguments(env.getConfiguration()));
            } else {
                compiled = env.compile(epl);
            }

            DeploymentOptions options = new DeploymentOptions().setStatementSubstitutionParameter(new StatementSubstitutionParameterOption() {
                public void setStatementParameters(StatementSubstitutionParameterContext env) {
                    env.setObject("a0", new Integer[]{1, 2});
                    env.setObject("a1", new int[]{3, 4});
                    env.setObject("a2", new Object[]{"a", "b"});
                    env.setObject("a3", new String[][]{{"A"}});
                    env.setObject("a4", new Object[][]{{5, 6}});
                }
            });
            try {
                env.deployment().deploy(compiled, options);
            } catch (EPDeployException e) {
                throw new RuntimeException(e);
            }
            env.addListener("s0");

            EventType eventType = env.statement("s0").getEventType();
            assertEquals(Integer[].class, eventType.getPropertyType("c0"));
            assertEquals(int[].class, eventType.getPropertyType("c1"));
            assertEquals(Object[].class, eventType.getPropertyType("c2"));
            assertEquals(String[][].class, eventType.getPropertyType("c3"));
            assertEquals(Object[][].class, eventType.getPropertyType("c4"));

            env.sendEventBean(new SupportBean());

            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertEqualsExactOrder(new Integer[]{1, 2}, (Integer[]) event.get("c0"));
            EPAssertionUtil.assertEqualsExactOrder(new int[]{3, 4}, (int[]) event.get("c1"));
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"a", "b"}, (Object[]) event.get("c2"));
            EPAssertionUtil.assertEqualsExactOrder(new String[][]{{"A"}}, (String[][]) event.get("c3"));
            EPAssertionUtil.assertEqualsExactOrder(new Object[][]{{5, 6}}, (Object[][]) event.get("c4"));

            env.undeployAll();
        }
    }

    private static class ClientCompileSubstParamMultiStmt implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBean_S0(id=?:subs_1:int);\n" +
                "@name('s1') select * from SupportBean_S1(p10=?:subs_2:string);\n";
            EPCompiled compiled = env.compile(epl);

            DeploymentOptions options = new DeploymentOptions().setStatementSubstitutionParameter(new StatementSubstitutionParameterOption() {
                public void setStatementParameters(StatementSubstitutionParameterContext env) {
                    if (env.getStatementName().equals("s1")) {
                        env.setObject("subs_2", "abc");
                    } else {
                        env.setObject("subs_1", 100);
                    }
                }
            });
            try {
                env.deployment().deploy(compiled, options);
            } catch (EPDeployException e) {
                throw new RuntimeException(e);
            }
            env.addListener("s0").addListener("s1");

            env.sendEventBean(new SupportBean_S1(-1, "abc"));
            assertTrue(env.listener("s1").isInvoked());

            env.sendEventBean(new SupportBean_S0(100));
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ClientCompileSubstParamResolverContext implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            MySubstitutionOption.getContexts().clear();
            EPCompiled compiled = env.compile("@name('s0') select ?:p0:int as c0 from SupportBean");
            DeploymentOptions options = new DeploymentOptions().setStatementSubstitutionParameter(new MySubstitutionOption());
            options.setDeploymentId("abc");
            try {
                env.deployment().deploy(compiled, options);
                fail();
            } catch (EPDeployException e) {
                // expected
            }
            assertEquals(1, MySubstitutionOption.getContexts().size());
            StatementSubstitutionParameterContext ctx = MySubstitutionOption.getContexts().get(0);
            assertNotNull(ctx.getAnnotations());
            assertEquals("abc", ctx.getDeploymentId());
            assertNotNull(ctx.getEpl());
            assertTrue(ctx.getStatementId() > 0);
            assertEquals("s0", ctx.getStatementName());
            assertEquals(Integer.class, ctx.getSubstitutionParameterTypes()[0]);
            assertEquals((Integer) 1, ctx.getSubstitutionParameterNames().get("p0"));
        }
    }

    private static class ClientCompileSubstParamPrimitiveVsBoxed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled compiled = env.compile("select ?:p0:int as c0, ?:p1:Integer as c1 from SupportBean");
            deployWithResolver(env, compiled, "s0", prepared -> {
                prepared.setObject("p0", 10);
                prepared.setObject("p1", 11);
            });
            env.addListener("s0");

            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1".split(","), new Object[]{10, 11});

            env.undeployAll();
        }
    }

    private static class ClientCompileSubstParamInvalidUse implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // invalid mix or named and unnamed
            tryInvalidCompile(env, "select ? as c0,?:a as c1 from SupportBean",
                "Inconsistent use of substitution parameters, expecting all substitutions to either all provide a name or provide no name");

            // keyword used for name
            tryInvalidCompile(env, "select ?:select from SupportBean",
                "Incorrect syntax near 'select' (a reserved keyword) at line 1 column 9");

            // invalid type incompatible
            tryInvalidCompile(env, "select ?:p0:int as c0, ?:p0:long from SupportBean",
                "Substitution parameter 'p0' incompatible type assignment between types 'java.lang.Integer' and 'java.lang.Long'");
        }
    }

    private static class ClientCompileSubstParamNamedParameter implements RegressionExecution {
        private final boolean soda;

        public ClientCompileSubstParamNamedParameter(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {

            String epl = "@name('s0') select ?:pint:int as c0 from SupportBean(theString=?:pstring:string and intPrimitive=?:pint:int and longPrimitive=?:plong:long)";
            EPCompiled compiled = env.compile(soda, epl, new CompilerArguments(new Configuration()));
            deployWithResolver(env, compiled, null, prepared -> {
                prepared.setObject("pstring", "E1");
                prepared.setObject("pint", 10);
                prepared.setObject("plong", 100L);
            });
            env.addListener("s0");

            SupportBean event = new SupportBean("E1", 10);
            event.setLongPrimitive(100);
            env.sendEventBean(event);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0".split(","), new Object[]{10});

            env.milestone(0);

            env.sendEventBean(event);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0".split(","), new Object[]{10});

            env.undeployAll();
        }
    }

    private static class ClientCompileSubstParamUnnamedParameterWType implements RegressionExecution {
        private final boolean soda;

        public ClientCompileSubstParamUnnamedParameterWType(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            EPCompiled compiled = env.compile(soda, "@name('s0') select * from SupportBean(theString=(?::SupportBean.getTheString()))", new CompilerArguments(new Configuration()));
            deployWithResolver(env, compiled, null, prepared -> prepared.setObject(1, new SupportBean("E1", 0)));
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            assertTrue(env.listener("s0").isInvoked());

            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", 0));
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ClientCompileSubstParamMethodInvocation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled compiled = env.compile("@name('s0') select * from SupportBean(theString = ?:psb:SupportBean.getTheString())");
            deployWithResolver(env, compiled, null, prepared -> prepared.setObject("psb", new SupportBean("E1", 0)));
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ClientCompileSubstParamPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "select * from pattern[SupportBean(theString=?::string)]";
            EPCompiled compiled = env.compile(epl);

            deployWithResolver(env, compiled, "s0", prepared -> prepared.setObject(1, "e1"));
            env.addListener("s0");
            assertEquals(epl, env.statement("s0").getProperty(StatementProperty.EPL));

            deployWithResolver(env, compiled, "s1", prepared -> prepared.setObject(1, "e2"));
            env.addListener("s1");
            assertEquals(epl, env.statement("s1").getProperty(StatementProperty.EPL));

            env.sendEventBean(new SupportBean("e2", 10));
            assertFalse(env.listener("s0").isInvoked());
            assertTrue(env.listener("s1").getAndClearIsInvoked());

            env.sendEventBean(new SupportBean("e1", 10));
            assertFalse(env.listener("s1").isInvoked());
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    private static class ClientCompileSubstParamSubselect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "select (select symbol from SupportMarketDataBean(symbol=?::string)#lastevent) as mysymbol from SupportBean";
            EPCompiled compiled = env.compile(stmtText);

            deployWithResolver(env, compiled, "s0", prepared -> prepared.setObject(1, "S1"));
            env.addListener("s0");

            deployWithResolver(env, compiled, "s1", prepared -> prepared.setObject(1, "S2"));
            env.addListener("s1");

            // test no event, should return null
            env.sendEventBean(new SupportBean("e1", -1));
            assertNull(env.listener("s0").assertOneGetNewAndReset().get("mysymbol"));
            assertNull(env.listener("s1").assertOneGetNewAndReset().get("mysymbol"));

            // test one non-matching event
            env.sendEventBean(new SupportMarketDataBean("XX", 0, 0L, ""));
            env.sendEventBean(new SupportBean("e1", -1));
            assertNull(env.listener("s0").assertOneGetNewAndReset().get("mysymbol"));
            assertNull(env.listener("s1").assertOneGetNewAndReset().get("mysymbol"));

            // test S2 matching event
            env.sendEventBean(new SupportMarketDataBean("S2", 0, 0L, ""));
            env.sendEventBean(new SupportBean("e1", -1));
            assertNull(env.listener("s0").assertOneGetNewAndReset().get("mysymbol"));
            assertEquals("S2", env.listener("s1").assertOneGetNewAndReset().get("mysymbol"));

            // test S1 matching event
            env.sendEventBean(new SupportMarketDataBean("S1", 0, 0L, ""));
            env.sendEventBean(new SupportBean("e1", -1));
            assertEquals("S1", env.listener("s0").assertOneGetNewAndReset().get("mysymbol"));
            assertEquals("S2", env.listener("s1").assertOneGetNewAndReset().get("mysymbol"));

            env.undeployAll();
        }
    }

    private static class ClientCompileSubstParamSimpleOneParameterWCast implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmt = "select * from SupportBean(theString=cast(?, string))";
            EPCompiled compiled = env.compile(stmt);

            deployWithResolver(env, compiled, "s0", prepared -> prepared.setObject(1, "e1"));
            env.addListener("s0");

            deployWithResolver(env, compiled, "s1", prepared -> prepared.setObject(1, "e2"));
            env.addListener("s1");

            env.sendEventBean(new SupportBean("e2", 10));
            assertFalse(env.listener("s0").isInvoked());
            assertTrue(env.listener("s1").getAndClearIsInvoked());

            env.sendEventBean(new SupportBean("e1", 10));
            assertFalse(env.listener("s1").isInvoked());
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    private static class ClientCompileSubstParamWInheritance implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Test substitution parameter and inheritance in key matching
            RegressionPath path = new RegressionPath();
            String types =
                "create schema MyEventOne as " + MyEventOne.class.getName() + ";\n" +
                    "create schema MyEventTwo as " + MyEventTwo.class.getName() + ";\n";
            env.compileDeployWBusPublicType(types, path);

            String epl = "select * from MyEventOne(key = ?::IKey)";
            EPCompiled compiled = env.compile(epl, path);
            MyObjectKeyInterface lKey = new MyObjectKeyInterface();
            deployWithResolver(env, compiled, "s0", prepared -> prepared.setObject(1, lKey));
            env.addListener("s0");

            env.sendEventBean(new MyEventOne(lKey));
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            // Test substitution parameter and concrete subclass in key matching
            epl = "select * from MyEventTwo where key = ?::MyObjectKeyConcrete";
            compiled = env.compile(epl, path);
            MyObjectKeyConcrete cKey = new MyObjectKeyConcrete();
            deployWithResolver(env, compiled, "s1", prepared -> prepared.setObject(1, cKey));
            env.addListener("s1");

            env.sendEventBean(new MyEventTwo(cKey));
            assertTrue(env.listener("s1").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    private static class ClientCompileSubstParamSimpleTwoParameterFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmt = "select * from SupportBean(theString=?::string,intPrimitive=?::int)";
            runSimpleTwoParameter(env, stmt, "A", true);
        }
    }

    private static class ClientCompileSubstParamSimpleTwoParameterWhere implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmt = "select * from SupportBean where theString=?::string and intPrimitive=?::int";
            runSimpleTwoParameter(env, stmt, "B", false);
        }
    }

    private static class ClientCompileSubstParamSimpleNoParameter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled compiled = env.compile("select * from SupportBean(theString=\"e1\")");
            deployWithResolver(env, compiled, "s0", prepared -> {
            });
            env.addListener("s0");

            env.sendEventBean(new SupportBean("e2", 10));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("e1", 10));
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    private static class ClientCompileSubstParamInvalidNoCallback implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidDeployNoCallbackProvided(env, "@name('s0') select * from SupportBean(theString=?::string)");
            tryInvalidDeployNoCallbackProvided(env, "@name('s0') select * from SupportBean(theString=cast(?,string))");
            tryInvalidDeployNoCallbackProvided(env, "@name('s0') select * from SupportBean(theString=?:myname:string)");
        }
    }

    private static class ClientCompileSubstParamInvalidInsufficientValues implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled compiled;

            compiled = env.compile("@name('s0') select * from SupportBean(theString=?::string, intPrimitive=?::int)");
            tryInvalidResolver(env, compiled, "Substitution parameters have not been provided: Missing value for substitution parameter 1 for statement 's0'",
                prepared -> {
                });
            tryInvalidResolver(env, compiled, "Substitution parameters have not been provided: Missing value for substitution parameter 2 for statement 's0'",
                prepared -> prepared.setObject(1, "abc"));

            compiled = env.compile("@name('s0') select * from SupportBean(theString=?:p0:string, intPrimitive=?:p1:int)");
            tryInvalidResolver(env, compiled, "Substitution parameters have not been provided: Missing value for substitution parameter 'p0' for statement 's0'",
                prepared -> {
                });
            tryInvalidResolver(env, compiled, "Substitution parameters have not been provided: Missing value for substitution parameter 'p1' for statement 's0'",
                prepared -> prepared.setObject("p0", "x"));
        }
    }

    private static class ClientCompileSubstParamInvalidParametersUntyped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled compiled;
            DeploymentOptions options;

            compiled = env.compile("select * from SupportBean(theString='ABC')");
            options = new DeploymentOptions().setStatementSubstitutionParameter(prepared -> {
                tryInvalidSetObject(prepared, stmt -> stmt.setObject("x", 10), "The statement has no substitution parameters");
                tryInvalidSetObject(prepared, stmt -> stmt.setObject(1, 10), "The statement has no substitution parameters");
            });
            deployWithOptionsWUndeploy(env, compiled, options);

            // numbered, untyped, casted at eventService
            compiled = env.compile("select * from SupportBean(theString=cast(?, String))");
            options = new DeploymentOptions().setStatementSubstitutionParameter(prepared -> {
                tryInvalidSetObject(prepared, stmt -> stmt.setObject("x", 10), "Substitution parameter names have not been provided for this statement");
                tryInvalidSetObject(prepared, stmt -> stmt.setObject(0, "a"), "Invalid substitution parameter index, expected an index between 1 and 1");
                tryInvalidSetObject(prepared, stmt -> stmt.setObject(2, "a"), "Invalid substitution parameter index, expected an index between 1 and 1");
                prepared.setObject(1, "xxx");
            });
            deployWithOptionsWUndeploy(env, compiled, options);

            // named, untyped, casted at eventService
            compiled = env.compile("select * from SupportBean(theString=cast(?:p0, String))");
            options = new DeploymentOptions().setStatementSubstitutionParameter(prepared -> {
                tryInvalidSetObject(prepared, stmt -> stmt.setObject("x", 10), "Failed to find substitution parameter named 'x', available parameters are [p0]");
                tryInvalidSetObject(prepared, stmt -> stmt.setObject(0, "a"), "Substitution parameter names have been provided for this statement, please set the value by name");
                prepared.setObject("p0", "xxx");
            });
            deployWithOptionsWUndeploy(env, compiled, options);
        }
    }

    private static class ClientCompileSubstParamInvalidParametersTyped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled compiled;
            DeploymentOptions options;

            // numbered, typed
            compiled = env.compile("select * from SupportBean(theString=?::string)");
            options = new DeploymentOptions().setStatementSubstitutionParameter(prepared -> {
                tryInvalidSetObject(prepared, stmt -> stmt.setObject(1, 10), "Failed to set substitution parameter 1, expected a value of type 'java.lang.String': " + SupportJavaVersionUtil.getCastMessage(Integer.class, String.class));
                prepared.setObject(1, "abc");
            });
            deployWithOptionsWUndeploy(env, compiled, options);

            // name, typed
            compiled = env.compile("select * from SupportBean(theString=?:p0:string)");
            options = new DeploymentOptions().setStatementSubstitutionParameter(prepared -> {
                tryInvalidSetObject(prepared, stmt -> stmt.setObject("p0", 10), "Failed to set substitution parameter 'p0', expected a value of type 'java.lang.String': " + SupportJavaVersionUtil.getCastMessage(Integer.class, String.class));
                prepared.setObject("p0", "abc");
            });
            deployWithOptionsWUndeploy(env, compiled, options);

            // name, primitive
            compiled = env.compile("select * from SupportBean(intPrimitive=?:p0:int)");
            options = new DeploymentOptions().setStatementSubstitutionParameter(prepared -> {
                // There is only boxed type consistent with all other column/variable/schema typing:
                // tryInvalidSetObject(prepared, stmt -> stmt.setObject("p0", null), "Failed to set substitution parameter 'p0', expected a value of type 'int': Received a null-value for a primitive type");
                prepared.setObject("p0", 10);
            });
            deployWithOptionsWUndeploy(env, compiled, options);
        }
    }

    private static void runSimpleTwoParameter(RegressionEnvironment env, String stmtText, String statementName, boolean compareText) {
        EPCompiled compiled = env.compile(stmtText);

        deployWithResolver(env, compiled, statementName, prepared -> {
            prepared.setObject(1, "e1");
            prepared.setObject(2, 1);
        });
        env.addListener(statementName);
        if (compareText) {
            assertEquals("select * from SupportBean(theString=?::string,intPrimitive=?::int)", env.statement(statementName).getProperty(StatementProperty.EPL));
        }

        deployWithResolver(env, compiled, statementName + "__1", prepared -> {
            prepared.setObject(1, "e2");
            prepared.setObject(2, 2);
        });
        env.addListener(statementName + "__1");
        if (compareText) {
            assertEquals("select * from SupportBean(theString=?::string,intPrimitive=?::int)", env.statement(statementName + "__1").getProperty(StatementProperty.EPL));
        }

        env.sendEventBean(new SupportBean("e2", 2));
        assertFalse(env.listener(statementName).isInvoked());
        assertTrue(env.listener(statementName + "__1").getAndClearIsInvoked());

        env.sendEventBean(new SupportBean("e1", 1));
        assertFalse(env.listener(statementName + "__1").isInvoked());
        assertTrue(env.listener(statementName).getAndClearIsInvoked());

        env.sendEventBean(new SupportBean("e1", 2));
        assertFalse(env.listener(statementName).isInvoked());
        assertFalse(env.listener(statementName + "__1").isInvoked());

        env.undeployAll();
    }

    private static void deployWithResolver(RegressionEnvironment env, EPCompiled compiled, String statementName, StatementSubstitutionParameterOption resolver) {
        DeploymentOptions options = new DeploymentOptions().setStatementSubstitutionParameter(resolver);
        options.setStatementNameRuntime(context -> statementName);
        try {
            env.deployment().deploy(compiled, options);
        } catch (EPDeployException e) {
            throw new RuntimeException(e);
        }
    }

    private static void tryInvalidDeployNoCallbackProvided(RegressionEnvironment env, String stmt) {
        EPCompiled compiled = env.compile(stmt);
        try {
            env.deployment().deploy(compiled);
            fail();
        } catch (EPDeploySubstitutionParameterException ex) {
            assertEquals("Substitution parameters have not been provided: Statement 's0' has 1 substitution parameters", ex.getMessage());
        } catch (EPDeployException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void tryInvalidSetObject(StatementSubstitutionParameterContext prepared, Consumer<StatementSubstitutionParameterContext> consumer, String message) {
        try {
            consumer.accept(prepared);
            fail();
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex, message);
        }
    }

    private static void tryInvalidResolver(RegressionEnvironment env, EPCompiled compiled, String expected, StatementSubstitutionParameterOption resolver) {
        DeploymentOptions options = new DeploymentOptions().setStatementSubstitutionParameter(resolver);
        try {
            env.deployment().deploy(compiled, options);
            fail();
        } catch (EPDeploySubstitutionParameterException e) {
            SupportMessageAssertUtil.assertMessage(e.getMessage(), expected);
        } catch (EPDeployException e) {
            throw new RuntimeException(e);
        }
    }

    private static void deployWithOptionsWUndeploy(RegressionEnvironment env, EPCompiled compiled, DeploymentOptions options) {
        env.deploy(compiled, options).undeployAll();
    }

    public static class MySubstitutionOption implements StatementSubstitutionParameterOption {
        private static List<StatementSubstitutionParameterContext> contexts = new ArrayList<>();

        public static List<StatementSubstitutionParameterContext> getContexts() {
            return contexts;
        }

        public void setStatementParameters(StatementSubstitutionParameterContext env) {
            contexts.add(env);
        }
    }

    public interface IKey extends Serializable {
    }

    public static class MyObjectKeyInterface implements IKey {
    }

    public static class MyEventOne {
        private IKey key;

        public MyEventOne(IKey key) {
            this.key = key;
        }

        public IKey getKey() {
            return key;
        }
    }

    public static class MyObjectKeyConcrete implements Serializable {
    }

    public static class MyEventTwo implements Serializable {
        private MyObjectKeyConcrete key;

        public MyEventTwo(MyObjectKeyConcrete key) {
            this.key = key;
        }

        public MyObjectKeyConcrete getKey() {
            return key;
        }
    }
}
