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
package com.espertech.esper.epl.lookup;

import com.espertech.esper.epl.join.plan.QueryGraphValueEntryHashKeyed;

import java.io.Serializable;

/**
 * Holds property information for joined properties in a lookup.
 */
public class SubordPropHashKey implements Serializable {
    private static final long serialVersionUID = -8830134829029646585L;

    private final QueryGraphValueEntryHashKeyed hashKey;
    private final Integer optionalKeyStreamNum;
    private final Class coercionType;

    public SubordPropHashKey(QueryGraphValueEntryHashKeyed hashKey, Integer optionalKeyStreamNum, Class coercionType) {
        this.hashKey = hashKey;
        this.optionalKeyStreamNum = optionalKeyStreamNum;
        this.coercionType = coercionType;
    }

    public Integer getOptionalKeyStreamNum() {
        return optionalKeyStreamNum;
    }

    public QueryGraphValueEntryHashKeyed getHashKey() {
        return hashKey;
    }

    public Class getCoercionType() {
        return coercionType;
    }
}
