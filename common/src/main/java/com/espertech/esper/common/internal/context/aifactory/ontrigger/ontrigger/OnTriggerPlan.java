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
package com.espertech.esper.common.internal.context.aifactory.ontrigger.ontrigger;

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableAIFactoryProviderBase;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectSubscriberDescriptor;
import com.espertech.esper.common.internal.fabric.FabricCharge;

import java.util.List;

public class OnTriggerPlan {
    private final StmtClassForgeableAIFactoryProviderBase factory;
    private final List<StmtClassForgeable> forgeables;
    private final SelectSubscriberDescriptor subscriberDescriptor;
    private final List<StmtClassForgeableFactory> additionalForgeables;
    private final FabricCharge fabricCharge;

    public OnTriggerPlan(StmtClassForgeableAIFactoryProviderBase factory, List<StmtClassForgeable> forgeables, SelectSubscriberDescriptor subscriberDescriptor, List<StmtClassForgeableFactory> additionalForgeables, FabricCharge fabricCharge) {
        this.factory = factory;
        this.forgeables = forgeables;
        this.subscriberDescriptor = subscriberDescriptor;
        this.additionalForgeables = additionalForgeables;
        this.fabricCharge = fabricCharge;
    }

    public StmtClassForgeableAIFactoryProviderBase getFactory() {
        return factory;
    }

    public List<StmtClassForgeable> getForgeables() {
        return forgeables;
    }

    public SelectSubscriberDescriptor getSubscriberDescriptor() {
        return subscriberDescriptor;
    }

    public List<StmtClassForgeableFactory> getAdditionalForgeables() {
        return additionalForgeables;
    }

    public FabricCharge getFabricCharge() {
        return fabricCharge;
    }
}
