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
package com.espertech.esper.common.internal.epl.variable.core;

import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;

import java.util.HashMap;
import java.util.Map;

public class VariableRepositoryPreconfigured {
    private final Map<String, VariableMetaData> metadata = new HashMap<>();

    public VariableRepositoryPreconfigured() {
    }

    public void addVariable(String name, VariableMetaData meta) {
        metadata.put(name, meta);
    }

    public VariableMetaData getMetadata(String name) {
        return metadata.get(name);
    }

    public Map<String, VariableMetaData> getMetadata() {
        return metadata;
    }

    public void mergeFrom(VariableRepositoryPreconfigured other) {
        for (Map.Entry<String, VariableMetaData> entry : other.getMetadata().entrySet()) {
            if (metadata.containsKey(entry.getKey())) {
                continue;
            }
            metadata.put(entry.getKey(), entry.getValue());
        }
    }
}
