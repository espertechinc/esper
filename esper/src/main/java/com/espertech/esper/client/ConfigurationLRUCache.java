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
package com.espertech.esper.client;

import java.io.Serializable;

/**
 * LRU cache settings.
 */
public class ConfigurationLRUCache implements ConfigurationDataCache, Serializable {
    private int size;
    private static final long serialVersionUID = 411347352942362467L;

    /**
     * Ctor.
     *
     * @param size is the maximum cache size
     */
    public ConfigurationLRUCache(int size) {
        this.size = size;
    }

    /**
     * Returns the maximum cache size.
     *
     * @return max cache size
     */
    public int getSize() {
        return size;
    }

    public String toString() {
        return "LRUCacheDesc size=" + size;
    }
}
