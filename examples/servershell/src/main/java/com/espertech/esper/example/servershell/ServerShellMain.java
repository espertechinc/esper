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
package com.espertech.esper.example.servershell;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.example.servershell.jms.JMSContext;
import com.espertech.esper.example.servershell.jms.JMSContextFactory;
import com.espertech.esper.example.servershell.jmx.EPServiceProviderJMX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.MessageConsumer;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.io.InputStream;
import java.rmi.registry.LocateRegistry;
import java.util.Properties;

public class ServerShellMain {
    private final static Logger log = LoggerFactory.getLogger(ServerShellMain.class);

    private boolean isShutdown;

    public static void main(String[] args) throws Exception {
        try {
            new ServerShellMain();
        } catch (Throwable t) {
            log.error("Error starting server shell : " + t.getMessage(), t);
            System.exit(-1);
        }
    }

    public ServerShellMain() throws Exception {
        log.info("Loading properties");
        Properties properties = new Properties();
        InputStream propertiesIS = ServerShellMain.class.getClassLoader().getResourceAsStream(ServerShellConstants.CONFIG_FILENAME);
        if (propertiesIS == null) {
            throw new RuntimeException("Properties file '" + ServerShellConstants.CONFIG_FILENAME + "' not found in classpath");
        }
        properties.load(propertiesIS);

        // Start RMI registry
        log.info("Starting RMI registry");
        int port = Integer.parseInt(properties.getProperty(ServerShellConstants.MGMT_RMI_PORT));
        LocateRegistry.createRegistry(port);

        // Obtain MBean servera
        log.info("Obtaining JMX server and connector");
        MBeanServer mbs = MBeanServerFactory.createMBeanServer();
        String jmxServiceURL = properties.getProperty(ServerShellConstants.MGMT_SERVICE_URL);
        JMXServiceURL jmxURL = new JMXServiceURL(jmxServiceURL);
        JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(jmxURL, null, mbs);
        cs.start();

        // Initialize engine
        log.info("Getting Esper engine instance");
        Configuration configuration = new Configuration();
        configuration.addEventType("SampleEvent", SampleEvent.class.getName());
        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider(configuration);

        // Initialize engine
        log.info("Creating sample statement");
        SampleStatement.createStatement(engine.getEPAdministrator());

        // Register MBean
        log.info("Registering MBean");
        ObjectName name = new ObjectName(ServerShellConstants.MGMT_MBEAN_NAME);
        EPServiceProviderJMX mbean = new EPServiceProviderJMX(engine);
        mbs.registerMBean(mbean, name);

        // Connect to JMS
        log.info("Connecting to JMS server");
        String factory = properties.getProperty(ServerShellConstants.JMS_CONTEXT_FACTORY);
        String jmsurl = properties.getProperty(ServerShellConstants.JMS_PROVIDER_URL);
        String connFactoryName = properties.getProperty(ServerShellConstants.JMS_CONNECTION_FACTORY_NAME);
        String user = properties.getProperty(ServerShellConstants.JMS_USERNAME);
        String password = properties.getProperty(ServerShellConstants.JMS_PASSWORD);
        String destination = properties.getProperty(ServerShellConstants.JMS_INCOMING_DESTINATION);
        boolean isTopic = Boolean.parseBoolean(properties.getProperty(ServerShellConstants.JMS_IS_TOPIC));
        JMSContext jmsCtx = JMSContextFactory.createContext(factory, jmsurl, connFactoryName, user, password, destination, isTopic);

        int numListeners = Integer.parseInt(properties.getProperty(ServerShellConstants.JMS_NUM_LISTENERS));
        log.info("Creating " + numListeners + " listeners to destination '" + destination + "'");

        SampleJMSMessageListener[] listeners = new SampleJMSMessageListener[numListeners];
        for (int i = 0; i < numListeners; i++) {
            listeners[i] = new SampleJMSMessageListener(engine.getEPRuntime());
            MessageConsumer consumer = jmsCtx.getSession().createConsumer(jmsCtx.getDestination());
            consumer.setMessageListener(listeners[i]);
        }

        // Start processing
        log.info("Starting JMS connection");
        jmsCtx.getConnection().start();

        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                isShutdown = true;
            }
        });

        // Report statistics
        long startTime = System.currentTimeMillis();
        long currTime;
        double deltaSeconds;
        int lastTotalEvents = 0;
        AccumulatingStat avgLast5 = new AccumulatingStat(5);
        AccumulatingStat avgLast10 = new AccumulatingStat(10);
        AccumulatingStat avgLast20 = new AccumulatingStat(20);
        do {
            // sleep
            Thread.sleep(1000);
            currTime = System.currentTimeMillis();
            deltaSeconds = (currTime - startTime) / 1000.0;

            // compute stats
            int totalEvents = 0;
            for (int i = 0; i < listeners.length; i++) {
                totalEvents += listeners[i].getCount();
            }

            double totalLastBatch = totalEvents - lastTotalEvents;

            avgLast5.add(totalLastBatch);
            avgLast10.add(totalLastBatch);
            avgLast20.add(totalLastBatch);

            log.info("total=" + totalEvents +
                    " last=" + totalLastBatch +
                    " last5Avg=" + avgLast5.getAvg() +
                    " last10Avg=" + avgLast10.getAvg() +
                    " last20Avg=" + avgLast20.getAvg() +
                    " time=" + deltaSeconds
            );
            lastTotalEvents = totalEvents;
        }
        while (!isShutdown);

        log.info("Shutting down server");
        jmsCtx.destroy();

        log.info("Exiting");
        System.exit(-1);
    }
}
