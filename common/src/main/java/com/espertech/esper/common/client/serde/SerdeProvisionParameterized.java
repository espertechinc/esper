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

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForgeParameterized;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForgeParameterizedVars;

import java.util.function.Function;

/**
 * For use with high-availability and scale-out only, this class instructs the compiler that the serializer and de-serializer (serde)
 * is available using a parameterized constructor that accepts expressions as represents by the functions provided.
 */
public class SerdeProvisionParameterized extends SerdeProvision {
    private final Class serdeClass;
    private final Function<DataInputOutputSerdeForgeParameterizedVars, CodegenExpression>[] functions;

    /**
     * Ctor
     * @param serdeClass serde class
     * @param functions parameter expressions
     */
    public SerdeProvisionParameterized(Class serdeClass, Function<DataInputOutputSerdeForgeParameterizedVars, CodegenExpression>... functions) {
        this.serdeClass = serdeClass;
        this.functions = functions;
    }

    public DataInputOutputSerdeForge toForge() {
        return new DataInputOutputSerdeForgeParameterized(serdeClass.getName(), functions);
    }
}
