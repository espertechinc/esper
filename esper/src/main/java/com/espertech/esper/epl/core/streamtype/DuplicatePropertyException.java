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
package com.espertech.esper.epl.core.streamtype;

/**
 * Indicates a property exists in multiple streams.
 */
public class DuplicatePropertyException extends StreamTypesException {
    private static final long serialVersionUID = -4239595353787781082L;

    /**
     * Ctor.
     *
     * @param msg - exception message
     */
    public DuplicatePropertyException(String msg) {
        super(msg, null);
    }
}
