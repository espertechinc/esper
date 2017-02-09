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
 * Provides an implementation of an empty map iterator.
 *
 * @author Stephen Colebourne
 * @version $Revision$ $Date$
 * @since Commons Collections 3.1
 */
public class EmptyMapIterator extends AbstractEmptyIterator implements MapIterator, ResettableIterator {

    /**
     * Singleton instance of the iterator.
     *
     * @since Commons Collections 3.1
     */
    public static final MapIterator INSTANCE = new EmptyMapIterator();

    /**
     * Constructor.
     */
    protected EmptyMapIterator() {
        super();
    }

}