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

public class EPServiceProviderName {
    /**
     * For the default provider instance, which carries a null provider URI,
     * the URI value is "default".
     */
    public static final String DEFAULT_ENGINE_URI = "default";

    /**
     * For the default provider instance, which carries a "default" provider URI,
     * the property name qualification and stream name qualification may use "default".
     */
    public static final String DEFAULT_ENGINE_URI_QUALIFIER = "default";

}
