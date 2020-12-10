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
package com.espertech.esper.common.internal.serde.serdeset.multikey;

import java.util.HashMap;
import java.util.Map;

public class DIOMultiKeyArraySerdeFactory {
    private static final Map<Class, DIOMultiKeyArraySerde> COMPONENT = new HashMap<>();
    private static Map<String, DIOMultiKeyArraySerde> byPrettyName = null;

    static {
        add(DIOMultiKeyArrayCharSerde.INSTANCE);
        add(DIOMultiKeyArrayBooleanSerde.INSTANCE);
        add(DIOMultiKeyArrayByteSerde.INSTANCE);
        add(DIOMultiKeyArrayShortSerde.INSTANCE);
        add(DIOMultiKeyArrayIntSerde.INSTANCE);
        add(DIOMultiKeyArrayLongSerde.INSTANCE);
        add(DIOMultiKeyArrayFloatSerde.INSTANCE);
        add(DIOMultiKeyArrayDoubleSerde.INSTANCE);
        add(DIOMultiKeyArrayObjectSerde.INSTANCE);
    }

    private static void add(DIOMultiKeyArraySerde<?> serde) {
        COMPONENT.put(serde.componentType(), serde);
    }

    /**
     * Returns the serde for the given Java built-in type.
     *
     * @param cls is the Java type
     * @return serde for marshalling and unmarshalling that type
     */
    public static DIOMultiKeyArraySerde<?> getSerde(Class<?> cls) {
        return COMPONENT.get(cls);
    }

    public static DIOMultiKeyArraySerde<?> getSerde(String classNamePretty) {
        if (byPrettyName == null) {
            byPrettyName = new HashMap<>();
            for (Map.Entry<Class, DIOMultiKeyArraySerde> serde : COMPONENT.entrySet()) {
                byPrettyName.put(serde.getKey().getSimpleName(), serde.getValue());
            }
        }
        return byPrettyName.get(classNamePretty);
    }
}
