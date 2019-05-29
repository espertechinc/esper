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
package com.espertech.esper.common.internal.serde.compiletime.resolve;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.serde.serdeset.builtin.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;

public class VMExtendedBuiltinSerdeFactory {
    public static DataInputOutputSerde getSerde(Class type) {
        if (type == BigInteger.class) {
            return DIOBigIntegerSerde.INSTANCE;
        }
        if (type == BigDecimal.class) {
            return DIOBigDecimalSerde.INSTANCE;
        }
        if (type == Date.class) {
            return DIODateSerde.INSTANCE;
        }
        if (type == java.sql.Date.class) {
            return DIOSqlDateSerde.INSTANCE;
        }
        if (type == Calendar.class) {
            return DIOCalendarSerde.INSTANCE;
        }
        if (type.isArray()) {
            Class componentType = type.getComponentType();
            if (componentType == int.class) {
                return DIOPrimitiveIntArrayNullableSerde.INSTANCE;
            }
            if (componentType == boolean.class) {
                return DIOPrimitiveBooleanArrayNullableSerde.INSTANCE;
            }
            if (componentType == char.class) {
                return DIOPrimitiveCharArrayNullableSerde.INSTANCE;
            }
            if (componentType == byte.class) {
                return DIOPrimitiveByteArrayNullableSerde.INSTANCE;
            }
            if (componentType == short.class) {
                return DIOPrimitiveShortArrayNullableSerde.INSTANCE;
            }
            if (componentType == long.class) {
                return DIOPrimitiveLongArrayNullableSerde.INSTANCE;
            }
            if (componentType == float.class) {
                return DIOPrimitiveFloatArrayNullableSerde.INSTANCE;
            }
            if (componentType == double.class) {
                return DIOPrimitiveDoubleArrayNullableSerde.INSTANCE;
            }
            if (componentType == String.class) {
                return DIOStringArrayNullableSerde.INSTANCE;
            }
            if (componentType == Character.class) {
                return DIOBoxedCharacterArrayNullableSerde.INSTANCE;
            }
            if (componentType == Boolean.class) {
                return DIOBoxedBooleanArrayNullableSerde.INSTANCE;
            }
            if (componentType == Byte.class) {
                return DIOBoxedByteArrayNullableSerde.INSTANCE;
            }
            if (componentType == Short.class) {
                return DIOBoxedShortArrayNullableSerde.INSTANCE;
            }
            if (componentType == Integer.class) {
                return DIOBoxedIntegerArrayNullableSerde.INSTANCE;
            }
            if (componentType == Long.class) {
                return DIOBoxedLongArrayNullableSerde.INSTANCE;
            }
            if (componentType == Float.class) {
                return DIOBoxedFloatArrayNullableSerde.INSTANCE;
            }
            if (componentType == Double.class) {
                return DIOBoxedDoubleArrayNullableSerde.INSTANCE;
            }
            if (componentType == BigDecimal.class) {
                return DIOBigDecimalArrayNullableSerde.INSTANCE;
            }
            if (componentType == BigInteger.class) {
                return DIOBigIntegerArrayNullableSerde.INSTANCE;
            }
            if (componentType == Date.class) {
                return DIODateArrayNullableSerde.INSTANCE;
            }
            if (componentType == java.sql.Date.class) {
                return DIOSqlDateArrayNullableSerde.INSTANCE;
            }
            if (componentType == Calendar.class) {
                return DIOCalendarArrayNullableSerde.INSTANCE;
            }
        }
        return null;
    }
}
