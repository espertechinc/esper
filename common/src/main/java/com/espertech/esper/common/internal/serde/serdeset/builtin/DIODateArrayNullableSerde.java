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
import java.util.Date;

public class DIODateArrayNullableSerde implements DataInputOutputSerde<Date[]> {
    public final static DIODateArrayNullableSerde INSTANCE = new DIODateArrayNullableSerde();

    private DIODateArrayNullableSerde() {
    }

    public void write(Date[] object, DataOutput output) throws IOException {
        writeInternal(object, output);
    }

    public Date[] read(DataInput input) throws IOException {
        return readInternal(input);
    }

    public void write(Date[] object, DataOutput output, byte[] unitKey, EventBeanCollatedWriter writer) throws IOException {
        writeInternal(object, output);
    }

    public Date[] read(DataInput input, byte[] unitKey) throws IOException {
        return readInternal(input);
    }

    private void writeInternal(Date[] object, DataOutput output) throws IOException {
        if (object == null) {
            output.writeInt(-1);
            return;
        }
        output.writeInt(object.length);
        for (Date i : object) {
            DIODateSerde.INSTANCE.write(i, output);
        }
    }

    private Date[] readInternal(DataInput input) throws IOException {
        int len = input.readInt();
        if (len == -1) {
            return null;
        }
        Date[] array = new Date[len];
        for (int i = 0; i < len; i++) {
            array[i] = DIODateSerde.INSTANCE.read(input);
        }
        return array;
    }
}
