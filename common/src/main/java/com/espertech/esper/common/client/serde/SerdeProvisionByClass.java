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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForgeEmptyCtor;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForgeSingleton;
import com.espertech.esper.common.internal.util.MethodResolver;
import com.espertech.esper.common.internal.util.MethodResolverNoSuchCtorException;

/**
 * For use with high-availability and scale-out only, this class instructs the compiler that the serializer and de-serializer (serde)
 * is available via a singleton-pattern-style static field named "INSTANCE" (preferred) or by has a default constructor.
 */
public class SerdeProvisionByClass extends SerdeProvision {
    private final Class serdeClass;

    /**
     * Class of the serde.
     * @param serdeClass serde class
     */
    public SerdeProvisionByClass(Class serdeClass) {
        this.serdeClass = serdeClass;
    }

    /**
     * Returns the class of the serde
     * @return serde class
     */
    public Class getSerdeClass() {
        return serdeClass;
    }

    public DataInputOutputSerdeForge toForge() {
        try {
            serdeClass.getField("INSTANCE");
            return new DataInputOutputSerdeForgeSingleton(serdeClass);
        } catch (NoSuchFieldException e) {
        }

        try {
            MethodResolver.resolveCtor(serdeClass, new Class[0]);
            return new DataInputOutputSerdeForgeEmptyCtor(serdeClass);
        } catch (MethodResolverNoSuchCtorException ex) {
        }

        throw new EPException("Serde class '" + serdeClass.getName() + "' does not have a singleton-style INSTANCE field or default constructor");
    }
}
