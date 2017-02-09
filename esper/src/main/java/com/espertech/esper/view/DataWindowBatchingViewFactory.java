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
package com.espertech.esper.view;

/**
 * Tag interface for data window view factories that express a batch expiry policy.
 * <p>
 * Such data windows allow iteration through the currently batched events,
 * and such data windows post insert stream events only when batching conditions have been met and
 * the batch is released.
 */
public interface DataWindowBatchingViewFactory {
}
