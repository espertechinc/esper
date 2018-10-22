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
package com.espertech.esper.common.internal.epl.index.base;

import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.context.module.ModuleIndexMeta;
import com.espertech.esper.common.internal.epl.index.compile.IndexCollector;
import com.espertech.esper.common.internal.epl.index.compile.IndexCompileTimeKey;
import com.espertech.esper.common.internal.epl.index.compile.IndexDetail;

import java.util.Set;

public class IndexCollectorRuntime implements IndexCollector {
    private final Set<ModuleIndexMeta> moduleIndexes;

    public IndexCollectorRuntime(Set<ModuleIndexMeta> moduleIndexes) {
        this.moduleIndexes = moduleIndexes;
    }

    public void registerIndex(IndexCompileTimeKey indexKey, IndexDetail indexDetail) {
        if (indexKey.getVisibility() == NameAccessModifier.PUBLIC) {
            moduleIndexes.add(new ModuleIndexMeta(indexKey.isNamedWindow(), indexKey.getInfraName(), indexKey.getInfraModuleName(), indexKey.getIndexName(), indexKey.getInfraModuleName()));
        }
    }
}
