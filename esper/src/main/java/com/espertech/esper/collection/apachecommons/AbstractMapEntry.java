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

import java.util.Map;

/**
 * Abstract Pair class to assist with creating correct
 * {@link java.util.Map.Entry Map.Entry} implementations.
 *
 * @author James Strachan
 * @author Michael A. Smith
 * @author Neil O'Toole
 * @author Stephen Colebourne
 * @version $Revision$ $Date$
 * @since Commons Collections 3.0
 */
public abstract class AbstractMapEntry extends AbstractKeyValue implements Map.Entry {

    /**
     * Constructs a new entry with the given key and given value.
     *
     * @param key   the key for the entry, may be null
     * @param value the value for the entry, may be null
     */
    protected AbstractMapEntry(Object key, Object value) {
        super(key, value);
    }

    // Map.Entry interface
    //-------------------------------------------------------------------------

    /**
     * Sets the value stored in this <code>Map.Entry</code>.
     * <p>
     * This <code>Map.Entry</code> is not connected to a Map, so only the
     * local data is changed.
     *
     * @param value the new value
     * @return the previous value
     */
    public Object setValue(Object value) {
        Object answer = this.value;
        this.value = value;
        return answer;
    }

    /**
     * Compares this <code>Map.Entry</code> with another <code>Map.Entry</code>.
     * <p>
     * Implemented per API documentation of {@link java.util.Map.Entry#equals(Object)}
     *
     * @param obj the object to compare to
     * @return true if equal key and value
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Map.Entry)) {
            return false;
        }
        Map.Entry other = (Map.Entry) obj;
        return
                (getKey() == null ? other.getKey() == null : getKey().equals(other.getKey())) &&
                        (getValue() == null ? other.getValue() == null : getValue().equals(other.getValue()));
    }

    /**
     * Gets a hashCode compatible with the equals method.
     * <p>
     * Implemented per API documentation of {@link java.util.Map.Entry#hashCode()}
     *
     * @return a suitable hash code
     */
    public int hashCode() {
        return (getKey() == null ? 0 : getKey().hashCode()) ^
                (getValue() == null ? 0 : getValue().hashCode());
    }

}