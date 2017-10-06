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
package com.espertech.esper.epl.agg.access;

import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionCodegenType;

public interface AggregationAccessorForge {
    AggregationAccessor getAccessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName);

    default PlugInAggregationMultiFunctionCodegenType getPluginCodegenType() {
        return PlugInAggregationMultiFunctionCodegenType.CODEGEN_NONE;
    }

    default void getValueCodegen(AggregationAccessorForgeGetCodegenContext context) {}
    default void getEnumerableEventsCodegen(AggregationAccessorForgeGetCodegenContext context) {}
    default void getEnumerableEventCodegen(AggregationAccessorForgeGetCodegenContext context) {}
    default void getEnumerableScalarCodegen(AggregationAccessorForgeGetCodegenContext context) {}
}
