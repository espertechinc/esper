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

import java.util.Iterator;

/**
 * Defines an iterator that operates over a <code>Map</code>.
 * <p>
 * This iterator is a special version designed for maps. It can be more
 * efficient to use this rather than an entry set iterator where the option
 * is available, and it is certainly more convenient.
 * <p>
 * A map that provides this interface may not hold the data internally using
 * Map Entry objects, thus this interface can avoid lots of object creation.
 * <p>
 * In use, this iterator iterates through the keys in the map. After each call
 * to <code>next()</code>, the <code>getValue()</code> method provides direct
 * access to the value. The value can also be set using <code>setValue()</code>.
 * <pre>
 * MapIterator it = map.mapIterator();
 * while (it.hasNext()) {
 *   Object key = it.next();
 *   Object value = it.getValue();
 *   it.setValue(newValue);
 * }
 * </pre>
 *
 * @author Stephen Colebourne
 * @version $Revision$ $Date$
 * @since Commons Collections 3.0
 */
public interface MapIterator extends Iterator {

    /**
     * Checks to see if there are more entries still to be iterated.
     *
     * @return <code>true</code> if the iterator has more elements
     */
    boolean hasNext();

    /**
     * Gets the next <em>key</em> from the <code>Map</code>.
     *
     * @return the next key in the iteration
     * @throws java.util.NoSuchElementException if the iteration is finished
     */
    Object next();

    //-----------------------------------------------------------------------

    /**
     * Gets the current key, which is the key returned by the last call
     * to <code>next()</code>.
     *
     * @return the current key
     * @throws IllegalStateException if <code>next()</code> has not yet been called
     */
    Object getKey();

    /**
     * Gets the current value, which is the value associated with the last key
     * returned by <code>next()</code>.
     *
     * @return the current value
     * @throws IllegalStateException if <code>next()</code> has not yet been called
     */
    Object getValue();

    //-----------------------------------------------------------------------

    /**
     * Removes the last returned key from the underlying <code>Map</code> (optional operation).
     * <p>
     * This method can be called once per call to <code>next()</code>.
     *
     * @throws UnsupportedOperationException if remove is not supported by the map
     * @throws IllegalStateException         if <code>next()</code> has not yet been called
     *                                       or if <code>remove()</code> has already been called
     *                                       since the last call to <code>next()</code>
     */
    void remove();

    /**
     * Sets the value associated with the current key (optional operation).
     *
     * @param value the new value
     * @return the previous value
     * @throws UnsupportedOperationException if setValue is not supported by the map
     * @throws IllegalStateException         if <code>next()</code> has not yet been called
     *                                       or if <code>remove()</code> has been called since the
     *                                       last call to <code>next()</code>
     */
    Object setValue(Object value);

}