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
package com.espertech.esper.client.annotation;

/**
 * Annotation for use in EPL statements to suppress any statement-level locking (use with caution, see below).
 * <p>
 * Caution: We provide this annotation for the purpose of identifing locking overhead,
 * or when your application is single-threaded, or when using an external mechanism for concurreny control
 * or for example with virtual data windows or plug-in data windows to allow customizing concurrency
 * for application-provided data windows.
 * Using this annotation may have unpredictable results unless your application is taking concurrency under consideration.
 * </p>
 */
public @interface NoLock {
}
