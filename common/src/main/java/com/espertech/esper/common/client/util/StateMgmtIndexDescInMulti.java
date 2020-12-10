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

import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

public class StateMgmtIndexDescInMulti {
    private final String[] indexedProps;
    private final DataInputOutputSerdeForge[] serdes;

    public StateMgmtIndexDescInMulti(String[] indexedProps, DataInputOutputSerdeForge[] serdes) {
        this.indexedProps = indexedProps;
        this.serdes = serdes;
    }

    public String[] getIndexedProps() {
        return indexedProps;
    }

    public DataInputOutputSerdeForge[] getSerdes() {
        return serdes;
    }
}
