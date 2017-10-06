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
package com.espertech.esper.epl.agg.service.table;

public enum AggSvcTableGetterType {
    GETVALUE("getValue"),
    GETCOLLECTIONOFEVENTS("getEnumerableEvents"),
    GETCOLLECTIONSCALAR("getEnumerableScalar"),
    GETEVENTBEAN("getEnumerableEvent");

    private final String accessorMethod;

    AggSvcTableGetterType(String accessorMethod) {
        this.accessorMethod = accessorMethod;
    }

    public String getAccessorMethod() {
        return accessorMethod;
    }
}
