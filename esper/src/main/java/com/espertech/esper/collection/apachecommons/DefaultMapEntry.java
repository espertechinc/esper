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
 * A restricted implementation of {@link java.util.Map.Entry} that prevents
 * the <code>Map.Entry</code> contract from being broken.
 *
 * @author James Strachan
 * @author Michael A. Smith
 * @author Neil O'Toole
 * @author Stephen Colebourne
 * @version $Revision$ $Date$
 * @since Commons Collections 3.0
 */
public final class DefaultMapEntry extends AbstractMapEntry {

    /**
     * Constructs a new entry with the specified key and given value.
     *
     * @param key   the key for the entry, may be null
     * @param value the value for the entry, may be null
     */
    public DefaultMapEntry(final Object key, final Object value) {
        super(key, value);
    }

    /**
     * Constructs a new entry from the specified <code>KeyValue</code>.
     *
     * @param pair the pair to copy, must not be null
     * @throws NullPointerException if the entry is null
     */
    public DefaultMapEntry(final KeyValue pair) {
        super(pair.getKey(), pair.getValue());
    }

    /**
     * Constructs a new entry from the specified <code>Map.Entry</code>.
     *
     * @param entry the entry to copy, must not be null
     * @throws NullPointerException if the entry is null
     */
    public DefaultMapEntry(final Map.Entry entry) {
        super(entry.getKey(), entry.getValue());
    }

}