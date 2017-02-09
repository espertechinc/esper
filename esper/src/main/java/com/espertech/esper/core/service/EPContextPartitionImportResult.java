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
package com.espertech.esper.core.service;

import java.io.Serializable;
import java.util.Map;

public class EPContextPartitionImportResult implements Serializable {
    private static final long serialVersionUID = -1344280301808943635L;
    private final Map<Integer, Integer> existingToImported;
    private final Map<Integer, Integer> allocatedToImported;

    public EPContextPartitionImportResult(Map<Integer, Integer> existingToImported, Map<Integer, Integer> allocatedToImported) {
        this.existingToImported = existingToImported;
        this.allocatedToImported = allocatedToImported;
    }

    public Map<Integer, Integer> getAllocatedToImported() {
        return allocatedToImported;
    }

    public Map<Integer, Integer> getExistingToImported() {
        return existingToImported;
    }
}
