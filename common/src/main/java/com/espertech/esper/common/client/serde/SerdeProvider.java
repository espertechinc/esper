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

/**
 * For use with high-availability and scale-out only, provider for serde (serializer and deserializer) information
 * to the compiler.
 */
public interface SerdeProvider {

    /**
     * Returns a serde provision or null if none can be determined.
     * @param context provider context object
     * @return null for no serde available, or the serde provider descriptor for the compiler
     */
    SerdeProvision resolveSerdeForClass(SerdeProviderContextClass context);

    /**
     * Returns a serde for a map or object-array event type or null for using the default serde
     * @param context provides information about the event type
     * @return null to use the default runtime serde, or the serde provider descriptor for the compiler
     */
    default SerdeProvision resolveSerdeForEventType(SerdeProviderEventTypeContext context) {
        return null;
    }
}
