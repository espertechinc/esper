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
package com.espertech.esper.regressionlib.support.extend.vdw;

import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategyClassNewInstance;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowFactoryMode;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowFactoryModeManaged;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowForge;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowForgeContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SupportVirtualDWForge implements VirtualDataWindowForge {
    private static List<VirtualDataWindowForgeContext> initializations = new ArrayList<>();
    private static Set<String> uniqueKeys;

    public static void setUniqueKeys(Set<String> uniqueKeys) {
        SupportVirtualDWForge.uniqueKeys = uniqueKeys;
    }

    public static List<VirtualDataWindowForgeContext> getInitializations() {
        return initializations;
    }

    public void initialize(VirtualDataWindowForgeContext initializeContext) {
        initializations.add(initializeContext);
    }

    public VirtualDataWindowFactoryMode getFactoryMode() {
        return new VirtualDataWindowFactoryModeManaged().setInjectionStrategyFactoryFactory(new InjectionStrategyClassNewInstance(SupportVirtualDWFactoryFactory.class));
    }

    public Set<String> getUniqueKeyPropertyNames() {
        return uniqueKeys;
    }
}
