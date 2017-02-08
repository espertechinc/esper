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
package com.espertech.esper.dataflow.core;

import com.espertech.esper.client.dataflow.EPDataFlowSavedConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DataFlowConfigurationStateServiceImpl implements DataFlowConfigurationStateService {
    private final Map<String, EPDataFlowSavedConfiguration> savedConfigs = new HashMap<String, EPDataFlowSavedConfiguration>();

    public boolean exists(String savedConfigName) {
        return savedConfigs.containsKey(savedConfigName);
    }

    public void add(EPDataFlowSavedConfiguration savedConfiguration) {
        savedConfigs.put(savedConfiguration.getSavedConfigurationName(), savedConfiguration);
    }

    public String[] getSavedConfigNames() {
        Set<String> names = savedConfigs.keySet();
        return names.toArray(new String[names.size()]);
    }

    public EPDataFlowSavedConfiguration getSavedConfig(String savedConfigName) {
        return savedConfigs.get(savedConfigName);
    }

    public EPDataFlowSavedConfiguration removePrototype(String savedConfigName) {
        return savedConfigs.remove(savedConfigName);
    }
}
