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
import java.util.Calendar;
import java.util.TimeZone;

public class DIOCalendarSerde implements DataInputOutputSerde<Calendar> {
    public final static DIOCalendarSerde INSTANCE = new DIOCalendarSerde();

    private DIOCalendarSerde() {
    }

    public void write(Calendar object, DataOutput output) throws IOException {
        writeCalendar(object, output);
    }

    public Calendar read(DataInput input) throws IOException {
        return readCalendar(input);
    }

    public void write(Calendar object, DataOutput output, byte[] unitKey, EventBeanCollatedWriter writer) throws IOException {
        writeCalendar(object, output);
    }

    public Calendar read(DataInput input, byte[] unitKey) throws IOException {
        return readCalendar(input);
    }

    public static void writeCalendar(Calendar cal, DataOutput output) throws IOException {
        if (cal == null) {
            output.writeBoolean(true);
            return;
        }
        output.writeBoolean(false);
        output.writeUTF(cal.getTimeZone().getID());
        output.writeLong(cal.getTimeInMillis());
    }

    public static Calendar readCalendar(DataInput input) throws IOException {
        boolean isNull = input.readBoolean();
        if (isNull) {
            return null;
        }
        String timeZoneId = input.readUTF();
        long millis = input.readLong();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId));
        cal.setTimeInMillis(millis);
        return cal;
    }
}
