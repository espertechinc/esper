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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.client.annotation.AppliesTo;
import com.espertech.esper.common.client.util.StateMgmtSetting;
import com.espertech.esper.common.internal.fabric.FabricTypeCollector;

public interface AggregationServiceFactoryForge {
    void setStateMgmtSetting(StateMgmtSetting stateMgmtSetting);
    AppliesTo appliesTo();
    void appendRowFabricType(FabricTypeCollector fabricTypeCollector);
    <T> T accept(AggregationServiceFactoryForgeVisitor<T> visitor);
}
