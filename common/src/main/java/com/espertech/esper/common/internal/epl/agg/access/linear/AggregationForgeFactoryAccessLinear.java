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
package com.espertech.esper.common.internal.epl.agg.access.linear;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionStateKey;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregationAgentForge;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregationForgeFactoryAccessBase;
import com.espertech.esper.common.internal.epl.agg.core.AggregationAccessorForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.core.AggregationStateFactoryForge;
import com.espertech.esper.common.internal.epl.expression.agg.accessagg.ExprAggMultiFunctionLinearAccessNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.settings.ClasspathImportService;

public class AggregationForgeFactoryAccessLinear extends AggregationForgeFactoryAccessBase {

    private final ExprAggMultiFunctionLinearAccessNode parent;
    private final AggregationAccessorForge accessor;
    private final Class accessorResultType;
    private final AggregationMultiFunctionStateKey optionalStateKey;
    private final AggregationStateFactoryForge optionalStateFactory;
    private final AggregationAgentForge optionalAgent;
    private final EventType containedEventType;

    public AggregationForgeFactoryAccessLinear(ExprAggMultiFunctionLinearAccessNode parent, AggregationAccessorForge accessor, Class accessorResultType, AggregationMultiFunctionStateKey optionalStateKey, AggregationStateFactoryForge optionalStateFactory, AggregationAgentForge optionalAgent, EventType containedEventType) {
        this.parent = parent;
        this.accessor = accessor;
        this.accessorResultType = accessorResultType;
        this.optionalStateKey = optionalStateKey;
        this.optionalStateFactory = optionalStateFactory;
        this.optionalAgent = optionalAgent;
        this.containedEventType = containedEventType;
    }

    public Class getResultType() {
        return accessorResultType;
    }

    public AggregationMultiFunctionStateKey getAggregationStateKey(boolean isMatchRecognize) {
        return optionalStateKey;
    }

    public AggregationStateFactoryForge getAggregationStateFactory(boolean isMatchRecognize) {
        return optionalStateFactory;
    }

    public AggregationAccessorForge getAccessorForge() {
        return accessor;
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public AggregationPortableValidation getAggregationPortableValidation() {
        return new AggregationPortableValidationLinear(containedEventType);
    }

    public AggregationAgentForge getAggregationStateAgent(ClasspathImportService classpathImportService, String statementName) {
        return optionalAgent;
    }
}
