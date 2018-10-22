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
package com.espertech.esper.common.internal.epl.index.compile;

import java.util.Map;

public class IndexCompileTimeRegistry {
    private final Map<IndexCompileTimeKey, IndexDetailForge> indexes;

    public IndexCompileTimeRegistry(Map<IndexCompileTimeKey, IndexDetailForge> indexes) {
        this.indexes = indexes;
    }

    public void newIndex(IndexCompileTimeKey key, IndexDetailForge detail) {
        IndexDetailForge existing = indexes.get(key);
        if (existing != null) {
            throw new IllegalStateException("A duplicate index has been encountered for key '" + key + "'");
        }
        indexes.put(key, detail);
    }

    public Map<IndexCompileTimeKey, IndexDetailForge> getIndexes() {
        return indexes;
    }
}
