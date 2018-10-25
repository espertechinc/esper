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
package com.espertech.esper.regressionlib.suite.infra.nwtable;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQueryParameterized;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportJavaVersionUtil;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.assertMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class InfraNWTableFAFSubstitutionParams implements IndexBackingTableInfo {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraParameterizedQuery(true));
        execs.add(new InfraParameterizedQuery(false));
        execs.add(new InfraParameterizedQueryNamedParameter());
        execs.add(new InfraParameterizedQueryInvalidUse());
        execs.add(new InfraParameterizedQueryInvalidInsufficientValues());
        execs.add(new InfraParameterizedQueryInvalidParametersUntyped());
        execs.add(new InfraParameterizedQueryInvalidParametersTyped());
        return execs;
    }

    private static class InfraParameterizedQueryNamedParameter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = setupInfra(env, true);
            EPFireAndForgetPreparedQueryParameterized query;

            String eplOneParam = "select * from MyInfra where intPrimitive = ?:p0:int";
            runParameterizedQueryWCompile(env, path, eplOneParam, Collections.singletonMap("p0", 5), new String[]{"E5"});

            String eplTwiceUsed = "select * from MyInfra where intPrimitive = ?:p0:int or intBoxed = ?:p0:int";
            runParameterizedQueryWCompile(env, path, eplTwiceUsed, Collections.singletonMap("p0", 12), new String[]{"E2"});

            String eplTwoParam = "select * from MyInfra where intPrimitive = ?:p1:int and intBoxed = ?:p0:int";
            query = compilePrepare(eplTwoParam, path, env);
            runParameterizedQuery(env, query, CollectionUtil.populateNameValueMap("p0", 13, "p1", 3), new String[]{"E3"});
            runParameterizedQuery(env, query, CollectionUtil.populateNameValueMap("p0", 3, "p1", 3), new String[0]);
            runParameterizedQuery(env, query, CollectionUtil.populateNameValueMap("p0", 13, "p1", 13), new String[0]);

            env.undeployAll();
        }
    }

    private static class InfraParameterizedQueryInvalidUse implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window MyWindow#keepall as SupportBean", path);

            // invalid mix or named and unnamed
            tryInvalidCompileFAF(env, path, "select ? as c0,?:a as c1 from MyWindow",
                "Inconsistent use of substitution parameters, expecting all substitutions to either all provide a name or provide no name");

            // keyword used for name
            tryInvalidCompileFAF(env, path, "select ?:select from MyWindow",
                "Incorrect syntax near 'select' (a reserved keyword) at line 1 column 9");

            // invalid type incompatible
            tryInvalidCompileFAF(env, path, "select ?:p0:int as c0, ?:p0:long from MyWindow",
                "Substitution parameter 'p0' incompatible type assignment between types 'java.lang.Integer' and 'java.lang.Long'");

            env.undeployAll();
        }
    }

    private static class InfraParameterizedQueryInvalidInsufficientValues implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window MyWindow#keepall as SupportBean", path);

            // invalid execute without prepare-params
            EPCompiled compiled = env.compileFAF("select * from MyWindow where theString=?::string", path);
            try {
                env.runtime().getFireAndForgetService().executeQuery(compiled);
                fail();
            } catch (EPException ex) {
                SupportMessageAssertUtil.assertMessage(ex.getMessage(), "Missing values for substitution parameters, use prepare-parameterized instead");
            }

            // invalid prepare without prepare-params
            try {
                env.runtime().getFireAndForgetService().prepareQuery(compiled);
                fail();
            } catch (EPException ex) {
                SupportMessageAssertUtil.assertMessage(ex.getMessage(), "Missing values for substitution parameters, use prepare-parameterized instead");
            }

            // missing params
            tryInvalidlyParameterized(env, compiled, query -> {
            }, "Missing value for substitution parameter 1");

            compiled = env.compileFAF("select * from MyWindow where theString=?::string and intPrimitive=?::int", path);
            tryInvalidlyParameterized(env, compiled, query -> {
            }, "Missing value for substitution parameter 1");
            tryInvalidlyParameterized(env, compiled, query -> {
                query.setObject(1, "a");
            }, "Missing value for substitution parameter 2");

            compiled = env.compileFAF("select * from MyWindow where theString=?:p0:string and intPrimitive=?:p1:int", path);
            tryInvalidlyParameterized(env, compiled, query -> {
            }, "Missing value for substitution parameter 'p0'");
            tryInvalidlyParameterized(env, compiled, query -> {
                query.setObject("p0", "a");
            }, "Missing value for substitution parameter 'p1");

            env.undeployAll();
        }
    }

    private static class InfraParameterizedQueryInvalidParametersUntyped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window MyWindow#keepall as SupportBean", path);

            EPCompiled compiled = env.compileFAF("select * from MyWindow where theString='ABC'", path);
            tryInvalidSetObject(env, compiled, query -> query.setObject("x", 10), "The query has no substitution parameters");
            tryInvalidSetObject(env, compiled, query -> query.setObject("1", 10), "The query has no substitution parameters");

            // numbered, untyped, casted at eventService
            compiled = env.compileFAF("select * from MyWindow where theString=cast(?, String)", path);
            tryInvalidSetObject(env, compiled, query -> query.setObject("x", 10), "Substitution parameter names have not been provided for this query");
            tryInvalidSetObject(env, compiled, query -> query.setObject(0, "a"), "Invalid substitution parameter index, expected an index between 1 and 1");
            tryInvalidSetObject(env, compiled, query -> query.setObject(2, "a"), "Invalid substitution parameter index, expected an index between 1 and 1");

            // named, untyped, casted at eventService
            compiled = env.compileFAF("select * from MyWindow where theString=cast(?:p0, String)", path);
            tryInvalidSetObject(env, compiled, query -> query.setObject("x", 10), "Failed to find substitution parameter named 'x', available parameters are [p0]");
            tryInvalidSetObject(env, compiled, query -> query.setObject(0, "a"), "Substitution parameter names have been provided for this query, please set the value by name");

            env.undeployAll();
        }
    }

    private static class InfraParameterizedQueryInvalidParametersTyped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window MyWindow#keepall as SupportBean", path);
            EPCompiled compiled;

            // numbered, typed
            compiled = env.compileFAF("select * from MyWindow where theString=?::string", path);
            tryInvalidSetObject(env, compiled, query -> query.setObject(1, 10), "Failed to set substitution parameter 1, expected a value of type 'java.lang.String': " + SupportJavaVersionUtil.getCastMessage(Integer.class, String.class));

            // name, typed
            compiled = env.compileFAF("select * from MyWindow where theString=?:p0:string", path);
            tryInvalidSetObject(env, compiled, query -> query.setObject("p0", 10), "Failed to set substitution parameter 'p0', expected a value of type 'java.lang.String': " + SupportJavaVersionUtil.getCastMessage(Integer.class, String.class));

            // consistent with variables/schema/table-column the "int" becomes "Integer" as a type and there is no fail checking for null

            env.undeployAll();
        }
    }

    private static class InfraParameterizedQuery implements RegressionExecution {
        private final boolean namedWindow;

        public InfraParameterizedQuery(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {

            RegressionPath path = setupInfra(env, namedWindow);

            // test one parameter
            String eplOneParam = "select * from MyInfra where intPrimitive = ?::int";
            EPFireAndForgetPreparedQueryParameterized pqOneParam = compilePrepare(eplOneParam, path, env);
            for (int i = 0; i < 10; i++) {
                runParameterizedQuery(env, pqOneParam, new Object[]{i}, new String[]{"E" + i});
            }
            runParameterizedQuery(env, pqOneParam, new Object[]{-1}, null); // not found

            // test two parameter
            String eplTwoParam = "select * from MyInfra where intPrimitive = ?::int and longPrimitive = ?::long";
            EPFireAndForgetPreparedQueryParameterized pqTwoParam = compilePrepare(eplTwoParam, path, env);
            for (int i = 0; i < 10; i++) {
                runParameterizedQuery(env, pqTwoParam, new Object[]{i, (long) i * 1000}, new String[]{"E" + i});
            }
            runParameterizedQuery(env, pqTwoParam, new Object[]{-1, 1000L}, null); // not found

            // test in-clause with string objects
            String eplInSimple = "select * from MyInfra where theString in (?::string, ?::string, ?::string)";
            EPFireAndForgetPreparedQueryParameterized pqInSimple = compilePrepare(eplInSimple, path, env);
            runParameterizedQuery(env, pqInSimple, new Object[]{"A", "A", "A"}, null); // not found
            runParameterizedQuery(env, pqInSimple, new Object[]{"A", "E3", "A"}, new String[]{"E3"});

            // test in-clause with string array
            String eplInArray = "select * from MyInfra where theString in (?::string[])";
            EPFireAndForgetPreparedQueryParameterized pqInArray = compilePrepare(eplInArray, path, env);
            runParameterizedQuery(env, pqInArray, new Object[]{new String[]{"E3", "E6", "E8"}}, new String[]{"E3", "E6", "E8"});

            // various combinations
            runParameterizedQuery(env, compilePrepare("select * from MyInfra where theString in (?::string[]) and longPrimitive = 4000", path, env),
                new Object[]{new String[]{"E3", "E4", "E8"}}, new String[]{"E4"});
            runParameterizedQuery(env, compilePrepare("select * from MyInfra where longPrimitive > 8000", path, env),
                new Object[]{}, new String[]{"E9"});
            runParameterizedQuery(env, compilePrepare("select * from MyInfra where longPrimitive < ?::long", path, env),
                new Object[]{2000L}, new String[]{"E0", "E1"});
            runParameterizedQuery(env, compilePrepare("select * from MyInfra where longPrimitive between ?::int and ?::int", path, env),
                new Object[]{2000, 4000}, new String[]{"E2", "E3", "E4"});

            env.undeployAll();
        }
    }

    private static RegressionPath setupInfra(RegressionEnvironment env, boolean namedWindow) {
        RegressionPath path = new RegressionPath();
        String eplCreate = namedWindow ?
            "@Name('TheInfra') create window MyInfra#keepall as select * from SupportBean" :
            "@Name('TheInfra') create table MyInfra as (theString string primary key, intPrimitive int primary key, longPrimitive long)";
        env.compileDeploy(eplCreate, path);
        String eplInsert = namedWindow ?
            "@Name('Insert') insert into MyInfra select * from SupportBean" :
            "@Name('Insert') on SupportBean sb merge MyInfra mi where mi.theString = sb.theString and mi.intPrimitive=sb.intPrimitive" +
                " when not matched then insert select theString, intPrimitive, longPrimitive";
        env.compileDeploy(eplInsert, path);

        for (int i = 0; i < 10; i++) {
            env.sendEventBean(makeBean("E" + i, i, i * 1000));
        }

        return path;
    }

    private static SupportBean makeBean(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        bean.setIntBoxed(10 + intPrimitive);
        return bean;
    }

    private static void runParameterizedQueryWCompile(RegressionEnvironment env, RegressionPath path, String eplOneParam, Map<String, Object> params, String[] expected) {
        EPFireAndForgetPreparedQueryParameterized query = compilePrepare(eplOneParam, path, env);
        runParameterizedQuery(env, query, params, expected);
    }

    private static void runParameterizedQuery(RegressionEnvironment env, EPFireAndForgetPreparedQueryParameterized parameterizedQuery, Object[] parameters, String[] expected) {

        for (int i = 0; i < parameters.length; i++) {
            parameterizedQuery.setObject(i + 1, parameters[i]);
        }
        runAndAssertResults(env, parameterizedQuery, expected);
    }

    private static void runParameterizedQuery(RegressionEnvironment env, EPFireAndForgetPreparedQueryParameterized parameterizedQuery, Map<String, Object> parameters, String[] expected) {

        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            parameterizedQuery.setObject(entry.getKey(), entry.getValue());
        }
        runAndAssertResults(env, parameterizedQuery, expected);
    }

    private static void runAndAssertResults(RegressionEnvironment env, EPFireAndForgetPreparedQueryParameterized parameterizedQuery, String[] expected) {
        EPFireAndForgetQueryResult result = env.runtime().getFireAndForgetService().executeQuery(parameterizedQuery);
        if (expected == null) {
            assertEquals(0, result.getArray().length);
        } else {
            assertEquals(expected.length, result.getArray().length);
            String[] resultStrings = new String[result.getArray().length];
            for (int i = 0; i < resultStrings.length; i++) {
                resultStrings[i] = (String) result.getArray()[i].get("theString");
            }
            EPAssertionUtil.assertEqualsAnyOrder(expected, resultStrings);
        }
    }

    private static EPFireAndForgetPreparedQueryParameterized compilePrepare(String faf, RegressionPath path, RegressionEnvironment env) {
        EPCompiled compiled = env.compileFAF(faf, path);
        return env.runtime().getFireAndForgetService().prepareQueryWithParameters(compiled);
    }

    private static void tryInvalidCompileFAF(RegressionEnvironment env, RegressionPath path, String faf, String expected) {
        try {
            CompilerArguments args = new CompilerArguments(env.getConfiguration());
            args.getPath().addAll(path.getCompileds());
            EPCompilerProvider.getCompiler().compileQuery(faf, args);
            fail();
        } catch (EPCompileException ex) {
            assertMessage(ex, expected);
        }
    }

    private static void tryInvalidlyParameterized(RegressionEnvironment env, EPCompiled compiled, Consumer<EPFireAndForgetPreparedQueryParameterized> query, String message) {
        EPFireAndForgetPreparedQueryParameterized parameterized = env.runtime().getFireAndForgetService().prepareQueryWithParameters(compiled);
        query.accept(parameterized);
        try {
            env.runtime().getFireAndForgetService().executeQuery(parameterized);
            fail();
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex.getMessage(), message);
        }
    }

    private static void tryInvalidSetObject(RegressionEnvironment env, EPCompiled compiled, Consumer<EPFireAndForgetPreparedQueryParameterized> query, String message) {
        EPFireAndForgetPreparedQueryParameterized parameterized = env.runtime().getFireAndForgetService().prepareQueryWithParameters(compiled);
        try {
            query.accept(parameterized);
            fail();
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex.getMessage(), message);
        }
    }
}

