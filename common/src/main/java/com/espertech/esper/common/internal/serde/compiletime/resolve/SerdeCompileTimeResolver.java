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
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.event.core.BaseNestableEventType;

public interface SerdeCompileTimeResolver {
    DataInputOutputSerdeForge serdeForFilter(Class evaluationType, StatementRawInfo raw);
    DataInputOutputSerdeForge[] serdeForDataWindowSortCriteria(Class[] types, StatementRawInfo raw);
    DataInputOutputSerdeForge[] serdeForMultiKey(Class[] types, StatementRawInfo raw);
    DataInputOutputSerdeForge serdeForKeyNonArray(Class paramType, StatementRawInfo raw);
    DataInputOutputSerdeForge serdeForDerivedViewAddProp(Class evalType, StatementRawInfo raw);
    DataInputOutputSerdeForge serdeForBeanEventType(StatementRawInfo raw, Class underlyingType, String eventTypeName, EventType[] eventTypeSupertypes);
    DataInputOutputSerdeForge serdeForEventProperty(Class typedProperty, String eventTypeName, String propertyName, StatementRawInfo raw);
    DataInputOutputSerdeForge serdeForAggregation(Class type, StatementRawInfo raw);
    DataInputOutputSerdeForge serdeForAggregationDistinct(Class type, StatementRawInfo raw);
    DataInputOutputSerdeForge serdeForIndexBtree(Class rangeType, StatementRawInfo raw);
    DataInputOutputSerdeForge serdeForIndexHashNonArray(Class propType, StatementRawInfo raw);
    DataInputOutputSerdeForge serdeForVariable(Class type, String variableName, StatementRawInfo raw);
    DataInputOutputSerdeForge serdeForEventTypeExternalProvider(BaseNestableEventType eventType, StatementRawInfo raw);
    boolean isTargetHA();
}
