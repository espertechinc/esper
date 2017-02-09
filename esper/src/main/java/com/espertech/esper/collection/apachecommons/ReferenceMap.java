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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * A <code>Map</code> implementation that allows mappings to be
 * removed by the garbage collector.
 * <p>
 * When you construct a <code>ReferenceMap</code>, you can specify what kind
 * of references are used to store the map's keys and values.
 * If non-hard references are used, then the garbage collector can remove
 * mappings if a key or value becomes unreachable, or if the JVM's memory is
 * running low. For information on how the different reference types behave,
 * see {@link java.lang.ref.Reference Reference}.
 * <p>
 * Different types of references can be specified for keys and values.
 * The keys can be configured to be weak but the values hard,
 * in which case this class will behave like a
 * <a href="http://java.sun.com/j2se/1.4/docs/api/java/util/WeakHashMap.html">
 * <code>WeakHashMap</code></a>. However, you can also specify hard keys and
 * weak values, or any other combination. The default constructor uses
 * hard keys and soft values, providing a memory-sensitive cache.
 * <p>
 * This map is similar to
 * ReferenceIdentityMap.
 * It differs in that keys and values in this class are compared using <code>equals()</code>.
 * <p>
 * This {@link java.util.Map Map} implementation does <i>not</i> allow null elements.
 * Attempting to add a null key or value to the map will raise a <code>NullPointerException</code>.
 * <p>
 * This implementation is not synchronized.
 * You can use {@link java.util.Collections#synchronizedMap} to
 * provide synchronized access to a <code>ReferenceMap</code>.
 * Remember that synchronization will not stop the garbage collecter removing entries.
 * <p>
 * All the available iterators can be reset back to the start by casting to
 * <code>ResettableIterator</code> and calling <code>reset()</code>.
 * <p>
 * <strong>Note that ReferenceMap is not synchronized and is not thread-safe.</strong>
 * If you wish to use this map from multiple threads concurrently, you must use
 * appropriate synchronization. The simplest approach is to wrap this map
 * using {@link java.util.Collections#synchronizedMap}. This class may throw
 * exceptions when accessed by concurrent threads without synchronization.
 * <p>
 * NOTE: As from Commons Collections 3.1 this map extends <code>AbstractReferenceMap</code>
 * (previously it extended AbstractMap). As a result, the implementation is now
 * extensible and provides a <code>MapIterator</code>.
 *
 * @author Paul Jack
 * @author Stephen Colebourne
 * @version $Revision$ $Date$
 * @see java.lang.ref.Reference
 * @since Commons Collections 3.0 (previously in main package v2.1)
 */
public class ReferenceMap extends AbstractReferenceMap implements Serializable {

    /**
     * Serialization version
     */
    private static final long serialVersionUID = 1555089888138299607L;

    /**
     * Constructs a new <code>ReferenceMap</code> that will
     * use hard references to keys and soft references to values.
     */
    public ReferenceMap() {
        super(HARD, SOFT, DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, false);
    }

    /**
     * Constructs a new <code>ReferenceMap</code> that will
     * use the specified types of references.
     *
     * @param keyType   the type of reference to use for keys;
     *                  must be {@link #HARD}, {@link #SOFT}, {@link #WEAK}
     * @param valueType the type of reference to use for values;
     *                  must be {@link #HARD}, {@link #SOFT}, {@link #WEAK}
     */
    public ReferenceMap(int keyType, int valueType) {
        super(keyType, valueType, DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, false);
    }

    /**
     * Constructs a new <code>ReferenceMap</code> that will
     * use the specified types of references.
     *
     * @param keyType     the type of reference to use for keys;
     *                    must be {@link #HARD}, {@link #SOFT}, {@link #WEAK}
     * @param valueType   the type of reference to use for values;
     *                    must be {@link #HARD}, {@link #SOFT}, {@link #WEAK}
     * @param purgeValues should the value be automatically purged when the
     *                    key is garbage collected
     */
    public ReferenceMap(int keyType, int valueType, boolean purgeValues) {
        super(keyType, valueType, DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, purgeValues);
    }

    /**
     * Constructs a new <code>ReferenceMap</code> with the
     * specified reference types, load factor and initial
     * capacity.
     *
     * @param keyType    the type of reference to use for keys;
     *                   must be {@link #HARD}, {@link #SOFT}, {@link #WEAK}
     * @param valueType  the type of reference to use for values;
     *                   must be {@link #HARD}, {@link #SOFT}, {@link #WEAK}
     * @param capacity   the initial capacity for the map
     * @param loadFactor the load factor for the map
     */
    public ReferenceMap(int keyType, int valueType, int capacity, float loadFactor) {
        super(keyType, valueType, capacity, loadFactor, false);
    }

    /**
     * Constructs a new <code>ReferenceMap</code> with the
     * specified reference types, load factor and initial
     * capacity.
     *
     * @param keyType     the type of reference to use for keys;
     *                    must be {@link #HARD}, {@link #SOFT}, {@link #WEAK}
     * @param valueType   the type of reference to use for values;
     *                    must be {@link #HARD}, {@link #SOFT}, {@link #WEAK}
     * @param capacity    the initial capacity for the map
     * @param loadFactor  the load factor for the map
     * @param purgeValues should the value be automatically purged when the
     *                    key is garbage collected
     */
    public ReferenceMap(int keyType, int valueType, int capacity,
                        float loadFactor, boolean purgeValues) {
        super(keyType, valueType, capacity, loadFactor, purgeValues);
    }

    //-----------------------------------------------------------------------

    /**
     * Write the map out using a custom routine.
     *
     * @param outputStream out stream
     * @throws IOException io error
     */
    private void writeObject(ObjectOutputStream outputStream) throws IOException {
        outputStream.defaultWriteObject();
        doWriteObject(outputStream);
    }

    /**
     * Read the map in using a custom routine.
     *
     * @param input in stream
     * @throws IOException            io error
     * @throws ClassNotFoundException class not found
     */
    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        doReadObject(input);
    }

}