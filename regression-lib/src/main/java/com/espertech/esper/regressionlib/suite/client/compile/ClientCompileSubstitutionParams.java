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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.module.Module;
import com.espertech.esper.common.client.module.ModuleItem;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportJavaVersionUtil;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.regressionlib.framework.*;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.regressionlib.support.client.SupportPortableDeployStatementName;
import com.espertech.esper.regressionlib.support.client.SupportPortableDeploySubstitutionParams;
import com.espertech.esper.runtime.client.DeploymentOptions;
import com.espertech.esper.runtime.client.EPDeployException;
import com.espertech.esper.runtime.client.EPDeploySubstitutionParameterException;
import com.espertech.esper.runtime.client.option.StatementSubstitutionParameterContext;
import com.espertech.esper.runtime.client.option.StatementSubstitutionParameterOption;
import org.junit.Assert;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

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
        execs.add(new ClientCompileSODAInvalidConstantUseSubsParamsInstead());
        execs.add(new ClientCompileSubstParamGenericType(false));
        execs.add(new ClientCompileSubstParamGenericType(true));
        return execs;
    }

    private static class ClientCompileSubstParamGenericType implements RegressionExecution {
        private final boolean soda;

        public ClientCompileSubstParamGenericType(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "?:a0:java.util.List<String> as c0, " +
                "?:a1:java.util.Map<String,Integer> as c1 " +
                "from SupportBean";
            EPCompiled compiled = env.compile(soda, epl, new CompilerArguments(env.getConfiguration()));

            SupportPortableDeploySubstitutionParams param = new SupportPortableDeploySubstitutionParams();
            param.add("a0", new ArrayList<>(Arrays.asList("a"))).add("a1", Collections.singletonMap("k1", 10));
            DeploymentOptions options = new DeploymentOptions().setStatementSubstitutionParameter(param);
            env.deploy(compiled, options).addListener("s0");

            env.assertStatement("s0", statement -> {
                EventType eventType = statement.getEventType();
                assertEquals(EPTypeClassParameterized.from(List.class, String.class), eventType.getPropertyEPType("c0"));
                assertEquals(EPTypeClassParameterized.from(Map.class, String.class, Integer.class), eventType.getPropertyEPType("c1"));
            });

            env.sendEventBean(new SupportBean());

            env.assertEventNew("s0", event -> {
                EPAssertionUtil.assertEqualsExactOrder(new Object[]{"a"}, ((List) event.get("c0")).toArray());
                EPAssertionUtil.assertPropsMap((Map) event.get("c1"), "k1".split(","), 10);
            });

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "soda=" + soda +
                '}';
        }
    }

    private static class ClientCompileSODAInvalidConstantUseSubsParamsInstead implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            Expression expression = Expressions.eq(
                Expressions.property("object"),
                Expressions.constant(new Object())
            );

            EPStatementObjectModel model = new EPStatementObjectModel()
                .selectClause(SelectClause.createWildcard())
                .fromClause(FromClause.create(FilterStream.create("SupportObjectCtor", expression)));
            try {
                Module module = new Module();
                module.getItems().add(new ModuleItem(model));
                env.getCompiler().compile(module, new CompilerArguments(env.getConfiguration()));
                fail();
            } catch (EPCompileException ex) {
                SupportMessageAssertUtil.assertMessage(ex, "Exception processing statement: Invalid constant of type 'Object' encountered as the class has no compiler representation, please use substitution parameters instead");
            }
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.INVALIDITY);
        }
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

            SupportPortableDeploySubstitutionParams params = new SupportPortableDeploySubstitutionParams();
            params.add("a0", new Integer[]{1, 2}).add("a1", new int[]{3, 4}).add("a2", new Object[]{"a", "b"})
                .add("a3", new String[][]{{"A"}}).add("a4", new Object[][]{{5, 6}});
            DeploymentOptions options = new DeploymentOptions().setStatementSubstitutionParameter(params);
            env.deploy(compiled, options).addListener("s0");

            env.assertStatement("s0", statement -> {
                EventType eventType = statement.getEventType();
                assertEquals(Integer[].class, eventType.getPropertyType("c0"));
                assertEquals(int[].class, eventType.getPropertyType("c1"));
                assertEquals(Object[].class, eventType.getPropertyType("c2"));
                assertEquals(String[][].class, eventType.getPropertyType("c3"));
                assertEquals(Object[][].class, eventType.getPropertyType("c4"));
            });

            env.sendEventBean(new SupportBean());

            env.assertEventNew("s0", event -> {
                EPAssertionUtil.assertEqualsExactOrder(new Integer[]{1, 2}, (Integer[]) event.get("c0"));
                EPAssertionUtil.assertEqualsExactOrder(new int[]{3, 4}, (int[]) event.get("c1"));
                EPAssertionUtil.assertEqualsExactOrder(new Object[]{"a", "b"}, (Object[]) event.get("c2"));
                EPAssertionUtil.assertEqualsExactOrder(new String[][]{{"A"}}, (String[][]) event.get("c3"));
                EPAssertionUtil.assertEqualsExactOrder(new Object[][]{{5, 6}}, (Object[][]) event.get("c4"));
            });

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "soda=" + soda +
                '}';
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
            env.assertListenerInvoked("s1");

            env.sendEventBean(new SupportBean_S0(100));
            env.assertListenerInvoked("s0");

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.RUNTIMEOPS);
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

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.STATICHOOK);
        }
    }

    private static class ClientCompileSubstParamPrimitiveVsBoxed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled compiled = env.compile("select ?:p0:int as c0, ?:p1:Integer as c1 from SupportBean");
            deployWithResolver(env, compiled, "s0", new SupportPortableDeploySubstitutionParams().add("p0", 10).add("p1", 11));
            env.addListener("s0");

            env.sendEventBean(new SupportBean());
            env.assertPropsNew("s0", "c0,c1".split(","), new Object[]{10, 11});

            env.undeployAll();
        }
    }

    private static class ClientCompileSubstParamInvalidUse implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // invalid mix or named and unnamed
            env.tryInvalidCompile("select ? as c0,?:a as c1 from SupportBean",
                "Inconsistent use of substitution parameters, expecting all substitutions to either all provide a name or provide no name");

            // keyword used for name
            env.tryInvalidCompile("select ?:select from SupportBean",
                "Incorrect syntax near 'select' (a reserved keyword) at line 1 column 9");

            // invalid type incompatible
            env.tryInvalidCompile("select ?:p0:int as c0, ?:p0:long from SupportBean",
                "Substitution parameter 'p0' incompatible type assignment between types 'Integer' and 'Long'");
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
            deployWithResolver(env, compiled, null, new SupportPortableDeploySubstitutionParams().add("pstring", "E1").add("pint", 10).add("plong", 100L));
            env.addListener("s0");

            SupportBean event = new SupportBean("E1", 10);
            event.setLongPrimitive(100);
            env.sendEventBean(event);
            env.assertPropsNew("s0", "c0".split(","), new Object[]{10});

            env.milestone(0);

            env.sendEventBean(event);
            env.assertPropsNew("s0", "c0".split(","), new Object[]{10});

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "soda_" + soda;
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.COMPILEROPS);
        }
    }

    private static class ClientCompileSubstParamUnnamedParameterWType implements RegressionExecution {
        private final boolean soda;

        public ClientCompileSubstParamUnnamedParameterWType(boolean soda) {
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {
            EPCompiled compiled = env.compile(soda, "@name('s0') select * from SupportBean(theString=(?::SupportBean.getTheString()))", new CompilerArguments(new Configuration()));
            deployWithResolver(env, compiled, null, new SupportPortableDeploySubstitutionParams().add(1, new SupportBean("E1", 0)));
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            env.assertListenerInvoked("s0");

            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", 0));
            env.assertListenerInvoked("s0");

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "soda=" + soda +
                '}';
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.COMPILEROPS);
        }
    }

    private static class ClientCompileSubstParamMethodInvocation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled compiled = env.compile("@name('s0') select * from SupportBean(theString = ?:psb:SupportBean.getTheString())");
            deployWithResolver(env, compiled, null, new SupportPortableDeploySubstitutionParams("psb", new SupportBean("E1", 0)));
            env.addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            env.assertListenerInvoked("s0");

            env.undeployAll();
        }
    }

    private static class ClientCompileSubstParamPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "select * from pattern[SupportBean(theString=?::string)]";
            EPCompiled compiled = env.compile(epl);

            deployWithResolver(env, compiled, "s0", new SupportPortableDeploySubstitutionParams(1, "e1"));
            env.addListener("s0");
            env.assertStatement("s0", statement -> assertEquals(epl, statement.getProperty(StatementProperty.EPL)));

            deployWithResolver(env, compiled, "s1", new SupportPortableDeploySubstitutionParams(1, "e2"));
            env.addListener("s1");
            env.assertStatement("s1", statement -> assertEquals(epl, statement.getProperty(StatementProperty.EPL)));

            env.sendEventBean(new SupportBean("e2", 10));
            env.assertListenerNotInvoked("s0");
            env.assertListenerInvoked("s1");

            env.sendEventBean(new SupportBean("e1", 10));
            env.assertListenerNotInvoked("s1");
            env.assertListenerInvoked("s0");

            env.undeployAll();
        }
    }

    private static class ClientCompileSubstParamSubselect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "select (select symbol from SupportMarketDataBean(symbol=?::string)#lastevent) as mysymbol from SupportBean";
            EPCompiled compiled = env.compile(stmtText);

            deployWithResolver(env, compiled, "s0", new SupportPortableDeploySubstitutionParams(1, "S1"));
            env.addListener("s0");

            deployWithResolver(env, compiled, "s1", new SupportPortableDeploySubstitutionParams(1, "S2"));
            env.addListener("s1");

            // test no event, should return null
            env.sendEventBean(new SupportBean("e1", -1));
            env.assertEqualsNew("s0", "mysymbol", null);
            env.assertEqualsNew("s1", "mysymbol", null);

            // test one non-matching event
            env.sendEventBean(new SupportMarketDataBean("XX", 0, 0L, ""));
            env.sendEventBean(new SupportBean("e1", -1));
            env.assertEqualsNew("s0", "mysymbol", null);
            env.assertEqualsNew("s1", "mysymbol", null);

            // test S2 matching event
            env.sendEventBean(new SupportMarketDataBean("S2", 0, 0L, ""));
            env.sendEventBean(new SupportBean("e1", -1));
            env.assertEqualsNew("s0", "mysymbol", null);
            env.assertEqualsNew("s1", "mysymbol", "S2");

            // test S1 matching event
            env.sendEventBean(new SupportMarketDataBean("S1", 0, 0L, ""));
            env.sendEventBean(new SupportBean("e1", -1));
            env.assertEqualsNew("s0", "mysymbol", "S1");
            env.assertEqualsNew("s1", "mysymbol", "S2");

            env.undeployAll();
        }
    }

    private static class ClientCompileSubstParamSimpleOneParameterWCast implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmt = "select * from SupportBean(theString=cast(?, string))";
            EPCompiled compiled = env.compile(stmt);

            deployWithResolver(env, compiled, "s0", new SupportPortableDeploySubstitutionParams(1, "e1"));
            env.addListener("s0");

            deployWithResolver(env, compiled, "s1", new SupportPortableDeploySubstitutionParams(1, "e2"));
            env.addListener("s1");

            env.sendEventBean(new SupportBean("e2", 10));
            env.assertListenerNotInvoked("s0");
            env.assertListenerInvoked("s1");

            env.sendEventBean(new SupportBean("e1", 10));
            env.assertListenerNotInvoked("s1");
            env.assertListenerInvoked("s0");

            env.undeployAll();
        }
    }

    private static class ClientCompileSubstParamWInheritance implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Test substitution parameter and inheritance in key matching
            RegressionPath path = new RegressionPath();
            String types =
                    "@public @buseventtype create schema MyEventOne as " + MyEventOne.class.getName() + ";\n" +
                    "@public @buseventtype create schema MyEventTwo as " + MyEventTwo.class.getName() + ";\n";
            env.compileDeploy(types, path);

            String epl = "select * from MyEventOne(key = ?::IKey)";
            EPCompiled compiled = env.compile(epl, path);
            MyObjectKeyInterface lKey = new MyObjectKeyInterface();
            deployWithResolver(env, compiled, "s0", new SupportPortableDeploySubstitutionParams(1, lKey));
            env.addListener("s0");

            env.sendEventBean(new MyEventOne(lKey));
            env.assertListenerInvoked("s0");

            // Test substitution parameter and concrete subclass in key matching
            epl = "select * from MyEventTwo where key = ?::MyObjectKeyConcrete";
            compiled = env.compile(epl, path);
            MyObjectKeyConcrete cKey = new MyObjectKeyConcrete();
            deployWithResolver(env, compiled, "s1", new SupportPortableDeploySubstitutionParams(1, cKey));
            env.addListener("s1");

            env.sendEventBean(new MyEventTwo(cKey));
            env.assertListenerInvoked("s1");

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
            deployWithResolver(env, compiled, "s0", new SupportPortableDeploySubstitutionParams());
            env.addListener("s0");

            env.sendEventBean(new SupportBean("e2", 10));
            env.assertListenerNotInvoked("s0");

            env.sendEventBean(new SupportBean("e1", 10));
            env.assertListenerInvoked("s0");

            env.undeployAll();
        }
    }

    private static class ClientCompileSubstParamInvalidNoCallback implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidDeployNoCallbackProvided(env, "@name('s0') select * from SupportBean(theString=?::string)");
            tryInvalidDeployNoCallbackProvided(env, "@name('s0') select * from SupportBean(theString=cast(?,string))");
            tryInvalidDeployNoCallbackProvided(env, "@name('s0') select * from SupportBean(theString=?:myname:string)");
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.INVALIDITY);
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

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.INVALIDITY);
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

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.INVALIDITY);
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

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.INVALIDITY);
        }
    }

    private static void runSimpleTwoParameter(RegressionEnvironment env, String stmtText, String statementName, boolean compareText) {
        EPCompiled compiled = env.compile(stmtText);

        deployWithResolver(env, compiled, statementName, new SupportPortableDeploySubstitutionParams(1, "e1", 2, 1));
        env.addListener(statementName);
        if (compareText) {
            env.assertStatement(statementName, statement -> assertEquals("select * from SupportBean(theString=?::string,intPrimitive=?::int)", statement.getProperty(StatementProperty.EPL)));
        }

        deployWithResolver(env, compiled, statementName + "__1", new SupportPortableDeploySubstitutionParams().add(1, "e2").add(2, 2));
        env.addListener(statementName + "__1");
        if (compareText) {
            env.assertStatement(statementName + "__1", statement -> assertEquals("select * from SupportBean(theString=?::string,intPrimitive=?::int)", statement.getProperty(StatementProperty.EPL)));
        }

        env.sendEventBean(new SupportBean("e2", 2));
        env.assertListenerNotInvoked(statementName);
        env.assertListenerInvoked(statementName + "__1");

        env.sendEventBean(new SupportBean("e1", 1));
        env.assertListenerInvoked(statementName);
        env.assertListenerNotInvoked(statementName + "__1");

        env.sendEventBean(new SupportBean("e1", 2));
        env.assertListenerNotInvoked(statementName);
        env.assertListenerNotInvoked(statementName + "__1");

        env.undeployAll();
    }

    private static void deployWithResolver(RegressionEnvironment env, EPCompiled compiled, String statementName, SupportPortableDeploySubstitutionParams resolver) {
        DeploymentOptions options = new DeploymentOptions().setStatementSubstitutionParameter(resolver);
        options.setStatementNameRuntime(new SupportPortableDeployStatementName(statementName));
        env.deploy(compiled, options);
    }

    private static void tryInvalidDeployNoCallbackProvided(RegressionEnvironment env, String stmt) {
        EPCompiled compiled = env.compile(stmt);
        try {
            env.deployment().deploy(compiled);
            fail();
        } catch (EPDeploySubstitutionParameterException ex) {
            assertEquals(-1, ex.getRolloutItemNumber());
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

    /**
     * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
     */
    public static class MyObjectKeyInterface implements IKey {
        private static final long serialVersionUID = 7269809189145438135L;
    }

    /**
     * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
     */
    public static class MyEventOne implements Serializable {
        private static final long serialVersionUID = 188967224166816051L;
        private IKey key;

        public MyEventOne(IKey key) {
            this.key = key;
        }

        public IKey getKey() {
            return key;
        }
    }

    /**
     * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
     */
    public static class MyObjectKeyConcrete implements Serializable {
        private static final long serialVersionUID = -1433373824226539405L;
    }

    /**
     * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
     */
    public static class MyEventTwo implements Serializable {
        private static final long serialVersionUID = -8374901696890458360L;
        private MyObjectKeyConcrete key;

        public MyEventTwo(MyObjectKeyConcrete key) {
            this.key = key;
        }

        public MyObjectKeyConcrete getKey() {
            return key;
        }
    }
}
