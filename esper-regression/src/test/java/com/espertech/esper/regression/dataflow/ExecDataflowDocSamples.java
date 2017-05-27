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
package com.espertech.esper.regression.dataflow;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.dataflow.EPDataFlowInstance;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.soda.EPStatementFormatter;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecDataflowDocSamples implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionDocSamples(epService);
        runAssertionSODA(epService);
    }

    private void runAssertionDocSamples(EPServiceProvider epService) throws Exception {
        String epl = "create dataflow HelloWorldDataFlow\n" +
                "BeaconSource -> helloworldStream { text: 'hello world', iterations : 1 }\n" +
                "LogSink(helloworldStream) {}";
        epService.getEPAdministrator().createEPL(epl);

        EPDataFlowInstance instance = epService.getEPRuntime().getDataFlowRuntime().instantiate("HelloWorldDataFlow");
        instance.run();

        tryEpl(epService, "create dataflow MyDataFlow\n" +
                "MyOperatorSimple {}");
        tryEpl(epService, "create dataflow MyDataFlow2\n" +
                "create schema MyEvent as (id string, price double),\n" +
                "MyOperator(myInStream) -> myOutStream<MyEvent> {\n" +
                "myParameter : 10\n" +
                "}");
        tryEpl(epService, "create dataflow MyDataFlow3\n" +
                "MyOperator(myInStream as mis) {}");
        tryEpl(epService, "create dataflow MyDataFlow4\n" +
                "MyOperator(streamOne as one, streamTwo as two) {}");
        tryEpl(epService, "create dataflow MyDataFlow5\n" +
                "MyOperator( (streamA, streamB) as streamsAB) {}");
        tryEpl(epService, "create dataflow MyDataFlow6\n" +
                "MyOperator(abc) -> my.out.stream {}");
        tryEpl(epService, "create dataflow MyDataFlow7\n" +
                "MyOperator -> my.out.one, my.out.two {}");
        tryEpl(epService, "create dataflow MyDataFlow8\n" +
                "create objectarray schema RFIDSchema (tagId string, locX double, locy double),\n" +
                "MyOperator -> rfid.stream<RFIDSchema> {}");
        tryEpl(epService, "create dataflow MyDataFlow9\n" +
                "create objectarray schema RFIDSchema (tagId string, locX double, locy double),\n" +
                "MyOperator -> rfid.stream<eventbean<RFIDSchema>> {}");
        tryEpl(epService, "create dataflow MyDataFlow10\n" +
                "MyOperator -> my.stream<eventbean<?>> {}");
        tryEpl(epService, "create dataflow MyDataFlow11\n" +
                "MyOperator {\n" +
                "stringParam : 'sample',\n" +
                "secondString : \"double-quotes are fine\",\n" +
                "intParam : 10\n" +
                "}");
        tryEpl(epService, "create dataflow MyDataFlow12\n" +
                "MyOperator {\n" +
                "intParam : 24*60^60,\n" +
                "threshold : var_threshold, // a variable defined in the engine\n" +
                "}");
        tryEpl(epService, "create dataflow MyDataFlow13\n" +
                "MyOperator {\n" +
                "someSystemProperty : systemProperties('mySystemProperty')\n" +
                "}");
        tryEpl(epService, "create dataflow MyDataFlow14\n" +
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

    private void runAssertionSODA(EPServiceProvider epService) {

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
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(soda);
        EPAssertionUtil.assertEqualsIgnoreNewline(soda, model.toEPL(new EPStatementFormatter(true)));
        epService.getEPAdministrator().create(model);
    }

    private void tryEpl(EPServiceProvider epService, String epl) {
        epService.getEPAdministrator().createEPL(epl);
    }

}
