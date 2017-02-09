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
package com.espertech.esper.view.stream;

/**
 * Static factory for implementations of the StreamFactoryService interface.
 */
public final class StreamFactoryServiceProvider {
    /**
     * Creates an implementation of the StreamFactoryService interface.
     *
     * @param isReuseViews indicator on whether stream and view resources are to be reused between statements
     * @param engineURI    engine URI
     * @return implementation
     */
    public static StreamFactoryService newService(String engineURI, boolean isReuseViews) {
        return new StreamFactorySvcImpl(engineURI, isReuseViews);
    }
}
