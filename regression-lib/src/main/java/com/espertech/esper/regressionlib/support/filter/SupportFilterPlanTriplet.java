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
package com.espertech.esper.regressionlib.support.filter;

import com.espertech.esper.common.internal.filterspec.FilterOperator;

public class SupportFilterPlanTriplet {
    private String lookupable;
    private FilterOperator op;
    private String value;
    private String controlConfirm;

    public SupportFilterPlanTriplet(String lookupable, FilterOperator op, String value) {
        this.lookupable = lookupable;
        this.op = op;
        this.value = value;
    }

    public SupportFilterPlanTriplet(String lookupable, FilterOperator op, String value, String controlConfirm) {
        this.lookupable = lookupable;
        this.op = op;
        this.value = value;
        this.controlConfirm = controlConfirm;
    }

    public String getLookupable() {
        return lookupable;
    }

    public FilterOperator getOp() {
        return op;
    }

    public String getValue() {
        return value;
    }

    public String getControlConfirm() {
        return controlConfirm;
    }

    public String toString() {
        return "SupportFilterPlanTriplet{" +
            "lookupable='" + lookupable + '\'' +
            ", op=" + op +
            ", value='" + value + '\'' +
            ", controlConfirm='" + controlConfirm + '\'' +
            '}';
    }
}
