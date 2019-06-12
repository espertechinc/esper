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

public class DataInputOutputSerdeException extends RuntimeException {

    private static final long serialVersionUID = -6976465921234144033L;

    public DataInputOutputSerdeException(String message) {
        super(message);
    }

    public DataInputOutputSerdeException(String message, Throwable cause) {
        super(message, cause);
    }
}
