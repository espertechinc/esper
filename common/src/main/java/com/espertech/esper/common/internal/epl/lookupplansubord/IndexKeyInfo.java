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
package com.espertech.esper.common.internal.epl.lookupplansubord;

import com.espertech.esper.common.internal.epl.join.queryplan.CoercionDesc;
import com.espertech.esper.common.internal.epl.lookupplan.SubordPropHashKeyForge;
import com.espertech.esper.common.internal.epl.lookupplan.SubordPropRangeKeyForge;

import java.util.List;

public class IndexKeyInfo {

    private List<SubordPropHashKeyForge> orderedKeyProperties;
    private CoercionDesc orderedKeyCoercionTypes;
    private List<SubordPropRangeKeyForge> orderedRangeDesc;
    private CoercionDesc orderedRangeCoercionTypes;

    public IndexKeyInfo(List<SubordPropHashKeyForge> orderedKeyProperties, CoercionDesc orderedKeyCoercionTypes, List<SubordPropRangeKeyForge> orderedRangeDesc, CoercionDesc orderedRangeCoercionTypes) {
        this.orderedKeyProperties = orderedKeyProperties;
        this.orderedKeyCoercionTypes = orderedKeyCoercionTypes;
        this.orderedRangeDesc = orderedRangeDesc;
        this.orderedRangeCoercionTypes = orderedRangeCoercionTypes;
    }

    public List<SubordPropHashKeyForge> getOrderedHashDesc() {
        return orderedKeyProperties;
    }

    public CoercionDesc getOrderedKeyCoercionTypes() {
        return orderedKeyCoercionTypes;
    }

    public List<SubordPropRangeKeyForge> getOrderedRangeDesc() {
        return orderedRangeDesc;
    }

    public CoercionDesc getOrderedRangeCoercionTypes() {
        return orderedRangeCoercionTypes;
    }
}
