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

public class DataInputOutputSerdeForgeSingletonMKArray extends DataInputOutputSerdeForgeSingleton {

    private final String simpleNameComponent;

    public DataInputOutputSerdeForgeSingletonMKArray(Class serdeClass, String simpleNameComponent) {
        super(serdeClass);
        this.simpleNameComponent = simpleNameComponent;
    }

    public String getSimpleNameComponent() {
        return simpleNameComponent;
    }
}
