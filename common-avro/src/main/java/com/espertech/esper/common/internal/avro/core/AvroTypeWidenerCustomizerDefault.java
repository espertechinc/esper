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

import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.TypeWidenerCustomizer;
import com.espertech.esper.common.internal.util.TypeWidenerSPI;

import java.nio.ByteBuffer;
import java.util.Collection;

import static com.espertech.esper.common.internal.util.TypeWidenerFactory.BYTE_ARRAY_TO_BYTE_BUFFER_COERCER;
import static com.espertech.esper.common.internal.util.TypeWidenerFactory.getArrayToCollectionCoercer;

public class AvroTypeWidenerCustomizerDefault implements TypeWidenerCustomizer {
    public final static AvroTypeWidenerCustomizerDefault INSTANCE = new AvroTypeWidenerCustomizerDefault();

    private AvroTypeWidenerCustomizerDefault() {
    }

    public TypeWidenerSPI widenerFor(String columnName, Class columnType, Class writeablePropertyType, String writeablePropertyName, String statementName) {
        if (columnType == byte[].class && writeablePropertyType == ByteBuffer.class) {
            return BYTE_ARRAY_TO_BYTE_BUFFER_COERCER;
        } else if (columnType != null && columnType.isArray() && JavaClassHelper.isImplementsInterface(writeablePropertyType, Collection.class)) {
            return getArrayToCollectionCoercer(columnType.getComponentType());
        }
        return null;
    }
}
