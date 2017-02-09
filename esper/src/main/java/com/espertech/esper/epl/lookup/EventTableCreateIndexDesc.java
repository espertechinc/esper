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

import java.util.Arrays;
import java.util.List;

public class EventTableCreateIndexDesc {
    private final List<IndexedPropDesc> hashProps;
    private final List<IndexedPropDesc> btreeProps;
    private final boolean unique;

    public EventTableCreateIndexDesc(List<IndexedPropDesc> hashProps, List<IndexedPropDesc> btreeProps, boolean unique) {
        this.hashProps = hashProps;
        this.btreeProps = btreeProps;
        this.unique = unique;
    }

    public List<IndexedPropDesc> getHashProps() {
        return hashProps;
    }

    public List<IndexedPropDesc> getBtreeProps() {
        return btreeProps;
    }

    public boolean isUnique() {
        return unique;
    }

    public static EventTableCreateIndexDesc fromMultiKey(IndexMultiKey multiKey) {
        return new EventTableCreateIndexDesc(
                Arrays.asList(multiKey.getHashIndexedProps()),
                Arrays.asList(multiKey.getRangeIndexedProps()),
                multiKey.isUnique());
    }
}
