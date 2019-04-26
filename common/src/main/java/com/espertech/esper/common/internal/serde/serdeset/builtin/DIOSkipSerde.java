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

public class DIOSkipSerde implements DataInputOutputSerde {

    public final static DIOSkipSerde INSTANCE = new DIOSkipSerde();

    private DIOSkipSerde() {
    }

    public void write(Object object, DataOutput output, byte[] pageFullKey, EventBeanCollatedWriter writer) throws IOException {
    }

    public Object read(DataInput s, byte[] resourceKey) throws IOException {
        return null;
    }

    public void write(Object object, DataOutput output) throws IOException {

    }

    public Object read(DataInput input) throws IOException {
        return null;
    }
}
