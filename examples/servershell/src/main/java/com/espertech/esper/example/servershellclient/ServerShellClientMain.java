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
package com.espertech.esper.example.servershellclient;

import com.espertech.esper.example.servershell.ServerShellConstants;
import com.espertech.esper.example.servershell.jms.JMSContext;
import com.espertech.esper.example.servershell.jms.JMSContextFactory;
import com.espertech.esper.example.servershell.jmx.EPServiceProviderJMXMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.BytesMessage;
import javax.jms.DeliveryMode;
import javax.jms.MessageProducer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.Properties;
import java.util.Random;

public class ServerShellClientMain {
    private final static Logger log = LoggerFactory.getLogger(ServerShellClientMain.class);

    public static void main(String[] args) throws Exception {
        try {
            new ServerShellClientMain();
        } catch (Throwable t) {
            log.error("Error starting server shell client : " + t.getMessage(), t);
            System.exit(-1);
        }
    }

    public ServerShellClientMain() throws Exception {
        log.info("Loading properties");
        Properties properties = new Properties();
        InputStream propertiesIS = ServerShellClientMain.class.getClassLoader().getResourceAsStream(ServerShellConstants.CONFIG_FILENAME);
        if (propertiesIS == null) {
            throw new RuntimeException("Properties file '" + ServerShellConstants.CONFIG_FILENAME + "' not found in classpath");
        }
        properties.load(propertiesIS);

        // Attached via JMX to running server
        log.info("Attach to server via JMX");
        JMXServiceURL url = new JMXServiceURL(properties.getProperty(ServerShellConstants.MGMT_SERVICE_URL));
        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
        ObjectName mBeanName = new ObjectName(ServerShellConstants.MGMT_MBEAN_NAME);
        EPServiceProviderJMXMBean proxy = (EPServiceProviderJMXMBean) MBeanServerInvocationHandler.newProxyInstance(
                mbsc, mBeanName, EPServiceProviderJMXMBean.class, true);

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

        // Create statement via JMX
        log.info("Creating a statement via Java Management Extensions (JMX) MBean Proxy");
        proxy.createEPL("select * from SampleEvent where duration > 9.9", "filterStatement", new ClientSideUpdateListener());

        // Get producer
        jmsCtx.getConnection().start();
        MessageProducer producer = jmsCtx.getSession().createProducer(jmsCtx.getDestination());

        Random random = new Random();
        String[] ipAddresses = {"127.0.1.0", "127.0.2.0", "127.0.3.0", "127.0.4.0"};
        NumberFormat format = NumberFormat.getInstance();

        // Send messages
        for (int i = 0; i < 1000; i++) {
            String ipAddress = ipAddresses[random.nextInt(ipAddresses.length)];
            double duration = 10 * random.nextDouble();
            String durationStr = format.format(duration);
            String payload = ipAddress + "," + durationStr;

            BytesMessage bytesMessage = jmsCtx.getSession().createBytesMessage();
            bytesMessage.writeBytes(payload.getBytes());
            bytesMessage.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
            producer.send(bytesMessage);

            if (i % 100 == 0) {
                log.info("Sent " + i + " messages");
            }
        }

        // Create statement via JMX
        log.info("Destroing statement via Java Management Extensions (JMX) MBean Proxy");
        proxy.destroy("filterStatement");

        log.info("Shutting down JMS client connection");
        jmsCtx.destroy();

        log.info("Exiting");
        System.exit(-1);
    }
}
