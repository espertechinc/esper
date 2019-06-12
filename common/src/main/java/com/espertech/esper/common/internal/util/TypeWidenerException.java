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
package com.espertech.esper.common.internal.util;

public class TypeWidenerException extends Exception {
    private static final long serialVersionUID = -4201286185409234550L;

    public TypeWidenerException(String message) {
        super(message);
    }

    public TypeWidenerException(String message, Throwable cause) {
        super(message, cause);
    }
}
