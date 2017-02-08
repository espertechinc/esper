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

import java.util.ArrayList;
import java.util.List;

public class SubordinateQueryPlannerIndexPropListPair {
    private List<IndexedPropDesc> hashedProps = new ArrayList<IndexedPropDesc>();
    private List<IndexedPropDesc> btreeProps = new ArrayList<IndexedPropDesc>();

    public SubordinateQueryPlannerIndexPropListPair(List<IndexedPropDesc> hashedProps, List<IndexedPropDesc> btreeProps) {
        this.hashedProps = hashedProps;
        this.btreeProps = btreeProps;
    }

    public List<IndexedPropDesc> getHashedProps() {
        return hashedProps;
    }

    public List<IndexedPropDesc> getBtreeProps() {
        return btreeProps;
    }
}
