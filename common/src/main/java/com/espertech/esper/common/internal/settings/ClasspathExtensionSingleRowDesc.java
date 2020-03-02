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
package com.espertech.esper.common.internal.settings;

import com.espertech.esper.common.internal.collection.Pair;

public class ClasspathExtensionSingleRowDesc {
    private final String optionalModuleName;
    private final Class clazz;
    private final ClasspathImportSingleRowDesc desc;

    public ClasspathExtensionSingleRowDesc(String optionalModuleName, Class clazz, ClasspathImportSingleRowDesc desc) {
        this.optionalModuleName = optionalModuleName;
        this.clazz = clazz;
        this.desc = desc;
    }

    public String getOptionalModuleName() {
        return optionalModuleName;
    }

    public Class getClazz() {
        return clazz;
    }

    public ClasspathImportSingleRowDesc getDesc() {
        return desc;
    }

    public Pair<Class, ClasspathImportSingleRowDesc> getAsPair() {
        return new Pair<>(clazz, desc);
    }
}
