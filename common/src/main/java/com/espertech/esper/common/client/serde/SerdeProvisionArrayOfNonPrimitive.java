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
package com.espertech.esper.common.client.serde;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.serde.serdeset.builtin.DIONullableObjectArraySerdeForge;

/**
 * For use with high-availability and scale-out only, this class instructs the compiler that the serializer and de-serializer (serde)
 * is available for a given component type and component type serde
 */
public class SerdeProvisionArrayOfNonPrimitive extends SerdeProvision {
    private final EPTypeClass componentType;
    private final SerdeProvision componentSerde;

    public SerdeProvisionArrayOfNonPrimitive(EPTypeClass componentType, SerdeProvision componentSerde) {
        this.componentType = componentType;
        this.componentSerde = componentSerde;
    }

    public DataInputOutputSerdeForge toForge() {
        return new DIONullableObjectArraySerdeForge(componentType, componentSerde.toForge());
    }
}
