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
package com.espertech.esper.common.internal.epl.lookupplansubord;

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;

import java.util.List;

public class SubordinateQueryIndexSuggest {
    private final SubordinateQueryIndexDescForge forge;
    private final List<StmtClassForgeableFactory> multiKeyForgeables;

    public SubordinateQueryIndexSuggest(SubordinateQueryIndexDescForge forge, List<StmtClassForgeableFactory> multiKeyForgeables) {
        this.forge = forge;
        this.multiKeyForgeables = multiKeyForgeables;
    }

    public SubordinateQueryIndexDescForge getForge() {
        return forge;
    }

    public List<StmtClassForgeableFactory> getMultiKeyForgeables() {
        return multiKeyForgeables;
    }
}
