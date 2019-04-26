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
package com.espertech.esper.common.internal.context.controller.initterm;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.context.controller.condition.ContextConditionDescriptor;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerDetail;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

public class ContextControllerDetailInitiatedTerminated implements ContextControllerDetail {

    private ContextConditionDescriptor startCondition;
    private ContextConditionDescriptor endCondition;
    private boolean overlapping;
    private ExprEvaluator distinctEval;
    private Class[] distinctTypes;
    private DataInputOutputSerde<Object> distinctSerde;

    public ContextConditionDescriptor getStartCondition() {
        return startCondition;
    }

    public void setStartCondition(ContextConditionDescriptor startCondition) {
        this.startCondition = startCondition;
    }

    public ContextConditionDescriptor getEndCondition() {
        return endCondition;
    }

    public void setEndCondition(ContextConditionDescriptor endCondition) {
        this.endCondition = endCondition;
    }

    public boolean isOverlapping() {
        return overlapping;
    }

    public void setOverlapping(boolean overlapping) {
        this.overlapping = overlapping;
    }

    public ExprEvaluator getDistinctEval() {
        return distinctEval;
    }

    public void setDistinctEval(ExprEvaluator distinctEval) {
        this.distinctEval = distinctEval;
    }

    public Class[] getDistinctTypes() {
        return distinctTypes;
    }

    public void setDistinctTypes(Class[] distinctTypes) {
        this.distinctTypes = distinctTypes;
    }

    public DataInputOutputSerde<Object> getDistinctSerde() {
        return distinctSerde;
    }

    public void setDistinctSerde(DataInputOutputSerde<Object> distinctSerde) {
        this.distinctSerde = distinctSerde;
    }
}
