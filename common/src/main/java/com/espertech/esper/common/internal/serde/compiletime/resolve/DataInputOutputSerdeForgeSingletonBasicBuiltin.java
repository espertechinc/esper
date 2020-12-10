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

import com.espertech.esper.common.client.type.EPTypeClass;

public class DataInputOutputSerdeForgeSingletonBasicBuiltin extends DataInputOutputSerdeForgeSingleton {
    private final static EPTypeClass EPTYPE = new EPTypeClass(DataInputOutputSerdeForgeSingletonBasicBuiltin.class);

    private final EPTypeClass basicBuiltin;

    public DataInputOutputSerdeForgeSingletonBasicBuiltin(Class serdeClass, EPTypeClass basicBuiltin) {
        super(serdeClass);
        this.basicBuiltin = basicBuiltin;
    }

    public EPTypeClass getBasicBuiltin() {
        return basicBuiltin;
    }

    public String toString() {
        return "DataInputOutputSerdeForgeSingletonBasicBuiltin{" +
            "basicBuiltin=" + basicBuiltin +
            '}';
    }
}
