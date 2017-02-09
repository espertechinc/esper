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
/*
 *  Credit: Apache Commons Collections
 */
package com.espertech.esper.collection.apachecommons;

/**
 * Defines a simple key value pair.
 * <p>
 * A Map Entry has considerable additional semantics over and above a simple
 * key-value pair. This interface defines the minimum key value, with just the
 * two get methods.
 *
 * @author Stephen Colebourne
 * @version $Revision$ $Date$
 * @since Commons Collections 3.0
 */
public interface KeyValue {

    /**
     * Gets the key from the pair.
     *
     * @return the key
     */
    Object getKey();

    /**
     * Gets the value from the pair.
     *
     * @return the value
     */
    Object getValue();

}
