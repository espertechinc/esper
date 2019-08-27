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
package com.espertech.esper.common.client.hook.enummethod;

/**
 * Enumeration method extension API for adding enum-methods.
 */
public interface EnumMethodForgeFactory {
    /**
     * Called by the compiler to receive the list of footprints.
     *
     * @param context contextual information
     * @return footprints
     */
    EnumMethodDescriptor initialize(EnumMethodInitializeContext context);

    /**
     * Called by the compiler to allow validation of actual parameters beyond validation of the footprint information
     * that the compiler does automatically.
     * <p>
     * Can be used to pre-evaluate parameter expressions.
     * </p>
     *
     * @param context contextual information
     * @return enumeration method descriptor
     */
    EnumMethodMode validate(EnumMethodValidateContext context);
}
