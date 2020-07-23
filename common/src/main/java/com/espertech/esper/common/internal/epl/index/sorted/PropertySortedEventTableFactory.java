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
package com.espertech.esper.common.internal.epl.index.sorted;

import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganization;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganizationType;

/**
 * Index that organizes events by the event property values into a single TreeMap sortable non-nested index
 * with Object keys that store the property values.
 */
public class PropertySortedEventTableFactory implements EventTableFactory {
    protected final int streamNum;
    protected final String propertyName;
    protected final EventPropertyValueGetter propertyGetter;
    protected final EPTypeClass valueType;

    public PropertySortedEventTableFactory(int streamNum, String propertyName, EventPropertyValueGetter propertyGetter, EPTypeClass valueType) {
        this.streamNum = streamNum;
        this.propertyName = propertyName;
        this.propertyGetter = propertyGetter;
        this.valueType = valueType;
    }

    public EventTable[] makeEventTables(ExprEvaluatorContext exprEvaluatorContext, Integer subqueryNumber) {
        return new EventTable[]{new PropertySortedEventTableImpl(this)};
    }

    public Class getEventTableClass() {
        return PropertySortedEventTable.class;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() +
                " streamNum=" + streamNum +
                " propertyName=" + propertyName;
    }

    public int getStreamNum() {
        return streamNum;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public EPTypeClass getValueType() {
        return valueType;
    }

    protected EventTableOrganization getOrganization() {
        return new EventTableOrganization(null, false, false, streamNum, new String[]{propertyName}, EventTableOrganizationType.BTREE);
    }
}
