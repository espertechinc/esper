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
package com.espertech.esper.common.internal.epl.join.queryplanbuild;

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.join.queryplanouter.LookupInstructionPlanForge;

import java.util.List;

public class LookupInstructionPlanDesc {
    private final List<LookupInstructionPlanForge> forges;
    private final List<StmtClassForgeableFactory> additionalForgeables;

    public LookupInstructionPlanDesc(List<LookupInstructionPlanForge> forges, List<StmtClassForgeableFactory> additionalForgeables) {
        this.forges = forges;
        this.additionalForgeables = additionalForgeables;
    }

    public List<LookupInstructionPlanForge> getForges() {
        return forges;
    }

    public List<StmtClassForgeableFactory> getAdditionalForgeables() {
        return additionalForgeables;
    }
}
