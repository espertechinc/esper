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
package com.espertech.esper.client;

/**
 * This exception is thrown to indicate that a subscriber registration failed
 * such as when the subscribe does not expose an acceptable method to receive statement results.
 */
public class EPSubscriberException extends EPException {
    private static final long serialVersionUID = -2217801048068728940L;

    /**
     * Ctor.
     *
     * @param message - error message
     */
    public EPSubscriberException(final String message) {
        super(message);
    }
}
