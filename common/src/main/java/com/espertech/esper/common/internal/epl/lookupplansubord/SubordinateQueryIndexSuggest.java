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
import com.espertech.esper.common.internal.fabric.FabricCharge;

import java.util.List;

public class SubordinateQueryIndexSuggest {
    private final SubordinateQueryIndexDescForge forge;
    private final List<StmtClassForgeableFactory> multiKeyForgeables;
    private final FabricCharge fabricCharge;

    public SubordinateQueryIndexSuggest(SubordinateQueryIndexDescForge forge, List<StmtClassForgeableFactory> multiKeyForgeables, FabricCharge fabricCharge) {
        this.forge = forge;
        this.multiKeyForgeables = multiKeyForgeables;
        this.fabricCharge = fabricCharge;
    }

    public SubordinateQueryIndexDescForge getForge() {
        return forge;
    }

    public List<StmtClassForgeableFactory> getMultiKeyForgeables() {
        return multiKeyForgeables;
    }

    public FabricCharge getFabricCharge() {
        return fabricCharge;
    }
}
