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
package com.espertech.esper.common.internal.epl.lookupplan;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryHashKeyedForge;

/**
 * Holds property information for joined properties in a lookup.
 */
public class SubordPropHashKeyForge {
    private final QueryGraphValueEntryHashKeyedForge hashKey;
    private final Integer optionalKeyStreamNum;
    private final EPTypeClass coercionType;

    public SubordPropHashKeyForge(QueryGraphValueEntryHashKeyedForge hashKey, Integer optionalKeyStreamNum, EPTypeClass coercionType) {
        this.hashKey = hashKey;
        this.optionalKeyStreamNum = optionalKeyStreamNum;
        this.coercionType = coercionType;
    }

    public Integer getOptionalKeyStreamNum() {
        return optionalKeyStreamNum;
    }

    public QueryGraphValueEntryHashKeyedForge getHashKey() {
        return hashKey;
    }

    public EPTypeClass getCoercionType() {
        return coercionType;
    }
}
