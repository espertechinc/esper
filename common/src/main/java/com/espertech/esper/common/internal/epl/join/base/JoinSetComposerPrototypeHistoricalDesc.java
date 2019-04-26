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
package com.espertech.esper.common.internal.epl.join.base;

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.historical.indexingstrategy.PollResultIndexingStrategyForge;
import com.espertech.esper.common.internal.epl.historical.lookupstrategy.HistoricalIndexLookupStrategyForge;

import java.util.List;

public class JoinSetComposerPrototypeHistoricalDesc {
    private final HistoricalIndexLookupStrategyForge lookupForge;
    private final PollResultIndexingStrategyForge indexingForge;
    private final List<StmtClassForgeableFactory> additionalForgeables;

    public JoinSetComposerPrototypeHistoricalDesc(HistoricalIndexLookupStrategyForge lookupForge, PollResultIndexingStrategyForge indexingForge, List<StmtClassForgeableFactory> additionalForgeables) {
        this.lookupForge = lookupForge;
        this.indexingForge = indexingForge;
        this.additionalForgeables = additionalForgeables;
    }

    public HistoricalIndexLookupStrategyForge getLookupForge() {
        return lookupForge;
    }

    public PollResultIndexingStrategyForge getIndexingForge() {
        return indexingForge;
    }

    public List<StmtClassForgeableFactory> getAdditionalForgeables() {
        return additionalForgeables;
    }
}
