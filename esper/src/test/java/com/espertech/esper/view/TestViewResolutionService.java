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
package com.espertech.esper.view;

import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.client.ConfigurationPlugInView;
import com.espertech.esper.client.ConfigurationPlugInVirtualDataWindow;
import com.espertech.esper.core.support.SupportEngineImportServiceFactory;
import com.espertech.esper.epl.spec.PluggableObjectCollection;
import com.espertech.esper.epl.spec.PluggableObjectRegistryImpl;
import com.espertech.esper.supportunit.view.SupportViewFactoryOne;
import com.espertech.esper.supportunit.view.SupportViewFactoryTwo;
import com.espertech.esper.view.stat.UnivariateStatisticsViewFactory;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class TestViewResolutionService extends TestCase {
    private ViewResolutionService service;

    public void setUp() {
        PluggableObjectRegistryImpl registry = new PluggableObjectRegistryImpl(new PluggableObjectCollection[]{ViewEnumHelper.getBuiltinViews()});
        service = new ViewResolutionServiceImpl(registry, null, null);
    }

    public void testInitializeFromConfig() throws Exception {
        service = createService(new String[]{"a", "b"}, new String[]{"v1", "v2"},
                new String[]{SupportViewFactoryOne.class.getName(), SupportViewFactoryTwo.class.getName()});

        ViewFactory factory = service.create("a", "v1");
        assertTrue(factory instanceof SupportViewFactoryOne);

        factory = service.create("b", "v2");
        assertTrue(factory instanceof SupportViewFactoryTwo);

        tryInvalid("a", "v3");
        tryInvalid("c", "v1");

        try {
            service = createService(new String[]{"a"}, new String[]{"v1"}, new String[]{"abc"});
            fail();
        } catch (ConfigurationException ex) {
            // expected
        }
    }

    private void tryInvalid(String namespace, String name) {
        try {
            service.create(namespace, name);
            fail();
        } catch (ViewProcessingException ex) {
            // expected
        }
    }

    public void testCreate() throws Exception {
        ViewFactory viewFactory = service.create(ViewEnum.UNIVARIATE_STATISTICS.getNamespace(), ViewEnum.UNIVARIATE_STATISTICS.getName());
        assertTrue(viewFactory instanceof UnivariateStatisticsViewFactory);
    }

    public void testInvalidViewName() {
        try {
            service.create("dummy", "bumblebee");
            assertFalse(true);
        } catch (ViewProcessingException ex) {
            log.debug(".testInvalidViewName Expected exception caught, msg=" + ex.getMessage());
        }
    }

    private ViewResolutionService createService(String[] namespaces, String[] names, String[] classNames) {
        List<ConfigurationPlugInView> configs = new LinkedList<ConfigurationPlugInView>();
        for (int i = 0; i < namespaces.length; i++) {
            ConfigurationPlugInView config = new ConfigurationPlugInView();
            config.setNamespace(namespaces[i]);
            config.setName(names[i]);
            config.setFactoryClassName(classNames[i]);
            configs.add(config);
        }

        PluggableObjectCollection desc = new PluggableObjectCollection();
        desc.addViews(configs, Collections.<ConfigurationPlugInVirtualDataWindow>emptyList(), SupportEngineImportServiceFactory.make());
        PluggableObjectRegistryImpl registry = new PluggableObjectRegistryImpl(new PluggableObjectCollection[]{desc});
        return new ViewResolutionServiceImpl(registry, null, null);
    }

    private static final Logger log = LoggerFactory.getLogger(TestViewResolutionService.class);
}
