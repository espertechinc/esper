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

import com.espertech.esper.core.context.mgr.ContextStatePathKey;
import com.espertech.esper.core.context.mgr.ContextStatePathValue;

import java.io.Serializable;
import java.util.TreeMap;

public class EPContextPartitionImportable implements Serializable {
    private static final long serialVersionUID = 4455652878395126963L;
    private final TreeMap<ContextStatePathKey, ContextStatePathValue> paths;

    public EPContextPartitionImportable(TreeMap<ContextStatePathKey, ContextStatePathValue> paths) {
        this.paths = paths;
    }

    public TreeMap<ContextStatePathKey, ContextStatePathValue> getPaths() {
        return paths;
    }
}
