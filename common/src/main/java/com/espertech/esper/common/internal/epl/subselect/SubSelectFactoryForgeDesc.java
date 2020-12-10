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
package com.espertech.esper.common.internal.epl.subselect;

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.fabric.FabricCharge;

import java.util.List;

public class SubSelectFactoryForgeDesc {
    private final SubSelectFactoryForge subSelectFactoryForge;
    private final List<StmtClassForgeableFactory> additionalForgeables;
    private final FabricCharge fabricCharge;

    public SubSelectFactoryForgeDesc(SubSelectFactoryForge subSelectFactoryForge, List<StmtClassForgeableFactory> additionalForgeables, FabricCharge fabricCharge) {
        this.subSelectFactoryForge = subSelectFactoryForge;
        this.additionalForgeables = additionalForgeables;
        this.fabricCharge = fabricCharge;
    }

    public SubSelectFactoryForge getSubSelectFactoryForge() {
        return subSelectFactoryForge;
    }

    public List<StmtClassForgeableFactory> getAdditionalForgeables() {
        return additionalForgeables;
    }

    public FabricCharge getFabricCharge() {
        return fabricCharge;
    }
}
