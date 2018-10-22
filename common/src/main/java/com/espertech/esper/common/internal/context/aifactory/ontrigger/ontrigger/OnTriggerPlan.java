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

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgableAIFactoryProviderBase;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectSubscriberDescriptor;

import java.util.List;

public class OnTriggerPlan {
    private final StmtClassForgableAIFactoryProviderBase factory;
    private final List<StmtClassForgable> forgables;
    private final SelectSubscriberDescriptor subscriberDescriptor;

    public OnTriggerPlan(StmtClassForgableAIFactoryProviderBase factory, List<StmtClassForgable> forgables, SelectSubscriberDescriptor subscriberDescriptor) {
        this.factory = factory;
        this.forgables = forgables;
        this.subscriberDescriptor = subscriberDescriptor;
    }

    public StmtClassForgableAIFactoryProviderBase getFactory() {
        return factory;
    }

    public List<StmtClassForgable> getForgables() {
        return forgables;
    }

    public SelectSubscriberDescriptor getSubscriberDescriptor() {
        return subscriberDescriptor;
    }
}
