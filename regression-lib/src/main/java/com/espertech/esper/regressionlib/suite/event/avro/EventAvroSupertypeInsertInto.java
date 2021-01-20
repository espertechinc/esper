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
package com.espertech.esper.regressionlib.suite.event.avro;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

public class EventAvroSupertypeInsertInto implements RegressionExecution {
    private final static String[] FIELDS = new String[] {"symbol"};

    public void run(RegressionEnvironment env) {
        String epl = "@name('input') @public @buseventtype create avro schema Input(symbol string, price double);\n" +
            "\n" +
            "@public @buseventtype create avro schema SuperType(symbol string);\n" +
            "@public @buseventtype create avro schema B() inherits SuperType;\n" +
            "@public @buseventtype create avro schema A() inherits SuperType;\n" +
            "\n" +
            "insert into B select symbol from Input(symbol = 'B');\n" +
            "insert into A select symbol from Input(symbol = 'A');\n" +
            "\n" +
            "@Name('ss') select * from SuperType;\n" +
            "@Name('sa') select * from A;\n" +
            "@Name('sb') select * from B;\n";
        env.compileDeploy(epl).addListener("ss").addListener("sa").addListener("sb");

        sendEvent(env, "B");
        assertReceived(env, "ss", "B");
        assertReceived(env, "sb", "B");
        env.assertListenerNotInvoked("sa");

        sendEvent(env, "A");
        assertReceived(env, "ss", "A");
        assertReceived(env, "sa", "A");
        env.assertListenerNotInvoked("sb");

        env.undeployAll();
    }

    private void sendEvent(RegressionEnvironment env, String symbol) {
        Schema schema = env.runtimeAvroSchemaByDeployment("input", "Input");
        GenericData.Record rec = new GenericData.Record(schema);
        rec.put("symbol", symbol);
        rec.put("price", 1d);
        env.sendEventAvro(rec, "Input");
    }

    private void assertReceived(RegressionEnvironment env, String statementName, String symbol) {
        env.assertPropsNew(statementName, FIELDS, new Object[] {symbol});
    }
}
