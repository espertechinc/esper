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
package com.espertech.esper.common.internal.serde.serdeset.builtin;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.client.serde.EventBeanCollatedWriter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class DIOUnsupportedSerde implements DataInputOutputSerde {
    public final static DIOUnsupportedSerde INSTANCE = new DIOUnsupportedSerde();

    private DIOUnsupportedSerde() {
    }

    public void write(Object object, DataOutput output, byte[] unitKey, EventBeanCollatedWriter writer) throws IOException {
        throw new UnsupportedOperationException("Operation not supported");
    }

    public Object read(DataInput input, byte[] unitKey) throws IOException {
        throw new UnsupportedOperationException("Operation not supported");
    }
}
