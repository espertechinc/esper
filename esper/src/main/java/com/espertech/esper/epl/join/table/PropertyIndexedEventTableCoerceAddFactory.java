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
package com.espertech.esper.epl.join.table;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.SimpleNumberCoercer;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

/**
 * Index that organizes events by the event property values into hash buckets. Based on a HashMap
 * with {@link com.espertech.esper.collection.MultiKeyUntyped} keys that store the property values.
 * <p>
 * Performs coercion of the index keys before storing the keys.
 * <p>
 * Takes a list of property names as parameter. Doesn't care which event type the events have as long as the properties
 * exist. If the same event is added twice, the class throws an exception on add.
 */
public class PropertyIndexedEventTableCoerceAddFactory extends PropertyIndexedEventTableFactory {
    protected final SimpleNumberCoercer[] coercers;
    protected final Class[] coercionType;

    /**
     * Ctor.
     *
     * @param streamNum     is the stream number of the indexed stream
     * @param eventType     is the event type of the indexed stream
     * @param propertyNames are the property names to get property values
     * @param coercionType  are the classes to coerce indexed values to
     */
    public PropertyIndexedEventTableCoerceAddFactory(int streamNum, EventType eventType, String[] propertyNames, Class[] coercionType) {
        super(streamNum, eventType, propertyNames, false, null);
        this.coercionType = coercionType;
        coercers = new SimpleNumberCoercer[coercionType.length];
        for (int i = 0; i < coercionType.length; i++) {
            if (JavaClassHelper.isNumeric(coercionType[i])) {
                coercers[i] = SimpleNumberCoercerFactory.getCoercer(null, coercionType[i]);
            }
        }
    }

    public EventTable[] makeEventTables(EventTableFactoryTableIdent tableIdent, ExprEvaluatorContext exprEvaluatorContext) {
        EventTableOrganization organization = getOrganization();
        return new EventTable[]{new PropertyIndexedEventTableCoerceAdd(propertyGetters, organization, coercers, coercionType)};
    }

    protected EventTableOrganization getOrganization() {
        return new EventTableOrganization(optionalIndexName, unique, true, streamNum, propertyNames, EventTableOrganizationType.HASH);
    }
}
