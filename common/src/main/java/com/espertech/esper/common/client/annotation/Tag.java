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
package com.espertech.esper.common.client.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation for use in EPL statement to tag a statement with a name-value pair.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Tag {
    /**
     * Returns the tag name.
     *
     * @return tag name.
     */
    public String name();

    /**
     * Returns the tag value.
     *
     * @return tag value.
     */
    public String value();
}
