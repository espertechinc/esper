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
package com.espertech.esper.example.benchmark.server;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.example.benchmark.MarketData;
import com.espertech.esper.runtime.client.*;

/**
 * A factory and interface to wrap ESP/CEP runtime dependency in a single space
 *
 * @author Alexandre Vasseur http://avasseur.blogspot.com
 */
public class CEPProvider {

    public static interface ICEPProvider {

        public void init(int sleepListenerMillis);

        public void registerStatement(String epl, Object userObject);

        public void sendEventBean(Object theEvent, String eventTypeName);
    }

    public static ICEPProvider getCEPProvider() {
        String className = System.getProperty("esper.benchmark.provider", EsperCEPProvider.class.getName());
        try {
            Class klass = Class.forName(className);
            return (ICEPProvider) klass.newInstance();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    public static class EsperCEPProvider implements ICEPProvider {

        private EPRuntime runtime;

        private EPEventService epRuntime;

        // only one of those 2 will be attached to statement depending on the -mode selected
        private UpdateListener updateListener;
        private MySubscriber subscriber;

        private static int sleepListenerMillis;

        public EsperCEPProvider() {
        }

        public void init(final int sleepListenerMillisConfig) {
            sleepListenerMillis = sleepListenerMillisConfig;
            Configuration configuration;

            // EsperHA enablement - if available
            try {
                Class configurationHAClass = Class.forName("com.espertech.esperha.client.ConfigurationHA");
                configuration = (Configuration) configurationHAClass.newInstance();
                System.out.println("=== EsperHA is available, using ConfigurationHA ===");
            } catch (ClassNotFoundException e) {
                configuration = new Configuration();
            } catch (Throwable t) {
                System.err.println("Could not properly determine if EsperHA is available, default to Esper");
                t.printStackTrace();
                configuration = new Configuration();
            }
            configuration.getCommon().addEventType("Market", MarketData.class);

            // EsperJMX enablement - if available
            try {
                Class.forName("com.espertech.esper.jmx.client.EsperJMXPlugin");
                configuration.getRuntime().addPluginLoader(
                    "EsperJMX",
                    "com.espertech.esper.jmx.client.EsperJMXPlugin",
                    null); // will use platform mbean - should enable platform mbean connector in startup command line
                System.out.println("=== EsperJMX is available, using platform mbean ===");
            } catch (ClassNotFoundException e) {
            }

            runtime = EPRuntimeProvider.getRuntime("benchmark", configuration);
            updateListener = new MyUpdateListener();
            subscriber = new MySubscriber();
        }

        public void registerStatement(String epl, Object userObject) {
            // Compile statement. We simply use the provided configuration.
            CompilerArguments args = new CompilerArguments(runtime.getConfigurationDeepCopy());
            args.getConfiguration().getCompiler().getByteCode().setAllowSubscriber(true); // using subcribers

            EPCompiled compiled;
            try {
                compiled = EPCompilerProvider.getCompiler().compile(epl, args);
            } catch (EPCompileException e) {
                throw new RuntimeException("Failed to compile: " + e.getMessage(), e);
            }

            EPStatement stmt;
            try {
                stmt = runtime.getDeploymentService().deploy(compiled, new DeploymentOptions().setStatementUserObjectRuntime(ctx -> userObject)).getStatements()[0];
            } catch (EPDeployException e) {
                throw new RuntimeException("Failed to deploy: " + e.getMessage(), e);
            }

            if (System.getProperty("esper.benchmark.ul") != null) {
                stmt.addListener(updateListener);
            } else {
                stmt.setSubscriber(subscriber);
            }
        }

        public void sendEventBean(Object theEvent, String eventTypeName) {
            runtime.getEventService().sendEventBean(theEvent, eventTypeName);
        }
    }

    public static class MyUpdateListener implements UpdateListener {
        public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
            if (newEvents != null) {
                if (EsperCEPProvider.sleepListenerMillis > 0) {
                    try {
                        Thread.sleep(EsperCEPProvider.sleepListenerMillis);
                    } catch (InterruptedException ie) {
                    }
                }
            }
        }
    }

    public static class MySubscriber {
        public void update(String ticker) {
            if (EsperCEPProvider.sleepListenerMillis > 0) {
                try {
                    Thread.sleep(EsperCEPProvider.sleepListenerMillis);
                } catch (InterruptedException ie) {
                }
            }
        }

        public void update(MarketData marketData) {
            if (EsperCEPProvider.sleepListenerMillis > 0) {
                try {
                    Thread.sleep(EsperCEPProvider.sleepListenerMillis);
                } catch (InterruptedException ie) {
                }
            }
        }

        public void update(String ticker, double avg, long count, double sum) {
            if (EsperCEPProvider.sleepListenerMillis > 0) {
                try {
                    Thread.sleep(EsperCEPProvider.sleepListenerMillis);
                } catch (InterruptedException ie) {
                }
            }
        }
    }

}
