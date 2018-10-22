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

import com.espertech.esper.common.client.module.Module;
import com.espertech.esper.common.client.module.ModuleItem;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.fail;

public class ClientCompileSyntaxValidate {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientCompileOptionsValidateOnly());
        execs.add(new ClientCompileSyntaxMgs());
        return execs;
    }

    private static class ClientCompileSyntaxMgs implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidCompile(env, "insert into 7event select * from SupportBeanReservedKeyword",
                "Incorrect syntax near '7' at line 1 column 12");

            tryInvalidCompile(env, "select foo, create from SupportBeanReservedKeyword",
                "Incorrect syntax near 'create' (a reserved keyword) at line 1 column 12, please check the select clause");

            tryInvalidCompile(env, "select * from pattern [",
                "Unexpected end-of-input at line 1 column 23, please check the pattern expression within the from clause");

            tryInvalidCompile(env, "select * from A, into",
                "Incorrect syntax near 'into' (a reserved keyword) at line 1 column 17, please check the from clause");

            tryInvalidCompile(env, "select * from pattern[A -> B - C]",
                "Incorrect syntax near '-' expecting a right angle bracket ']' but found a minus '-' at line 1 column 29, please check the from clause");

            tryInvalidCompile(env, "insert into A (a",
                "Unexpected end-of-input at line 1 column 16 [insert into A (a]");

            tryInvalidCompile(env, "select case when 1>2 from A",
                "Incorrect syntax near 'from' (a reserved keyword) expecting 'then' but found 'from' at line 1 column 21, please check the case expression within the select clause [select case when 1>2 from A]");

            tryInvalidCompile(env, "select * from A full outer join B on A.field < B.field",
                "Incorrect syntax near '<' expecting an equals '=' but found a lesser then '<' at line 1 column 45, please check the outer join within the from clause [select * from A full outer join B on A.field < B.field]");

            tryInvalidCompile(env, "select a.b('aa\") from A",
                "Failed to parse: Unexpected exception recognizing module text, recognition failed for LexerNoViableAltException(''')");

            tryInvalidCompile(env, "select * from A, sql:mydb [\"",
                "Failed to parse: Unexpected exception recognizing module text, recognition failed for LexerNoViableAltException('\"')");

            tryInvalidCompile(env, "select * google",
                "Incorrect syntax near 'google' at line 1 column 9 [");

            tryInvalidCompile(env, "insert into into",
                "Incorrect syntax near 'into' (a reserved keyword) at line 1 column 12 [insert into into]");

            tryInvalidCompile(env, "on SupportBean select 1",
                "Required insert-into clause is not provided, the clause is required for split-stream syntax");
        }
    }

    private static class ClientCompileOptionsValidateOnly implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            Module module = new Module();
            module.getItems().add(new ModuleItem("select * from NoSuchEvent"));
            try {
                EPCompilerProvider.getCompiler().syntaxValidate(module, null);
            } catch (EPCompileException e) {
                throw new RuntimeException(e);
            }

            module = new Module();
            module.getItems().add(new ModuleItem("xxx"));
            try {
                EPCompilerProvider.getCompiler().syntaxValidate(module, null);
                fail();
            } catch (EPCompileException ex) {
                SupportMessageAssertUtil.assertMessage(ex, "Incorrect syntax near 'xxx'");
            }

            module = new Module();
            EPStatementObjectModel model = new EPStatementObjectModel();
            module.getItems().add(new ModuleItem(model));
            try {
                EPCompilerProvider.getCompiler().syntaxValidate(module, null);
                fail();
            } catch (EPCompileException ex) {
                SupportMessageAssertUtil.assertMessage(ex, "Select-clause has not been defined");
            }
        }
    }
}
