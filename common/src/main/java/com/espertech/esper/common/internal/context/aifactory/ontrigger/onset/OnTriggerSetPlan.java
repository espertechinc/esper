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
package com.espertech.esper.common.internal.context.aifactory.ontrigger.onset;

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableAIFactoryProviderBase;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectSubscriberDescriptor;

import java.util.List;

public class OnTriggerSetPlan {
    private final StmtClassForgeableAIFactoryProviderBase forgeable;
    private final List<StmtClassForgeable> forgeables;
    private final SelectSubscriberDescriptor selectSubscriberDescriptor;
    private final List<StmtClassForgeableFactory> additionalForgeables;

    public OnTriggerSetPlan(StmtClassForgeableAIFactoryProviderBase forgeable, List<StmtClassForgeable> forgeables, SelectSubscriberDescriptor selectSubscriberDescriptor, List<StmtClassForgeableFactory> additionalForgeables) {
        this.forgeable = forgeable;
        this.forgeables = forgeables;
        this.selectSubscriberDescriptor = selectSubscriberDescriptor;
        this.additionalForgeables = additionalForgeables;
    }

    public StmtClassForgeableAIFactoryProviderBase getForgeable() {
        return forgeable;
    }

    public List<StmtClassForgeable> getForgeables() {
        return forgeables;
    }

    public SelectSubscriberDescriptor getSelectSubscriberDescriptor() {
        return selectSubscriberDescriptor;
    }

    public List<StmtClassForgeableFactory> getAdditionalForgeables() {
        return additionalForgeables;
    }
}


