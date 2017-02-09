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
 * Defines a map that can be iterated directly without needing to create an entry set.
 * <p>
 * A map iterator is an efficient way of iterating over maps.
 * There is no need to access the entry set or cast to Map Entry objects.
 * <pre>
 * IterableMap map = new HashedMap();
 * MapIterator it = map.mapIterator();
 * while (it.hasNext()) {
 *   Object key = it.next();
 *   Object value = it.getValue();
 *   it.setValue("newValue");
 * }
 * </pre>
 *
 * @author Stephen Colebourne
 * @version $Revision$ $Date$
 * @since Commons Collections 3.0
 */
public interface IterableMap extends Map {

    /**
     * Obtains a <code>MapIterator</code> over the map.
     * <p>
     * A map iterator is an efficient way of iterating over maps.
     * There is no need to access the entry set or cast to Map Entry objects.
     * <pre>
     * IterableMap map = new HashedMap();
     * MapIterator it = map.mapIterator();
     * while (it.hasNext()) {
     *   Object key = it.next();
     *   Object value = it.getValue();
     *   it.setValue("newValue");
     * }
     * </pre>
     *
     * @return a map iterator
     */
    MapIterator mapIterator();

}