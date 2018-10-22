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
package com.espertech.esperio.kafka;

import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static com.espertech.esperio.kafka.EsperIOKafkaConfig.OUTPUT_FLOWCONTROLLER_CONFIG;
import static com.espertech.esperio.kafka.EsperIOKafkaInputAdapter.getRequiredProperty;

public class EsperIOKafkaOutputAdapter {
    private static final Logger log = LoggerFactory.getLogger(EsperIOKafkaOutputAdapter.class);

    private final Properties properties;
    private final String runtimeURI;
    private EsperIOKafkaOutputFlowController controller;

    public EsperIOKafkaOutputAdapter(Properties properties, String runtimeURI) {
        this.properties = properties;
        this.runtimeURI = runtimeURI;
    }

    public void start() {

        if (log.isInfoEnabled()) {
            log.info("Starting EsperIO Kafka Output Adapter for runtime URI '{}'", runtimeURI);
        }

        // Obtain runtime
        EPRuntimeSPI runtime = (EPRuntimeSPI) EPRuntimeProvider.getRuntime(runtimeURI);

        // Obtain and invoke flow controlle
        String className = getRequiredProperty(properties, OUTPUT_FLOWCONTROLLER_CONFIG);
        EsperIOKafkaOutputFlowController controller;
        try {
            controller = (EsperIOKafkaOutputFlowController) JavaClassHelper.instantiate(EsperIOKafkaOutputFlowController.class, className, runtime.getServicesContext().getClasspathImportServiceRuntime().getClassForNameProvider());
            EsperIOKafkaOutputFlowControllerContext context = new EsperIOKafkaOutputFlowControllerContext(runtime, properties);
            controller.initialize(context);
        } catch (Throwable t) {
            throw new ConfigurationException("Unexpected exception invoking flow-controller initialize method on class " + className + " for runtime URI '" + runtimeURI + "': " + t.getMessage(), t);
        }

        if (log.isInfoEnabled()) {
            log.info("Completed starting EsperIO Kafka Output Adapter for runtime URI '{}'", runtimeURI);
        }
    }

    public void destroy() {
        if (log.isDebugEnabled()) {
            log.debug("Destroying Esper Kafka Output Adapter for runtime URI '{}'", runtimeURI);
        }

        if (controller != null) {
            try {
                controller.close();
            } catch (Throwable t) {
                log.warn("Unexpected exception invoking flow-controller close method: " + t.getMessage(), t);
            }
            controller = null;
        }
    }
}
