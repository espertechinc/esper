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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.serde.serdeset.builtin.*;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class VMExtendedBuiltinSerdeFactory {
    private final static Map<EPTypeClass, DataInputOutputSerde> SERDES = new HashMap<>();
    private static Map<String, DataInputOutputSerde> byPrettyName = null;

    static {
        SERDES.put(EPTypePremade.BIGINTEGER.getEPType(), DIOBigIntegerSerde.INSTANCE);
        SERDES.put(EPTypePremade.BIGDECIMAL.getEPType(), DIOBigDecimalSerde.INSTANCE);
        SERDES.put(EPTypePremade.DATE.getEPType(), DIODateSerde.INSTANCE);
        SERDES.put(EPTypePremade.SQLDATE.getEPType(), DIOSqlDateSerde.INSTANCE);
        SERDES.put(EPTypePremade.CALENDAR.getEPType(), DIOCalendarSerde.INSTANCE);

        SERDES.put(EPTypePremade.INTEGERPRIMITIVEARRAY.getEPType(), DIOPrimitiveIntArrayNullableSerde.INSTANCE);
        SERDES.put(EPTypePremade.BOOLEANPRIMITIVEARRAY.getEPType(), DIOPrimitiveBooleanArrayNullableSerde.INSTANCE);
        SERDES.put(EPTypePremade.CHARPRIMITIVEARRAY.getEPType(), DIOPrimitiveCharArrayNullableSerde.INSTANCE);
        SERDES.put(EPTypePremade.BYTEPRIMITIVEARRAY.getEPType(), DIOPrimitiveByteArrayNullableSerde.INSTANCE);
        SERDES.put(EPTypePremade.SHORTPRIMITIVEARRAY.getEPType(), DIOPrimitiveShortArrayNullableSerde.INSTANCE);
        SERDES.put(EPTypePremade.LONGPRIMITIVEARRAY.getEPType(), DIOPrimitiveLongArrayNullableSerde.INSTANCE);
        SERDES.put(EPTypePremade.FLOATPRIMITIVEARRAY.getEPType(), DIOPrimitiveFloatArrayNullableSerde.INSTANCE);
        SERDES.put(EPTypePremade.DOUBLEPRIMITIVEARRAY.getEPType(), DIOPrimitiveDoubleArrayNullableSerde.INSTANCE);
        SERDES.put(EPTypePremade.STRINGARRAY.getEPType(), DIOStringArrayNullableSerde.INSTANCE);
        SERDES.put(EPTypePremade.CHARBOXEDARRAY.getEPType(), DIOBoxedCharacterArrayNullableSerde.INSTANCE);
        SERDES.put(EPTypePremade.BOOLEANBOXEDARRAY.getEPType(), DIOBoxedBooleanArrayNullableSerde.INSTANCE);
        SERDES.put(EPTypePremade.BYTEBOXEDARRAY.getEPType(), DIOBoxedByteArrayNullableSerde.INSTANCE);
        SERDES.put(EPTypePremade.SHORTBOXEDARRAY.getEPType(), DIOBoxedShortArrayNullableSerde.INSTANCE);
        SERDES.put(EPTypePremade.INTEGERBOXEDARRAY.getEPType(), DIOBoxedIntegerArrayNullableSerde.INSTANCE);
        SERDES.put(EPTypePremade.LONGBOXEDARRAY.getEPType(), DIOBoxedLongArrayNullableSerde.INSTANCE);
        SERDES.put(EPTypePremade.FLOATBOXEDARRAY.getEPType(), DIOBoxedFloatArrayNullableSerde.INSTANCE);
        SERDES.put(EPTypePremade.DOUBLEBOXEDARRAY.getEPType(), DIOBoxedDoubleArrayNullableSerde.INSTANCE);
        SERDES.put(EPTypePremade.BIGDECIMALARRAY.getEPType(), DIOBigDecimalArrayNullableSerde.INSTANCE);
        SERDES.put(EPTypePremade.BIGINTEGERARRAY.getEPType(), DIOBigIntegerArrayNullableSerde.INSTANCE);
        SERDES.put(EPTypePremade.DATEARRAY.getEPType(), DIODateArrayNullableSerde.INSTANCE);
        SERDES.put(EPTypePremade.SQLDATEARRAY.getEPType(), DIOSqlDateArrayNullableSerde.INSTANCE);
        SERDES.put(EPTypePremade.CALENDARARRAY.getEPType(), DIOCalendarArrayNullableSerde.INSTANCE);

        addArrayList(String.class, DIOArrayListStringNullableSerde.INSTANCE);
        addArrayList(Character.class, DIOArrayListCharacterNullableSerde.INSTANCE);
        addArrayList(Boolean.class, DIOArrayListBooleanNullableSerde.INSTANCE);
        addArrayList(Byte.class, DIOArrayListByteNullableSerde.INSTANCE);
        addArrayList(Short.class, DIOArrayListShortNullableSerde.INSTANCE);
        addArrayList(Integer.class, DIOArrayListIntegerNullableSerde.INSTANCE);
        addArrayList(Long.class, DIOArrayListLongNullableSerde.INSTANCE);
        addArrayList(Float.class, DIOArrayListFloatNullableSerde.INSTANCE);
        addArrayList(Double.class, DIOArrayListDoubleNullableSerde.INSTANCE);
        addArrayList(BigDecimal.class, DIOArrayListBigDecimalNullableSerde.INSTANCE);
        addArrayList(BigInteger.class, DIOArrayListBigIntegerNullableSerde.INSTANCE);
        addArrayList(Date.class, DIOArrayListDateNullableSerde.INSTANCE);
        addArrayList(java.sql.Date.class, DIOArrayListSqlDateNullableSerde.INSTANCE);
        addArrayList(Calendar.class, DIOArrayListCalendarNullableSerde.INSTANCE);
    }

    private static void addArrayList(Class component, DataInputOutputSerde serde) {
        SERDES.put(EPTypeClassParameterized.from(Collection.class, component), serde);
        SERDES.put(EPTypeClassParameterized.from(List.class, component), serde);
        SERDES.put(EPTypeClassParameterized.from(ArrayList.class, component), serde);
    }

    public static DataInputOutputSerde getSerde(EPTypeClass typeClass) {
        return SERDES.get(typeClass);
    }

    public synchronized static DataInputOutputSerde getSerde(String classNamePretty) {
        if (byPrettyName == null) {
            byPrettyName = new HashMap<>();
            for (Map.Entry<EPTypeClass, DataInputOutputSerde> serde : SERDES.entrySet()) {
                EPTypeClass clazz = serde.getKey();
                String pretty = JavaClassHelper.getClassNameNormalized(clazz);
                if (byPrettyName.containsKey(pretty)) {
                    throw new IllegalStateException("Duplicate key '" + pretty + "'");
                }
                byPrettyName.put(pretty, serde.getValue());
            }
        }
        return byPrettyName.get(classNamePretty);
    }
}
