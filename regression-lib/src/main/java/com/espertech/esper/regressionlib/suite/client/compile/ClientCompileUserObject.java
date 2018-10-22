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
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.option.StatementUserObjectContext;
import com.espertech.esper.compiler.client.option.StatementUserObjectOption;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ClientCompileUserObject {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientCompileUserObjectDifferentTypes());
        execs.add(new ClientCompileUserObjectResolveContextInfo());
        return execs;
    }

    private static class ClientCompileUserObjectResolveContextInfo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            MyUserObjectResolver.getContexts().clear();
            CompilerArguments args = new CompilerArguments(env.getConfiguration());
            args.getOptions().setStatementUserObject(new MyUserObjectResolver());
            String epl = "@name('s0') select * from SupportBean";
            env.compile(epl, args);

            StatementUserObjectContext ctx = MyUserObjectResolver.getContexts().get(0);
            assertEquals(epl, ctx.getEplSupplier().get());
            assertEquals("s0", ctx.getStatementName());
            assertEquals(null, ctx.getModuleName());
            assertEquals(1, ctx.getAnnotations().length);
            assertEquals(0, ctx.getStatementNumber());
        }
    }

    private static class ClientCompileUserObjectDifferentTypes implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            assertUserObject(env, "ABC");
            assertUserObject(env, new int[]{1, 2, 3});
            assertUserObject(env, null);
            assertUserObject(env, new MyUserObject("hello"));
        }
    }

    private static void assertUserObject(RegressionEnvironment env, Serializable userObject) {
        CompilerArguments args = new CompilerArguments(env.getConfiguration());
        args.getOptions().setStatementUserObject(new StatementUserObjectOption() {
            public Serializable getValue(StatementUserObjectContext env) {
                return userObject;
            }
        });
        EPCompiled compiled = env.compile("@name('s0') select * from SupportBean", args);
        env.deploy(compiled);
        Object received = env.statement("s0").getUserObjectCompileTime();
        if (received == null) {
            assertNull(userObject);
        } else if (received.getClass() == int[].class) {
            assertTrue(Arrays.equals((int[]) received, (int[]) userObject));
        } else {
            assertEquals(userObject, env.statement("s0").getUserObjectCompileTime());
        }

        env.undeployAll();
    }

    private static class MyUserObject implements Serializable {
        private String id;

        public MyUserObject(String id) {
            this.id = id;
        }

        public MyUserObject() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MyUserObject that = (MyUserObject) o;

            return id.equals(that.id);
        }

        public int hashCode() {
            return id.hashCode();
        }
    }

    private static class MyUserObjectResolver implements StatementUserObjectOption {
        private static List<StatementUserObjectContext> contexts = new ArrayList<>();

        public static List<StatementUserObjectContext> getContexts() {
            return contexts;
        }

        public Serializable getValue(StatementUserObjectContext env) {
            contexts.add(env);
            return null;
        }
    }
}
