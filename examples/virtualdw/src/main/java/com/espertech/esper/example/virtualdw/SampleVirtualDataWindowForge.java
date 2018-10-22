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

import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategy;
import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategyClassNewInstance;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowFactoryMode;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowFactoryModeManaged;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowForge;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowForgeContext;

import java.util.Set;

public class SampleVirtualDataWindowForge implements VirtualDataWindowForge {

    public void initialize(VirtualDataWindowForgeContext initializeContext) {
    }

    public VirtualDataWindowFactoryMode getFactoryMode() {
        // The injection strategy defines how to obtain and configure the factory-factory.
        InjectionStrategy injectionStrategy = new InjectionStrategyClassNewInstance(SampleVirtualDataWindowFactoryFactory.class);

        // The managed-mode is the default. It uses the provided injection strategy.
        VirtualDataWindowFactoryModeManaged managed = new VirtualDataWindowFactoryModeManaged();
        managed.setInjectionStrategyFactoryFactory(injectionStrategy);

        return managed;
    }

    public Set<String> getUniqueKeyPropertyNames() {
        // lets assume there is no unique key property names
        return null;
    }
}
