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
package com.espertech.esper.common.client.dataflow.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for use with data flow operator forges to provide output type information
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface OutputType {
    /**
     * Returns the name
     *
     * @return name
     */
    public String name();

    /**
     * Returns the type
     *
     * @return type
     */
    public Class type() default OutputType.class;

    /**
     * Returns the type name
     *
     * @return type name
     */
    public String typeName() default "";
}
