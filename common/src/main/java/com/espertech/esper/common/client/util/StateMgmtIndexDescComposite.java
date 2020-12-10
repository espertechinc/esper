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
package com.espertech.esper.common.client.util;

import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

public class StateMgmtIndexDescComposite {
    private final String[] indexedProps;
    private final MultiKeyClassRef multiKeyPlan;
    private final String[] indexedRangeProps;
    private final DataInputOutputSerdeForge[] rangeSerdes;

    public StateMgmtIndexDescComposite(String[] indexedProps, MultiKeyClassRef multiKeyPlan, String[] indexedRangeProps, DataInputOutputSerdeForge[] rangeSerdes) {
        this.indexedProps = indexedProps;
        this.multiKeyPlan = multiKeyPlan;
        this.indexedRangeProps = indexedRangeProps;
        this.rangeSerdes = rangeSerdes;
    }

    public String[] getIndexedProps() {
        return indexedProps;
    }

    public MultiKeyClassRef getMultiKeyPlan() {
        return multiKeyPlan;
    }

    public String[] getIndexedRangeProps() {
        return indexedRangeProps;
    }

    public DataInputOutputSerdeForge[] getRangeSerdes() {
        return rangeSerdes;
    }
}
