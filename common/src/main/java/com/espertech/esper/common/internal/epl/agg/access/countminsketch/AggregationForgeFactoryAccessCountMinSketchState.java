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
package com.espertech.esper.common.internal.epl.agg.access.countminsketch;

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionStateKey;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregationAgentForge;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregationForgeFactoryAccessBase;
import com.espertech.esper.common.internal.epl.agg.core.AggregationAccessorForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregationPortableValidation;
import com.espertech.esper.common.internal.epl.agg.core.AggregationStateFactoryForge;
import com.espertech.esper.common.internal.epl.expression.agg.accessagg.ExprAggMultiFunctionCountMinSketchNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.settings.ClasspathImportService;

public class AggregationForgeFactoryAccessCountMinSketchState extends AggregationForgeFactoryAccessBase {
    private final ExprAggMultiFunctionCountMinSketchNode parent;
    private final AggregationStateCountMinSketchForge stateFactory;

    public AggregationForgeFactoryAccessCountMinSketchState(ExprAggMultiFunctionCountMinSketchNode parent, AggregationStateCountMinSketchForge stateFactory) {
        this.parent = parent;
        this.stateFactory = stateFactory;
    }

    public Class getResultType() {
        return null;
    }

    public AggregationMultiFunctionStateKey getAggregationStateKey(boolean isMatchRecognize) {
        throw new UnsupportedOperationException("State key not available as always used with tables");
    }

    public AggregationStateFactoryForge getAggregationStateFactory(boolean isMatchRecognize) {
        // For match-recognize we don't allow
        if (isMatchRecognize) {
            throw new IllegalStateException("Count-min-sketch is not supported for match-recognize");
        }
        return stateFactory;
    }

    public AggregationAccessorForge getAccessorForge() {
        return new AggregationAccessorForgeCountMinSketch();
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public AggregationAgentForge getAggregationStateAgent(ClasspathImportService classpathImportService, String statementName) {
        throw new UnsupportedOperationException("Agent not available for state-function");
    }

    public AggregationPortableValidation getAggregationPortableValidation() {
        return new AggregationPortableValidationCountMinSketch(stateFactory.specification.getAgent().getAcceptableValueTypes());
    }
}
