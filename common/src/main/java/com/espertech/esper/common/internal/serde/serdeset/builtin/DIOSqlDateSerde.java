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
import java.sql.Date;

public class DIOSqlDateSerde implements DataInputOutputSerde<Date> {
    public final static DIOSqlDateSerde INSTANCE = new DIOSqlDateSerde();

    private DIOSqlDateSerde() {
    }

    public void write(java.sql.Date object, DataOutput output) throws IOException {
        writeInternal(object, output);
    }

    public java.sql.Date read(DataInput input) throws IOException {
        return readInternal(input);
    }

    public void write(java.sql.Date object, DataOutput output, byte[] unitKey, EventBeanCollatedWriter writer) throws IOException {
        writeInternal(object, output);
    }

    public java.sql.Date read(DataInput input, byte[] unitKey) throws IOException {
        return readInternal(input);
    }

    protected static void writeInternal(java.sql.Date object, DataOutput output) throws IOException {
        if (object == null) {
            output.writeLong(-1);
            return;
        }
        output.writeLong(object.getTime());
    }

    protected static java.sql.Date readInternal(DataInput input) throws IOException {
        long value = input.readLong();
        if (value == -1) {
            return null;
        }
        return new java.sql.Date(value);
    }
}
