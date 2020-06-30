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
package com.espertech.esper.common.internal.serde.compiletime.resolve;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.event.core.BaseNestableEventType;

public interface SerdeCompileTimeResolver {
    DataInputOutputSerdeForge serdeForFilter(EPType evaluationType, StatementRawInfo raw);
    DataInputOutputSerdeForge[] serdeForDataWindowSortCriteria(EPType[] types, StatementRawInfo raw);
    DataInputOutputSerdeForge[] serdeForMultiKey(EPType[] types, StatementRawInfo raw);
    DataInputOutputSerdeForge serdeForKeyNonArray(EPType paramType, StatementRawInfo raw);
    DataInputOutputSerdeForge serdeForDerivedViewAddProp(EPType evalType, StatementRawInfo raw);
    DataInputOutputSerdeForge serdeForBeanEventType(StatementRawInfo raw, EPTypeClass underlyingType, String eventTypeName, EventType[] eventTypeSupertypes);
    DataInputOutputSerdeForge serdeForEventProperty(EPTypeClass typedProperty, String eventTypeName, String propertyName, StatementRawInfo raw);
    DataInputOutputSerdeForge serdeForAggregation(EPType type, StatementRawInfo raw);
    DataInputOutputSerdeForge serdeForAggregationDistinct(EPType type, StatementRawInfo raw);
    DataInputOutputSerdeForge serdeForIndexBtree(EPTypeClass rangeType, StatementRawInfo raw);
    DataInputOutputSerdeForge serdeForIndexHashNonArray(EPTypeClass propType, StatementRawInfo raw);
    DataInputOutputSerdeForge serdeForVariable(EPTypeClass type, String variableName, StatementRawInfo raw);
    DataInputOutputSerdeForge serdeForEventTypeExternalProvider(BaseNestableEventType eventType, StatementRawInfo raw);
    boolean isTargetHA();
}
