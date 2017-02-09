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
 * Tag interface for derived-value views. Derived-value views follow the view interface and do not keep a window over the
 * data received by their parent view. They simply derive a set of data points from a stream and
 * do not retain events.
 * <p>
 * Derived-Value views generally follow the following behavior:
 * <p>
 * They publish the output data when receiving insert or remove stream data from their parent view,
 * directly and not time-driven.
 * <p>
 * They typically change event type compared to their parent view, since they derive new information
 * or add information to events.
 */
public interface DerivedValueView extends View, GroupableView {
}
