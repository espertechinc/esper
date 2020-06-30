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

public class SerdeCompileTimeResolverNonHA implements SerdeCompileTimeResolver {
    public final static SerdeCompileTimeResolverNonHA INSTANCE = new SerdeCompileTimeResolverNonHA();

    private SerdeCompileTimeResolverNonHA() {
    }

    public DataInputOutputSerdeForge serdeForFilter(EPType evaluationType, StatementRawInfo raw) {
        return noop();
    }

    public DataInputOutputSerdeForge serdeForKeyNonArray(EPType paramType, StatementRawInfo raw) {
        return noop();
    }

    public DataInputOutputSerdeForge[] serdeForMultiKey(EPType[] types, StatementRawInfo raw) {
        return noop(types);
    }

    public DataInputOutputSerdeForge[] serdeForDataWindowSortCriteria(EPType[] types, StatementRawInfo raw) {
        return noop(types);
    }

    public DataInputOutputSerdeForge serdeForDerivedViewAddProp(EPType evalType, StatementRawInfo raw) {
        return noop();
    }

    public DataInputOutputSerdeForge serdeForBeanEventType(StatementRawInfo raw, EPTypeClass underlyingType, String eventTypeName, EventType[] eventTypeSupertypes) {
        return noop();
    }

    public DataInputOutputSerdeForge serdeForEventProperty(EPTypeClass typedProperty, String eventTypeName, String propertyName, StatementRawInfo raw) {
        return noop();
    }

    public DataInputOutputSerdeForge serdeForIndexBtree(EPTypeClass rangeType, StatementRawInfo raw) {
        return noop();
    }

    public DataInputOutputSerdeForge serdeForAggregation(EPType type, StatementRawInfo raw) {
        return noop();
    }

    public DataInputOutputSerdeForge serdeForAggregationDistinct(EPType type, StatementRawInfo raw) {
        return noop();
    }

    public DataInputOutputSerdeForge serdeForIndexHashNonArray(EPTypeClass propType, StatementRawInfo raw) {
        return noop();
    }

    public DataInputOutputSerdeForge serdeForVariable(EPTypeClass type, String variableName, StatementRawInfo raw) {
        return noop();
    }

    public DataInputOutputSerdeForge serdeForEventTypeExternalProvider(BaseNestableEventType eventType, StatementRawInfo raw) {
        return noop();
    }

    public boolean isTargetHA() {
        return false;
    }

    private DataInputOutputSerdeForge noop() {
        return DataInputOutputSerdeForgeNotApplicable.INSTANCE;
    }

    private DataInputOutputSerdeForge[] noop(EPType[] types) {
        DataInputOutputSerdeForge[] forges = new DataInputOutputSerdeForge[types.length];
        for (int i = 0; i < forges.length; i++) {
            forges[i] = noop();
        }
        return forges;
    }
}
