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
package com.espertech.esper.client.hook;

import com.espertech.esper.util.TypeWidener;

/**
 * For Avro use widener or transformer of object values to Avro record values
 */
public interface ObjectValueTypeWidenerFactory {
    /**
     * Returns a type widener or coercer.
     * <p>
     * Implementations can provide custom widening behavior from an object to a a widened, coerced or related object value.
     * </p>
     * <p>
     * Implementations should check whether an object value is assignable with or without coercion or widening.
     * </p>
     * <p>
     * This method can return null to use the default widening behavior.
     * </p>
     * <p>
     * Throw {@link UnsupportedOperationException} to indicate an unsupported widening or coercion(default behavior checking still applies if no exception is thrown)
     * </p>
     *
     * @param context context
     * @return coercer/widener
     * @throws UnsupportedOperationException to indicate an unsupported assignment (where not already covered by the default checking)
     */
    TypeWidener make(ObjectValueTypeWidenerFactoryContext context);
}
