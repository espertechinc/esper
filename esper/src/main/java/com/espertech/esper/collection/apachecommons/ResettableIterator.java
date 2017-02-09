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
 * Defines an iterator that can be reset back to an initial state.
 * <p>
 * This interface allows an iterator to be repeatedly reused.
 *
 * @author Stephen Colebourne
 * @version $Revision$ $Date$
 * @since Commons Collections 3.0
 */
public interface ResettableIterator extends Iterator {

    /**
     * Resets the iterator back to the position at which the iterator
     * was created.
     */
    public void reset();

}