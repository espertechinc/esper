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
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.settings.ClasspathImportService;

public class AggregationForgeFactoryAccessCountMinSketchAdd extends AggregationForgeFactoryAccessBase {
    private final ExprAggMultiFunctionCountMinSketchNode parent;
    private final ExprForge addOrFrequencyEvaluator;
    private final Class addOrFrequencyEvaluatorReturnType;

    public AggregationForgeFactoryAccessCountMinSketchAdd(ExprAggMultiFunctionCountMinSketchNode parent, ExprForge addOrFrequencyEvaluator, Class addOrFrequencyEvaluatorReturnType) {
        this.parent = parent;
        this.addOrFrequencyEvaluator = addOrFrequencyEvaluator;
        this.addOrFrequencyEvaluatorReturnType = addOrFrequencyEvaluatorReturnType;
    }

    public Class getResultType() {
        return null;
    }

    public AggregationMultiFunctionStateKey getAggregationStateKey(boolean isMatchRecognize) {
        throw new UnsupportedOperationException("State key not available as always used with tables");
    }

    public AggregationStateFactoryForge getAggregationStateFactory(boolean isMatchRecognize) {
        throw new UnsupportedOperationException("State factory not available for 'add' operation");
    }

    public AggregationAccessorForge getAccessorForge() {
        return new AggregationAccessorForgeCountMinSketch();
    }

    public ExprAggregateNodeBase getAggregationExpression() {
        return parent;
    }

    public AggregationAgentForge getAggregationStateAgent(ClasspathImportService classpathImportService, String statementName) {
        return new AggregationAgentCountMinSketchForge(addOrFrequencyEvaluator, parent.getOptionalFilter() == null ? null : parent.getOptionalFilter().getForge());
    }

    public AggregationPortableValidation getAggregationPortableValidation() {
        return new AggregationPortableValidationCountMinSketch();
    }

    public ExprAggMultiFunctionCountMinSketchNode getParent() {
        return parent;
    }

    public ExprForge getAddOrFrequencyEvaluator() {
        return addOrFrequencyEvaluator;
    }

    public Class getAddOrFrequencyEvaluatorReturnType() {
        return addOrFrequencyEvaluatorReturnType;
    }
}
