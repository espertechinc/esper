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


public class ServerShellConstants {
    public static final String CONFIG_FILENAME = "servershell_config.properties";

    public static final String JMS_CONTEXT_FACTORY = "jms-context-factory";
    public static final String JMS_PROVIDER_URL = "jms-provider-url";
    public static final String JMS_CONNECTION_FACTORY_NAME = "jms-connection-factory-name";
    public static final String JMS_USERNAME = "jms-user";
    public static final String JMS_PASSWORD = "jms-password";
    public static final String JMS_INCOMING_DESTINATION = "jms-incoming-destination";
    public static final String JMS_IS_TOPIC = "jms-is-topic";
    public static final String JMS_NUM_LISTENERS = "jms-num-listeners";

    public static final String MGMT_RMI_PORT = "rmi-port";
    public static final String MGMT_SERVICE_URL = "jmx-service-url";
    public static final String MGMT_MBEAN_NAME = "com.espertech.esper.mbean:type=EPServiceProviderJMXMBean";
}
