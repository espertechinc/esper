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
package com.espertech.esper.supportregression.epl;

import com.espertech.esper.client.hook.*;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.AggregationValidationContext;

public class SupportPluginAggregationMethodOneFactory implements AggregationFunctionFactory {
    public void setFunctionName(String functionName) {
    }

    public void validate(AggregationValidationContext validationContext) {
    }

    public AggregationMethod newAggregator() {
        return new SupportPluginAggregationMethodOne();
    }

    public Class getValueType() {
        return int.class;
    }

    public AggregationFunctionFactoryCodegenType getCodegenType() {
        return AggregationFunctionFactoryCodegenType.CODEGEN_MANAGED;
    }

    public void rowMemberCodegen(AggregationFunctionFactoryCodegenRowMemberContext context) {
        SupportPluginAggregationMethodOne.rowMemberCodegen(context);
    }

    public void applyEnterCodegenManaged(AggregationFunctionFactoryCodegenRowApplyContextManaged context) {
        SupportPluginAggregationMethodOne.applyEnterCodegenManaged(context);
    }

    public void applyLeaveCodegenManaged(AggregationFunctionFactoryCodegenRowApplyContextManaged context) {
        SupportPluginAggregationMethodOne.applyLeaveCodegenManaged(context);
    }

    public void applyEnterCodegenUnmanaged(AggregationFunctionFactoryCodegenRowApplyContextUnmanaged context) {
    }

    public void applyLeaveCodegenUnmanaged(AggregationFunctionFactoryCodegenRowApplyContextUnmanaged context) {
    }

    public void clearCodegen(AggregationFunctionFactoryCodegenRowClearContext context) {
        SupportPluginAggregationMethodOne.clearCodegen(context);
    }

    public void getValueCodegen(AggregationFunctionFactoryCodegenRowGetValueContext context) {
        SupportPluginAggregationMethodOne.getValueCodegen(context);
    }
}
