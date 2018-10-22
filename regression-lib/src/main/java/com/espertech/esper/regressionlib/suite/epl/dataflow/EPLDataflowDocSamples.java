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
package com.espertech.esper.regressionlib.suite.epl.dataflow;

import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstance;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.EPStatementFormatter;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.Collection;

import static junit.framework.TestCase.fail;

public class EPLDataflowDocSamples {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDataflowDocSamplesRun());
        execs.add(new EPLDataflowSODA());
        return execs;
    }

    private static class EPLDataflowDocSamplesRun implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('flow') create dataflow HelloWorldDataFlow\n" +
                "BeaconSource -> helloworldStream { text: 'hello world', iterations : 1 }\n" +
                "LogSink(helloworldStream) {}";
            env.compileDeploy(epl);

            EPDataFlowInstance instance = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "HelloWorldDataFlow");
            instance.run();

            tryEpl(env, "create dataflow MyDataFlow\n" +
                "MyOperator {}");
            tryEpl(env, "create dataflow MyDataFlow2\n" +
                "create schema MyEvent as (id string, price double),\n" +
                "MyOperator -> myOutStream<MyEvent> {\n" +
                "myParameter : 10\n" +
                "}");
            tryEpl(env, "create dataflow MyDataFlow3\n" +
                "MyOperator(myInStream as mis) {}");
            tryEpl(env, "create dataflow MyDataFlow4\n" +
                "MyOperator(streamOne as one, streamTwo as two) {}");
            tryEpl(env, "create dataflow MyDataFlow5\n" +
                "MyOperator( (streamA, streamB) as streamsAB) {}");
            tryEpl(env, "create dataflow MyDataFlow6\n" +
                "MyOperator(abc) -> my.out.stream {}");
            tryEpl(env, "create dataflow MyDataFlow7\n" +
                "MyOperator -> my.out.one, my.out.two {}");
            tryEpl(env, "create dataflow MyDataFlow8\n" +
                "create objectarray schema RFIDSchema (tagId string, locX double, locy double),\n" +
                "MyOperator -> rfid.stream<RFIDSchema> {}");
            tryEpl(env, "create dataflow MyDataFlow9\n" +
                "create objectarray schema RFIDSchema (tagId string, locX double, locy double),\n" +
                "MyOperator -> rfid.stream<eventbean<RFIDSchema>> {}");
            tryEpl(env, "create dataflow MyDataFlow10\n" +
                "MyOperator -> my.stream<eventbean<?>> {}");
            tryEpl(env, "create dataflow MyDataFlow11\n" +
                "MyOperator {\n" +
                "stringParam : 'sample',\n" +
                "secondString : \"double-quotes are fine\",\n" +
                "intParam : 10\n" +
                "}");
            tryEpl(env, "create dataflow MyDataFlow12\n" +
                "MyOperator {\n" +
                "intParam : 24*60^60,\n" +
                "threshold : var_threshold, // a variable defined in the runtime\n" +
                "}");
            tryEpl(env, "create dataflow MyDataFlow13\n" +
                "MyOperator {\n" +
                "someSystemProperty : systemProperties('mySystemProperty')\n" +
                "}");
            tryEpl(env, "create dataflow MyDataFlow14\n" +
                "MyOperator {\n" +
                "  myStringArray: ['a', \"b\",],\n" +
                "  myMapOrObject: {\n" +
                "    a : 10,\n" +
                "    b : 'xyz',\n" +
                "  },\n" +
                "  myInstance: {\n" +
                "    class: 'com.myorg.myapp.MyImplementation',\n" +
                "    myValue : 'sample'\n" +
                "  }\n" +
                "}");
        }
    }

    private static class EPLDataflowSODA implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String soda = "@Name('create dataflow full')\n" +
                "create dataflow DFFull\n" +
                "create map schema ABC1 as (col1 int, col2 int),\n" +
                "create map schema ABC2 as (col1 int, col2 int),\n" +
                "MyOperatorOne(instream.one) -> outstream.one {}\n" +
                "MyOperatorTwo(instream.two as IN1, input.three as IN2) -> outstream.one<Test>, outstream.two<EventBean<TestTwo>> {}\n" +
                "MyOperatorThree((instream.two, input.three) as IN1) {}\n" +
                "MyOperatorFour -> teststream {}\n" +
                "MyOperatorFive {\n" +
                "const_str: \"abc\",\n" +
                "somevalue: def*2,\n" +
                "select: (select * from ABC where 1=2),\n" +
                "jsonarr: [\"a\",\"b\"],\n" +
                "jsonobj: {a: \"a\",b: \"b\"}\n" +
                "}\n";
            EPStatementObjectModel model = env.eplToModel(soda);
            EPAssertionUtil.assertEqualsIgnoreNewline(soda, model.toEPL(new EPStatementFormatter(true)));
        }
    }

    private static void tryEpl(RegressionEnvironment env, String epl) {
        try {
            EPCompilerProvider.getCompiler().parseModule(epl);
        } catch (Throwable t) {
            fail(t.getMessage());
        }
    }
}
