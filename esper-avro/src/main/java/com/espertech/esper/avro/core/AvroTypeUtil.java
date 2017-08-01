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
package com.espertech.esper.avro.core;

import com.espertech.esper.util.JavaClassHelper;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericEnumSymbol;
import org.apache.avro.generic.GenericFixed;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.avro.core.AvroConstant.PROP_JAVA_STRING_KEY;
import static com.espertech.esper.avro.core.AvroConstant.PROP_JAVA_STRING_VALUE;

public class AvroTypeUtil {

    private final static AvroTypeDesc[] TYPES_PER_AVRO_ORD;

    static {
        TYPES_PER_AVRO_ORD = new AvroTypeDesc[Schema.Type.values().length];
        for (int ord = 0; ord < Schema.Type.values().length; ord++) {
            Schema.Type type = Schema.Type.values()[ord];
            if (type == Schema.Type.INT) {
                TYPES_PER_AVRO_ORD[ord] = new AvroTypeDesc(Integer.class);
            } else if (type == Schema.Type.LONG) {
                TYPES_PER_AVRO_ORD[ord] = new AvroTypeDesc(Long.class);
            } else if (type == Schema.Type.DOUBLE) {
                TYPES_PER_AVRO_ORD[ord] = new AvroTypeDesc(Double.class);
            } else if (type == Schema.Type.FLOAT) {
                TYPES_PER_AVRO_ORD[ord] = new AvroTypeDesc(Float.class);
            } else if (type == Schema.Type.BOOLEAN) {
                TYPES_PER_AVRO_ORD[ord] = new AvroTypeDesc(Boolean.class);
            } else if (type == Schema.Type.BYTES) {
                TYPES_PER_AVRO_ORD[ord] = new AvroTypeDesc(ByteBuffer.class);
            } else if (type == Schema.Type.NULL) {
                TYPES_PER_AVRO_ORD[ord] = new AvroTypeDesc(null);
            }
        }
    }

    public static Class propertyType(Schema fieldSchema) {
        if (fieldSchema.getType() == Schema.Type.UNION) {
            boolean hasNull = false;
            Set<Class> unionTypes = new HashSet<>();
            for (Schema memberSchema : fieldSchema.getTypes()) {
                if (memberSchema.getType() == Schema.Type.NULL) {
                    hasNull = true;
                } else {
                    Class type = propertyType(memberSchema);
                    if (type != null) {
                        unionTypes.add(type);
                    }
                }
            }
            if (unionTypes.isEmpty()) {
                return null;
            }
            if (unionTypes.size() == 1) {
                if (hasNull) {
                    return JavaClassHelper.getBoxedType(unionTypes.iterator().next());
                }
                return unionTypes.iterator().next();
            }
            boolean allNumeric = true;
            for (Class unioned : unionTypes) {
                if (!JavaClassHelper.isNumeric(unioned)) {
                    allNumeric = false;
                }
            }
            if (allNumeric) {
                return Number.class;
            }
            return Object.class;
        } else if (fieldSchema.getType() == Schema.Type.RECORD) {
            return GenericData.Record.class;
        } else if (fieldSchema.getType() == Schema.Type.ARRAY) {
            return Collection.class;
        } else if (fieldSchema.getType() == Schema.Type.MAP) {
            return Map.class;
        } else if (fieldSchema.getType() == Schema.Type.FIXED) {
            return GenericFixed.class;
        } else if (fieldSchema.getType() == Schema.Type.ENUM) {
            return GenericEnumSymbol.class;
        } else if (fieldSchema.getType() == Schema.Type.STRING) {
            String prop = fieldSchema.getProp(PROP_JAVA_STRING_KEY);
            return prop == null || !prop.equals(PROP_JAVA_STRING_VALUE) ? CharSequence.class : String.class;
        }
        AvroTypeDesc desc = TYPES_PER_AVRO_ORD[fieldSchema.getType().ordinal()];
        return desc == null ? null : desc.getType();
    }
}
