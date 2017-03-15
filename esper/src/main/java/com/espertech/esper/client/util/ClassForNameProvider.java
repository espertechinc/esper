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
package com.espertech.esper.client.util;

/**
 * Provider of lookup of a class name resolving into a class.
 */
public interface ClassForNameProvider {
    /**
     * Name.
     */
    String NAME = "ClassForNameProvider";

    /**
     * Lookup class name returning class.
     * @param className to look up
     * @return class
     * @throws ClassNotFoundException if the class cannot be found
     */
    Class classForName(String className) throws ClassNotFoundException;
}
