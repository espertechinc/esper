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
package com.espertech.esper.client.context;

import com.espertech.esper.client.EPException;

/**
 * Indicates an invalid combination of context declaration and context partition selector, i.e. cageory context with hash context partition selector.
 */
public class InvalidContextPartitionSelector extends EPException {
    private static final long serialVersionUID = -1903001646255533462L;

    /**
     * Ctor.
     *
     * @param message exception message
     */
    public InvalidContextPartitionSelector(String message) {
        super(message);
    }

    /**
     * Ctor.
     *
     * @param message exception message
     * @param cause   inner exception
     */
    public InvalidContextPartitionSelector(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Ctor.
     *
     * @param cause inner exception
     */
    public InvalidContextPartitionSelector(Throwable cause) {
        super(cause);
    }
}
