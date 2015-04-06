/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.pattern;

import com.espertech.esper.client.ConfigurationPlugInPatternObject;
import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.epl.spec.PatternGuardSpec;
import com.espertech.esper.epl.spec.PatternObserverSpec;
import com.espertech.esper.epl.spec.PluggableObjectCollection;
import com.espertech.esper.support.pattern.SupportObserverFactory;
import com.espertech.esper.support.pattern.SupportGuardFactory;
import com.espertech.esper.pattern.guard.TimerWithinGuardFactory;
import com.espertech.esper.pattern.observer.TimerIntervalObserverFactory;
import com.espertech.esper.view.TestViewSupport;

import java.util.List;
import java.util.ArrayList;

import junit.framework.TestCase;

public class TestPatternObjectResolutionServiceImpl extends TestCase
{
    private PatternObjectResolutionServiceImpl service;

    public void setUp()
    {
        List<ConfigurationPlugInPatternObject> init = new ArrayList<ConfigurationPlugInPatternObject>();
        init.add(makeGuardSpec("g", "h", SupportGuardFactory.class.getName()));
        init.add(makeObserverSpec("a", "b", SupportObserverFactory.class.getName()));
        PluggableObjectCollection desc = new PluggableObjectCollection();
        desc.addPatternObjects(init);
        desc.addObjects(PatternObjectHelper.getBuiltinPatternObjects());
        service = new PatternObjectResolutionServiceImpl(desc);
    }

    public void testMake() throws Exception
    {
        assertTrue(service.create(new PatternGuardSpec("g", "h", TestViewSupport.toExprListBean(new Object[] {100}))) instanceof SupportGuardFactory);
        assertTrue(service.create(new PatternObserverSpec("a", "b", TestViewSupport.toExprListBean(new Object[] {100}))) instanceof SupportObserverFactory);
        assertTrue(service.create(new PatternGuardSpec("timer", "within", TestViewSupport.toExprListBean(new Object[] {100}))) instanceof TimerWithinGuardFactory);
        assertTrue(service.create(new PatternObserverSpec("timer", "interval", TestViewSupport.toExprListBean(new Object[] {100}))) instanceof TimerIntervalObserverFactory);
    }

    public void testInvalidConfig()
    {
        List<ConfigurationPlugInPatternObject> init = new ArrayList<ConfigurationPlugInPatternObject>();
        init.add(makeGuardSpec("x", "y", "a"));
        tryInvalid(init);

        init.clear();
        init.add(makeGuardSpec("a", "b", null));
        tryInvalid(init);
    }

    private void tryInvalid(List<ConfigurationPlugInPatternObject> config)
    {
        try
        {
            PluggableObjectCollection desc = new PluggableObjectCollection();
            desc.addPatternObjects(config);
            service = new PatternObjectResolutionServiceImpl(desc);
            fail();
        }
        catch (ConfigurationException ex)
        {
            // expected
        }
    }


    private ConfigurationPlugInPatternObject makeGuardSpec(String namespace, String name, String factory)
    {
        ConfigurationPlugInPatternObject guardSpec = new ConfigurationPlugInPatternObject();
        guardSpec.setNamespace(namespace);
        guardSpec.setName(name);
        guardSpec.setPatternObjectType(ConfigurationPlugInPatternObject.PatternObjectType.GUARD);
        guardSpec.setFactoryClassName(factory);
        return guardSpec; 
    }

    private ConfigurationPlugInPatternObject makeObserverSpec(String namespace, String name, String factory)
    {
        ConfigurationPlugInPatternObject obsSpec = new ConfigurationPlugInPatternObject();
        obsSpec.setNamespace(namespace);
        obsSpec.setName(name);
        obsSpec.setPatternObjectType(ConfigurationPlugInPatternObject.PatternObjectType.OBSERVER);
        obsSpec.setFactoryClassName(factory);
        return obsSpec;
    }
}
