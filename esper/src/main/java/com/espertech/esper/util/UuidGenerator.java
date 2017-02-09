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
package com.espertech.esper.util;

import java.util.UUID;

/**
 * Unique id generator based on Java 5 {@link java.util.UUID}, generates 36-character unique ids.
 */
public class UuidGenerator {

    /**
     * Generates a 36-character alphanumeric value with dashes considering secure random id
     * and timestamp backed by {@link java.util.UUID}.
     *
     * @return unique id string
     */
    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
