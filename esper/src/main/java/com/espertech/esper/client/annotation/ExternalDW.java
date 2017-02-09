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
package com.espertech.esper.client.annotation;

/**
 * Annotation for defining an external data window name and open/close functon.
 */
public @interface ExternalDW {
    /**
     * Name
     *
     * @return name
     */
    String name();

    /**
     * Open function.
     *
     * @return open function.
     */
    String functionOpen() default "";

    /**
     * Close function.
     *
     * @return close function
     */
    String functionClose() default "";

    /**
     * Indicator whether unique-key semantics should apply.
     * <p>
     * This indicator is false by default meaning that the implementation should not assume unique-data-window semantics,
     * and would not need to post the previous value of the key as a remove stream event.
     * </p>
     * <p>
     * Setting this indicator is interpreted by an implementation to assume unique-data-window semantics,
     * thereby instructing to post the previous value for the currently-updated key as a remove stream event.
     * </p>
     *
     * @return unique-key semantics
     */
    boolean unique() default false;
}
