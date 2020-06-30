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
package com.espertech.esper.common.internal.avro.core;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.type.*;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import org.apache.avro.Schema;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_KEY;
import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_VALUE;

public class AvroTypeUtil {

    private final static EPType[] TYPES_PER_AVRO_ORD;

    static {
        TYPES_PER_AVRO_ORD = new EPType[Schema.Type.values().length];
        for (int ord = 0; ord < Schema.Type.values().length; ord++) {
            Schema.Type type = Schema.Type.values()[ord];
            if (type == Schema.Type.INT) {
                TYPES_PER_AVRO_ORD[ord] = EPTypePremade.INTEGERBOXED.getEPType();
            } else if (type == Schema.Type.LONG) {
                TYPES_PER_AVRO_ORD[ord] = EPTypePremade.LONGBOXED.getEPType();
            } else if (type == Schema.Type.DOUBLE) {
                TYPES_PER_AVRO_ORD[ord] = EPTypePremade.DOUBLEBOXED.getEPType();
            } else if (type == Schema.Type.FLOAT) {
                TYPES_PER_AVRO_ORD[ord] = EPTypePremade.FLOATBOXED.getEPType();
            } else if (type == Schema.Type.BOOLEAN) {
                TYPES_PER_AVRO_ORD[ord] = EPTypePremade.BOOLEANBOXED.getEPType();
            } else if (type == Schema.Type.BYTES) {
                TYPES_PER_AVRO_ORD[ord] = EPTypePremade.BYTEBUFFER.getEPType();
            } else if (type == Schema.Type.NULL) {
                TYPES_PER_AVRO_ORD[ord] = EPTypeNull.INSTANCE;
            }
        }
    }

    public static EPType propertyType(Schema fieldSchema) {
        if (fieldSchema.getType() == Schema.Type.UNION) {
            boolean hasNull = false;
            Set<EPType> unionTypes = new HashSet<>();
            for (Schema memberSchema : fieldSchema.getTypes()) {
                if (memberSchema.getType() == Schema.Type.NULL) {
                    hasNull = true;
                } else {
                    EPType type = propertyType(memberSchema);
                    if (type != null) {
                        unionTypes.add(type);
                    }
                }
            }
            if (unionTypes.isEmpty()) {
                return null;
            }
            if (unionTypes.size() == 1) {
                EPType type = unionTypes.iterator().next();
                if (hasNull) {
                    return JavaClassHelper.getBoxedType(type);
                }
                return type;
            }
            boolean allNumeric = true;
            for (EPType unioned : unionTypes) {
                if (!JavaClassHelper.isNumeric(unioned)) {
                    allNumeric = false;
                }
            }
            if (allNumeric) {
                return EPTypePremade.NUMBER.getEPType();
            }
            return EPTypePremade.OBJECT.getEPType();
        } else if (fieldSchema.getType() == Schema.Type.RECORD) {
            return AvroConstant.EPTYPE_RECORD;
        } else if (fieldSchema.getType() == Schema.Type.ARRAY) {
            EPType componentType = AvroTypeUtil.propertyType(fieldSchema.getElementType());
            if (componentType == null || componentType == EPTypeNull.INSTANCE) {
                return new EPTypeClass(Collection.class);
            }
            return new EPTypeClassParameterized(Collection.class, new EPTypeClass[]{(EPTypeClass) componentType});
        } else if (fieldSchema.getType() == Schema.Type.MAP) {
            EPTypeClass keyType = EPTypePremade.STRING.getEPType();
            EPType valueType = AvroTypeUtil.propertyType(fieldSchema.getValueType());
            if (valueType == null || valueType == EPTypeNull.INSTANCE) {
                throw new EPException("Invalid null value type for map");
            }
            return new EPTypeClassParameterized(Map.class, new EPTypeClass[]{keyType, (EPTypeClass) valueType});
        } else if (fieldSchema.getType() == Schema.Type.FIXED) {
            return AvroConstant.EPTYPE_GENERICFIXED;
        } else if (fieldSchema.getType() == Schema.Type.ENUM) {
            return AvroConstant.EPTYPE_GENERICENUMSYMBOL;
        } else if (fieldSchema.getType() == Schema.Type.STRING) {
            String prop = fieldSchema.getProp(PROP_JAVA_STRING_KEY);
            return prop == null || !prop.equals(PROP_JAVA_STRING_VALUE) ? EPTypePremade.CHARSEQUENCE.getEPType() : EPTypePremade.STRING.getEPType();
        }
        return TYPES_PER_AVRO_ORD[fieldSchema.getType().ordinal()];
    }
}
