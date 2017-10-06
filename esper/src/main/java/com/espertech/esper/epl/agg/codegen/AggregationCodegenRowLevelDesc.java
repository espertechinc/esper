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
package com.espertech.esper.epl.agg.codegen;

import com.espertech.esper.epl.agg.service.common.AggregationRowStateForgeDesc;

public class AggregationCodegenRowLevelDesc {
    public final static AggregationCodegenRowLevelDesc EMPTY = new AggregationCodegenRowLevelDesc(null, null);
    private final AggregationCodegenRowDetailDesc optionalTopRow;
    private final AggregationCodegenRowDetailDesc[] optionalAdditionalRows;

    public AggregationCodegenRowLevelDesc(AggregationCodegenRowDetailDesc optionalTopRow, AggregationCodegenRowDetailDesc[] optionalAdditionalRows) {
        this.optionalTopRow = optionalTopRow;
        this.optionalAdditionalRows = optionalAdditionalRows;
    }

    public AggregationCodegenRowDetailDesc getOptionalTopRow() {
        return optionalTopRow;
    }

    public AggregationCodegenRowDetailDesc[] getOptionalAdditionalRows() {
        return optionalAdditionalRows;
    }

    public static AggregationCodegenRowLevelDesc fromTopOnly(AggregationRowStateForgeDesc rowStateDesc) {
        AggregationCodegenRowDetailStateDesc state = new AggregationCodegenRowDetailStateDesc(rowStateDesc.getMethodForges(), rowStateDesc.getMethodFactories(), rowStateDesc.getAccessFactoriesForges());
        AggregationCodegenRowDetailDesc top = new AggregationCodegenRowDetailDesc(state, rowStateDesc.getAccessAccessorsForges());
        return new AggregationCodegenRowLevelDesc(top, null);
    }
}
