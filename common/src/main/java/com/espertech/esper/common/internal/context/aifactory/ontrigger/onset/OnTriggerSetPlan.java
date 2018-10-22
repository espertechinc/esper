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

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgableAIFactoryProviderBase;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectSubscriberDescriptor;

import java.util.List;

public class OnTriggerSetPlan {
    private final StmtClassForgableAIFactoryProviderBase forgable;
    private final List<StmtClassForgable> forgables;
    private final SelectSubscriberDescriptor selectSubscriberDescriptor;

    public OnTriggerSetPlan(StmtClassForgableAIFactoryProviderBase forgable, List<StmtClassForgable> forgables, SelectSubscriberDescriptor selectSubscriberDescriptor) {
        this.forgable = forgable;
        this.forgables = forgables;
        this.selectSubscriberDescriptor = selectSubscriberDescriptor;
    }

    public StmtClassForgableAIFactoryProviderBase getForgable() {
        return forgable;
    }

    public List<StmtClassForgable> getForgables() {
        return forgables;
    }

    public SelectSubscriberDescriptor getSelectSubscriberDescriptor() {
        return selectSubscriberDescriptor;
    }
}


