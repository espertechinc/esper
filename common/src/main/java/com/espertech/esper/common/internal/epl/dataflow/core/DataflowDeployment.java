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
package com.espertech.esper.common.internal.epl.dataflow.core;

import com.espertech.esper.common.internal.context.aifactory.createdataflow.DataflowDesc;

import java.util.HashMap;
import java.util.Map;

public class DataflowDeployment {
    private final Map<String, DataflowDesc> dataflows = new HashMap<>(4);

    public void add(String dataflowName, DataflowDesc metadata) {
        DataflowDesc existing = dataflows.get(dataflowName);
        if (existing != null) {
            throw new IllegalStateException("Dataflow already found for name '" + dataflowName + "'");
        }
        dataflows.put(dataflowName, metadata);
    }

    public DataflowDesc getDataflow(String dataflowName) {
        return dataflows.get(dataflowName);
    }

    public void remove(String dataflowName) {
        dataflows.remove(dataflowName);
    }

    public boolean isEmpty() {
        return dataflows.isEmpty();
    }

    public Map<String, DataflowDesc> getDataflows() {
        return dataflows;
    }
}
