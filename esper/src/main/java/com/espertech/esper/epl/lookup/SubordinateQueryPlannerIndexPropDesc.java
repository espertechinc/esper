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

public class SubordinateQueryPlannerIndexPropDesc {
    private final String[] hashIndexPropsProvided;
    private final Class[] hashIndexCoercionType;
    private final String[] rangeIndexPropsProvided;
    private final Class[] rangeIndexCoercionType;
    private final SubordinateQueryPlannerIndexPropListPair listPair;
    private final SubordPropHashKey[] hashJoinedProps;
    private final SubordPropRangeKey[] rangeJoinedProps;

    public SubordinateQueryPlannerIndexPropDesc(String[] hashIndexPropsProvided, Class[] hashIndexCoercionType, String[] rangeIndexPropsProvided, Class[] rangeIndexCoercionType, SubordinateQueryPlannerIndexPropListPair listPair, SubordPropHashKey[] hashJoinedProps, SubordPropRangeKey[] rangeJoinedProps) {
        this.hashIndexPropsProvided = hashIndexPropsProvided;
        this.hashIndexCoercionType = hashIndexCoercionType;
        this.rangeIndexPropsProvided = rangeIndexPropsProvided;
        this.rangeIndexCoercionType = rangeIndexCoercionType;
        this.listPair = listPair;
        this.hashJoinedProps = hashJoinedProps;
        this.rangeJoinedProps = rangeJoinedProps;
    }

    public String[] getHashIndexPropsProvided() {
        return hashIndexPropsProvided;
    }

    public Class[] getHashIndexCoercionType() {
        return hashIndexCoercionType;
    }

    public String[] getRangeIndexPropsProvided() {
        return rangeIndexPropsProvided;
    }

    public Class[] getRangeIndexCoercionType() {
        return rangeIndexCoercionType;
    }

    public SubordinateQueryPlannerIndexPropListPair getListPair() {
        return listPair;
    }

    public SubordPropHashKey[] getHashJoinedProps() {
        return hashJoinedProps;
    }

    public SubordPropRangeKey[] getRangeJoinedProps() {
        return rangeJoinedProps;
    }
}
