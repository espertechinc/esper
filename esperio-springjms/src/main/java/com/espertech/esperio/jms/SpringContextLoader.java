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
package com.espertech.esperio.jms;

import com.espertech.esper.adapter.Adapter;
import com.espertech.esper.adapter.AdapterSPI;
import com.espertech.esper.adapter.AdapterState;
import com.espertech.esper.client.EPException;
import com.espertech.esper.plugin.PluginLoader;
import com.espertech.esper.plugin.PluginLoaderInitContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Loader for Spring-configured input and output adapters.
 */
public class SpringContextLoader implements PluginLoader {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private AbstractXmlApplicationContext adapterSpringContext;
    private Map<String, Adapter> adapterMap = new HashMap<String, Adapter>();

    /**
     * Default Ctor needed for reflection instantiation.
     */
    public SpringContextLoader() {
    }

    public void destroy() {
        for (Adapter adapter : adapterMap.values()) {
            adapterSpringContext.destroy();
            if (adapter.getState() == AdapterState.STARTED) {
                adapter.stop();
            }
            if ((adapter.getState() == AdapterState.OPENED) || (adapter.getState() == AdapterState.PAUSED)) {
                adapter.destroy();
            }
        }
    }

    public void init(PluginLoaderInitContext context) {
        boolean fromClassPath = true;
        String resource = context.getProperties().getProperty(SpringContext.CLASSPATH_CONTEXT);
        if (resource == null) {
            fromClassPath = false;
            resource = context.getProperties().getProperty(SpringContext.FILE_APP_CONTEXT);
        }
        if (resource == null) {
            throw new IllegalArgumentException("Required property not found: " + SpringContext.CLASSPATH_CONTEXT + " or " + SpringContext.FILE_APP_CONTEXT);
        }

        // Load adapters
        log.debug(".Configuring from resource: " + resource);
        adapterSpringContext = createSpringApplicationContext(resource, fromClassPath);
        String[] beanNames = adapterSpringContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object o = adapterSpringContext.getBean(beanName);
            if (o instanceof Adapter) {
                adapterMap.put(beanName, (Adapter) o);
            }
        }

        // Initialize adapters
        Collection<Adapter> adapters = adapterMap.values();
        for (Adapter adapter : adapters) {
            if (adapter instanceof AdapterSPI) {
                AdapterSPI spi = (AdapterSPI) adapter;
                spi.setEPServiceProvider(context.getEpServiceProvider());
            }
            adapter.start();
        }
    }

    public void postInitialize() {
        // no action required
    }

    private AbstractXmlApplicationContext createSpringApplicationContext(String configuration, boolean fromClassPath) {
        if (fromClassPath) {
            log.debug("classpath configuration");
            return new ClassPathXmlApplicationContext(configuration);
        }
        if (new File(configuration).exists()) {
            log.debug("File configuration");
            return new FileSystemXmlApplicationContext(configuration);
        } else {
            throw new EPException("Spring configuration file not found.");
        }
    }
}
