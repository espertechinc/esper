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
package com.espertech.esper.example.namedwinquery;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQuery;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.EPDeployment;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.EPStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class NamedWindowQueryMain {
    private static final Logger log = LoggerFactory.getLogger(NamedWindowQueryMain.class);

    public static void main(String[] args) {
        NamedWindowQueryMain main = new NamedWindowQueryMain();

        try {
            main.runExample(false, "NamedWindowQuery");
        } catch (Exception ex) {
            log.error("Unexpected error occured running example:" + ex.getMessage(), ex);
        }
    }

    public void runExample(boolean isRunFromUnitTest, String runtimeURI) {
        int numEventsToLoad = 100000;
        int numFireAndForgetExecutions = 100;
        int numOnEventQueryExecutions = 100000;
        if (isRunFromUnitTest) {
            numEventsToLoad = 1000;
            numFireAndForgetExecutions = 5;
            numOnEventQueryExecutions = 5;
        }

        // define event types - this example uses Map event representation
        //
        Configuration configuration = new Configuration();

        Map<String, Object> definition = new LinkedHashMap<String, Object>();
        definition.put("sensor", String.class);
        definition.put("temperature", double.class);
        configuration.getCommon().addEventType("SensorEvent", definition);

        Map<String, Object> definitionQuery = new LinkedHashMap<String, Object>();
        definitionQuery.put("querytemp", double.class);
        configuration.getCommon().addEventType("SensorQueryEvent", definitionQuery);

        EPRuntime runtime = EPRuntimeProvider.getRuntime(runtimeURI, configuration);

        // This example initializes the runtime instance as it is running within an overall test suite.
        // This step would not be required unless re-using the same runtime instance with different configurations.
        runtime.initialize();

        // define a named window to hold the last 1000000 (1M) events
        //
        String epl = "create window SensorWindow#keepall as select * from SensorEvent";
        log.info("Creating named window : " + epl);
        compileDeploy(epl, runtime);

        epl = "insert into SensorWindow select * from SensorEvent";
        log.info("Creating insert statement for named window : " + epl);
        compileDeploy(epl, runtime);

        // load 1M events
        //
        Random random = new Random();
        String[] sensors = "s1,s2,s3,s4,s5,s6".split(",");

        log.info("Generating " + numEventsToLoad + " sensor events for the named window");
        List<Map<String, Object>> events = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < numEventsToLoad; i++) {
            double temperature = random.nextDouble() * 10 + 80;
            String sensor = sensors[random.nextInt(sensors.length)];

            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("temperature", temperature);
            data.put("sensor", sensor);

            events.add(data);
        }
        log.info("Completed generating sensor events");

        log.info("Sending " + events.size() + " sensor events into runtime");
        for (Map<String, Object> theEvent : events) {
            runtime.getEventService().sendEventMap(theEvent, "SensorEvent");
        }
        log.info("Completed sending sensor events");

        // prepare fire-and-forget query
        //
        double sampleTemperature = (Double) events.get(0).get("temperature");
        epl = "select * from SensorWindow where temperature = " + sampleTemperature;

        log.info("Compiling fire-and-forget query : " + epl);
        CompilerArguments args = new CompilerArguments();
        args.getPath().add(runtime.getRuntimePath());
        EPCompiled onDemandQueryCompiled;
        try {
            onDemandQueryCompiled = EPCompilerProvider.getCompiler().compileQuery(epl, args);
        } catch (EPCompileException e) {
            throw new RuntimeException(e);
        }

        log.info("Preparing fire-and-forget query : " + epl);
        EPFireAndForgetPreparedQuery onDemandQuery = runtime.getFireAndForgetService().prepareQuery(onDemandQueryCompiled);

        log.info("Executing fire-and-forget query " + numFireAndForgetExecutions + " times");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numFireAndForgetExecutions; i++) {
            EPFireAndForgetQueryResult result = onDemandQuery.execute();
            if (result.getArray().length != 1) {
                throw new RuntimeException("Failed assertion of result, expected a single row returned from query");
            }
        }
        long endTime = System.currentTimeMillis();
        double deltaSec = (endTime - startTime) / 1000.0;
        log.info("Executing fire-and-forget query " + numFireAndForgetExecutions + " times took " + deltaSec + " seconds");

        // prepare on-select
        //

        epl = "on SensorQueryEvent select sensor from SensorWindow where temperature = querytemp";
        log.info("Creating on-select statement for named window : " + epl);
        EPStatement onSelectStmt = compileDeploy(epl, runtime);
        onSelectStmt.setSubscriber(new Object() {
            /**
             * This is a dummy subscriber update method, for the purpose of example
             * @param result is the output of an EPL statement
             */
            public void update(String result) {
                // No action taken here
            }
        });

        log.info("Executing on-select query " + numOnEventQueryExecutions + " times");
        startTime = System.currentTimeMillis();
        for (int i = 0; i < numOnEventQueryExecutions; i++) {
            Map<String, Object> queryParams = new HashMap<String, Object>();
            queryParams.put("querytemp", sampleTemperature);

            runtime.getEventService().sendEventMap(queryParams, "SensorQueryEvent");
        }
        endTime = System.currentTimeMillis();
        deltaSec = (endTime - startTime) / 1000.0;
        log.info("Executing on-select query " + numOnEventQueryExecutions + " times took " + deltaSec + " seconds");

        runtime.destroy();
    }

    private EPStatement compileDeploy(String epl, EPRuntime runtime) {
        try {
            CompilerArguments args = new CompilerArguments();
            args.getPath().add(runtime.getRuntimePath());
            args.getOptions().setAccessModifierNamedWindow(env -> NameAccessModifier.PUBLIC); // All named windows are visibile
            args.getConfiguration().getCompiler().getByteCode().setAllowSubscriber(true); // allow subscribers

            EPCompiled compiled = EPCompilerProvider.getCompiler().compile(epl, args);
            EPDeployment deployment = runtime.getDeploymentService().deploy(compiled);
            return deployment.getStatements()[0];
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}

