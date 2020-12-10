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
package com.espertech.esper.common.internal.collection;

import com.espertech.esper.common.client.type.EPTypeClass;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * reference-counting set based on a HashMap implementation that stores keys and a reference counter for
 * each unique key value. Each time the same key is added, the reference counter increases.
 * Each time a key is removed, the reference counter decreases.
 */
public class RefCountedSet<K> {
    public final static EPTypeClass EPTYPE = new EPTypeClass(RefCountedSet.class);

    private Map<K, Integer> refSet;
    private int numValues;

    /**
     * Constructor.
     */
    public RefCountedSet() {
        refSet = new HashMap<K, Integer>();
    }

    public RefCountedSet(Map<K, Integer> refSet, int numValues) {
        this.refSet = refSet;
        this.numValues = numValues;
    }

    /**
     * Clear out the collection.
     */
    public void clear() {
        refSet.clear();
        numValues = 0;
    }

    /**
     * Add a key to the set. Add with a reference count of one if the key didn't exist in the set.
     * Increase the reference count by one if the key already exists.
     * Return true if this is the first time the key was encountered, or false if key is already in set.
     *
     * @param key to add
     * @return true if the key is not in the set already, false if the key is already in the set
     */
    public boolean add(K key) {
        Integer value = refSet.get(key);
        if (value == null) {
            refSet.put(key, 1);
            numValues++;
            return true;
        }

        value++;
        numValues++;
        refSet.put(key, value);
        return false;
    }

    /**
     * Add a key to the set with the given number of references.
     *
     * @param key           to add
     * @param numReferences initial number of references
     */
    public void add(K key, int numReferences) {
        Integer value = refSet.get(key);
        if (value == null) {
            refSet.put(key, numReferences);
            numValues += numReferences;
            return;
        }
        throw new IllegalArgumentException("Key '" + key + "' already in collection");
    }

    /**
     * Removed a key to the set. Removes the key if the reference count is one.
     * Decreases the reference count by one if the reference count is more then one.
     * Return true if the reference count was one and the key thus removed, or false if key is stays in set.
     *
     * @param key to add
     * @return true if the key is removed, false if it stays in the set
     * @throws IllegalStateException is a key is removed that wasn't added to the map
     */
    public boolean remove(K key) {
        Integer value = refSet.get(key);
        if (value == null) {
            return true; // ignore duplcate removals
        }

        if (value == 1) {
            refSet.remove(key);
            numValues--;
            return true;
        }

        value--;
        refSet.put(key, value);
        numValues--;
        return false;
    }

    /**
     * Remove a key from the set regardless of the number of references.
     *
     * @param key to add
     * @return true if the key is removed, false if the key was not found
     * @throws IllegalStateException if a key is removed that wasn't added to the map
     */
    public boolean removeAll(K key) {
        Integer value = refSet.remove(key);
        return value != null;
    }

    /**
     * Returns an iterator over the entry set.
     *
     * @return entry set iterator
     */
    public Iterator<Map.Entry<K, Integer>> entryIterator() {
        return refSet.entrySet().iterator();
    }

    /**
     * Returns a key iterator.
     *
     * @return key iterator
     */
    public Iterator<K> keyIterator() {
        return refSet.keySet().iterator();
    }

    /**
     * Returns the number of values in the collection.
     *
     * @return size
     */
    public int size() {
        return numValues;
    }

    public Map<K, Integer> getRefSet() {
        return refSet;
    }

    public int getNumValues() {
        return numValues;
    }

    public void setNumValues(int numValues) {
        this.numValues = numValues;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param output   output
     * @param valueSet values
     * @throws IOException io error
     */
    public static void writePointsDouble(DataOutput output, RefCountedSet<Double> valueSet) throws IOException {
        Map<Double, Integer> refSet = valueSet.getRefSet();
        output.writeInt(refSet.size());
        output.writeInt(valueSet.getNumValues());
        for (Map.Entry<Double, Integer> entry : valueSet.getRefSet().entrySet()) {
            output.writeDouble(entry.getKey());
            output.writeInt(entry.getValue());
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param input input
     * @return values
     * @throws IOException io error
     */
    public static RefCountedSet<Double> readPointsDouble(DataInput input) throws IOException {
        RefCountedSet<Double> valueSet = new RefCountedSet<>();
        Map<Double, Integer> und = valueSet.getRefSet();
        int size = input.readInt();
        valueSet.setNumValues(input.readInt());
        for (int i = 0; i < size; i++) {
            Double key = input.readDouble();
            Integer val = input.readInt();
            und.put(key, val);
        }
        return valueSet;
    }
}
