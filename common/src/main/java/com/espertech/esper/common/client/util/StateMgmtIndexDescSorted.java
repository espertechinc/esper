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

public class StateMgmtIndexDescSorted {
    private final String property;
    private final DataInputOutputSerdeForge serde;

    public StateMgmtIndexDescSorted(String property, DataInputOutputSerdeForge serde) {
        this.property = property;
        this.serde = serde;
    }

    public String getProperty() {
        return property;
    }

    public DataInputOutputSerdeForge getSerde() {
        return serde;
    }
}
