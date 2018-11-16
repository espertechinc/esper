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
package com.espertech.esper.example.virtualdw;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.EPDeployment;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.EPStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleVirtualDataWindowMain {
    private static final Logger log = LoggerFactory.getLogger(SampleVirtualDataWindowMain.class);

    public static void main(String[] args) {
        SampleVirtualDataWindowMain sample = new SampleVirtualDataWindowMain();
        try {
            sample.run();
        } catch (RuntimeException ex) {
            log.error("Unexpected exception :" + ex.getMessage(), ex);
        }
    }

    public void run() {
        log.info("Setting up runtime instance.");

        Configuration config = new Configuration();
        config.getCompiler().addPlugInVirtualDataWindow("sample", "samplevdw", SampleVirtualDataWindowForge.class.getName());
        config.getCommon().addEventType(SampleTriggerEvent.class);
        config.getCommon().addEventType(SampleJoinEvent.class);
        config.getCommon().addEventType(SampleMergeEvent.class);

        EPRuntime runtime = EPRuntimeProvider.getRuntime("LargeExternalDataExample", config);

        // First: Create an event type for rows of the external data - here the example use a Map-based event and any of the other types (POJO, XML) can be used as well.
        // Populate event property names and types.
        // Note: the type must match the data returned by virtual data window indexes.
        compileDeploy("create schema SampleEvent as (key1 string, key2 string, value1 int, value2 double)", runtime);

        log.info("Creating named window with virtual.");

        // Create Named Window holding SampleEvent instances
        compileDeploy("create window MySampleWindow.sample:samplevdw() as SampleEvent", runtime);

        // Example subquery
        log.info("Running subquery example.");
        runSubquerySample(runtime);

        // Example joins
        log.info("Running join example.");
        runJoinSample(runtime);

        // Sample FAF
        log.info("Running fire-and-forget query example.");
        runSampleFireAndForgetQuery(runtime);

        // Sample On-Merge
        log.info("Running on-merge example.");
        runSampleOnMerge(runtime);

        // Cleanup
        log.info("Destroying runtime instance, sample completed successfully.");
        runtime.destroy();
    }

    private void runSubquerySample(EPRuntime runtime) {

        String epl = "select (select key2 from MySampleWindow where key1 = ste.triggerKey) as key2 from SampleTriggerEvent ste";
        EPStatement stmt = compileDeploy(epl, runtime);
        SampleUpdateListener sampleListener = new SampleUpdateListener();
        stmt.addListener(sampleListener);

        runtime.getEventService().sendEventBean(new SampleTriggerEvent("sample1"), "SampleTriggerEvent");
        log.info("Subquery returned: " + sampleListener.getLastEvent().get("key2"));
        // For assertions against expected results please see the regression test suite
    }

    private void runJoinSample(EPRuntime runtime) {
        String epl = "select sw.* " +
            "from SampleJoinEvent#lastevent sje, MySampleWindow sw " +
            "where sw.key1 = sje.propOne and sw.key2 = sje.propTwo";
        EPStatement stmt = compileDeploy(epl, runtime);
        SampleUpdateListener sampleListener = new SampleUpdateListener();
        stmt.addListener(sampleListener);

        runtime.getEventService().sendEventBean(new SampleJoinEvent("sample1", "sample2"), "SampleJoinEvent"); // see values in SampleVirtualDataWindowIndex
        log.info("Join query returned: " + sampleListener.getLastEvent().get("key1") + " and " + sampleListener.getLastEvent().get("key2"));

        // For assertions against expected results please see the regression test suite
    }

    private void runSampleFireAndForgetQuery(EPRuntime runtime) {
        String fireAndForget = "select * from MySampleWindow where key1 = 'sample1' and key2 = 'sample2'"; // see values in SampleVirtualDataWindowIndex

        EPCompiled compiled;
        try {
            CompilerArguments args = new CompilerArguments();
            args.getPath().add(runtime.getRuntimePath());
            compiled = EPCompilerProvider.getCompiler().compileQuery(fireAndForget, args);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        EPFireAndForgetQueryResult result = runtime.getFireAndForgetService().executeQuery(compiled);

        log.info("Fire-and-forget query returned: " + result.getArray()[0].get("key1") + " and " + result.getArray()[0].get("key2"));

        // For assertions against expected results please see the regression test suite
    }

    private void runSampleOnMerge(EPRuntime runtime) {

        String onDelete =
            "on SampleMergeEvent " +
                "merge MySampleWindow " +
                "where key1 = propOne " +
                "when not matched then insert select propOne as key1, propTwo as key2, 0 as value1, 0d as value2 " +
                "when matched then update set key2 = propTwo";
        EPStatement stmt = compileDeploy(onDelete, runtime);
        SampleUpdateListener sampleListener = new SampleUpdateListener();
        stmt.addListener(sampleListener);

        // not-matching case
        runtime.getEventService().sendEventBean(new SampleMergeEvent("mykey-sample", "hello"), "SampleMergeEvent");
        log.info("Received inserted key: " + sampleListener.getLastEvent().get("key1") + " and " + sampleListener.getLastEvent().get("key2"));

        // matching case
        runtime.getEventService().sendEventBean(new SampleMergeEvent("sample1", "newvalue"), "SampleMergeEvent");  // see values in SampleVirtualDataWindowIndex
        log.info("Received updated key: " + sampleListener.getLastEvent().get("key1") + " and " + sampleListener.getLastEvent().get("key2"));

        // For assertions against expected results please see the regression test suite
    }

    private EPStatement compileDeploy(String epl, EPRuntime runtime) {
        try {
            CompilerArguments args = new CompilerArguments();
            args.getPath().add(runtime.getRuntimePath());
            args.getOptions().setAccessModifierEventType(env -> NameAccessModifier.PUBLIC);
            args.getOptions().setAccessModifierNamedWindow(env -> NameAccessModifier.PUBLIC);
            args.getOptions().setBusModifierEventType(env -> EventTypeBusModifier.BUS);
            args.setConfiguration(runtime.getConfigurationDeepCopy());

            EPCompiled compiled = EPCompilerProvider.getCompiler().compile(epl, args);
            EPDeployment deployment = runtime.getDeploymentService().deploy(compiled);
            return deployment.getStatements()[0];
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}

