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
 * Provides an implementation of an empty iterator.
 * <p>
 * This class provides an implementation of an empty iterator.
 * This class provides for binary compatability between Commons Collections
 * 2.1.1 and 3.1 due to issues with <code>IteratorUtils</code>.
 *
 * @author Stephen Colebourne
 * @version $Revision$ $Date$
 * @since Commons Collections 2.1.1 and 3.1
 */
public class EmptyIterator extends AbstractEmptyIterator implements ResettableIterator {

    /**
     * Singleton instance of the iterator.
     *
     * @since Commons Collections 3.1
     */
    public static final ResettableIterator RESETTABLE_INSTANCE = new EmptyIterator();
    /**
     * Singleton instance of the iterator.
     *
     * @since Commons Collections 2.1.1 and 3.1
     */
    public static final Iterator INSTANCE = RESETTABLE_INSTANCE;

    /**
     * Constructor.
     */
    protected EmptyIterator() {
        super();
    }

}