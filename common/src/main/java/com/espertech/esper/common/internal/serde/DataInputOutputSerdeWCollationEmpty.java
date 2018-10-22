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
package com.espertech.esper.common.internal.serde;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class DataInputOutputSerdeWCollationEmpty implements DataInputOutputSerdeWCollation<Object> {

    public final static DataInputOutputSerdeWCollationEmpty INSTANCE = new DataInputOutputSerdeWCollationEmpty();

    private DataInputOutputSerdeWCollationEmpty() {
    }

    public void write(java.lang.Object object, DataOutput output, byte[] unitKey, EventBeanCollatedWriter writer) throws IOException {
    }

    public Object read(DataInput input, byte[] unitKey) throws IOException {
        return null;
    }
}
