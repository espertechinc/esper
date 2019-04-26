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
 * For use with high-availability and scale-out only, this class provides contextual information about the class that we
 * looking to serialize or de-serialize, for use with {@link SerdeProvider}
 */
public class SerdeProviderContextClass {
    private final Class clazz;
    private final SerdeProviderAdditionalInfo additionalInfo;

    /**
     * Ctor.
     * @param clazz type
     * @param additionalInfo additional information on why and where a serde is need for this type
     */
    public SerdeProviderContextClass(Class clazz, SerdeProviderAdditionalInfo additionalInfo) {
        this.clazz = clazz;
        this.additionalInfo = additionalInfo;
    }

    /**
     * Returns the type to provide a serde for
     * @return type
     */
    public Class getClazz() {
        return clazz;
    }

    /**
     * Returns additional information on why and where a serde is need for this type
     * @return info
     */
    public SerdeProviderAdditionalInfo getAdditionalInfo() {
        return additionalInfo;
    }
}
