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

import com.espertech.esper.plugin.PluginLoader;
import com.espertech.esper.plugin.PluginLoaderInitContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EsperIOKafkaInputAdapterPlugin implements PluginLoader {
    private final static Logger log = LoggerFactory.getLogger(EsperIOKafkaInputAdapterPlugin.class);

    private EsperIOKafkaInputAdapter kafkaInputAdapter;
    private PluginLoaderInitContext context;

    public void init(PluginLoaderInitContext context) {
        this.context = context;
    }

    public void postInitialize() {
        log.info("Starting Kafka Input Adapter");
        kafkaInputAdapter = new EsperIOKafkaInputAdapter(context.getProperties(), context.getEpServiceProvider().getURI());
        kafkaInputAdapter.start();
    }

    public void destroy() {
        if (kafkaInputAdapter != null) {
            kafkaInputAdapter.destroy();
        }
        kafkaInputAdapter = null;
    }
}
