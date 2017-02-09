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
 * Abstract pair class to assist with creating <code>KeyValue</code>
 * and {@link java.util.Map.Entry Map.Entry} implementations.
 *
 * @author James Strachan
 * @author Michael A. Smith
 * @author Neil O'Toole
 * @author Stephen Colebourne
 * @version $Revision$ $Date$
 * @since Commons Collections 3.0
 */
public abstract class AbstractKeyValue implements KeyValue {

    /**
     * The key
     */
    protected Object key;
    /**
     * The value
     */
    protected Object value;

    /**
     * Constructs a new pair with the specified key and given value.
     *
     * @param key   the key for the entry, may be null
     * @param value the value for the entry, may be null
     */
    protected AbstractKeyValue(Object key, Object value) {
        super();
        this.key = key;
        this.value = value;
    }

    /**
     * Gets the key from the pair.
     *
     * @return the key
     */
    public Object getKey() {
        return key;
    }

    /**
     * Gets the value from the pair.
     *
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Gets a debugging String view of the pair.
     *
     * @return a String view of the entry
     */
    public String toString() {
        return new StringBuffer()
                .append(getKey())
                .append('=')
                .append(getValue())
                .toString();
    }

}