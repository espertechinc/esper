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

import com.espertech.esper.common.client.soda.StreamSelector;
import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerWindowDesc;
import com.espertech.esper.common.internal.compile.stage1.spec.StreamSpecCompiled;
import com.espertech.esper.common.internal.context.aifactory.ontrigger.core.OnTriggerActivatorDesc;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.subselect.SubSelectActivationPlan;

import java.util.Map;

public class OnTriggerWindowPlan {
    private final OnTriggerWindowDesc onTriggerDesc;
    private final String contextName;
    private final OnTriggerActivatorDesc activatorResult;
    private final StreamSelector optionalStreamSelector;
    private final Map<ExprSubselectNode, SubSelectActivationPlan> subselectActivation;
    private final StreamSpecCompiled streamSpec;

    public OnTriggerWindowPlan(OnTriggerWindowDesc onTriggerDesc, String contextName, OnTriggerActivatorDesc activatorResult, StreamSelector optionalStreamSelector, Map<ExprSubselectNode, SubSelectActivationPlan> subselectActivation, StreamSpecCompiled streamSpec) {
        this.onTriggerDesc = onTriggerDesc;
        this.contextName = contextName;
        this.activatorResult = activatorResult;
        this.optionalStreamSelector = optionalStreamSelector;
        this.subselectActivation = subselectActivation;
        this.streamSpec = streamSpec;
    }

    public OnTriggerWindowDesc getOnTriggerDesc() {
        return onTriggerDesc;
    }

    public String getContextName() {
        return contextName;
    }

    public OnTriggerActivatorDesc getActivatorResult() {
        return activatorResult;
    }

    public StreamSelector getOptionalStreamSelector() {
        return optionalStreamSelector;
    }

    public Map<ExprSubselectNode, SubSelectActivationPlan> getSubselectActivation() {
        return subselectActivation;
    }

    public StreamSpecCompiled getStreamSpec() {
        return streamSpec;
    }
}
