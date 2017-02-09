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
 * Marker interface for use with view factories that create data window views
 * that are asymetric in posting insert and remove stream data:
 * Data windows that post only a partial insert and remove stream as output when compared to
 * the insert and remove stream received.
 * <p>
 * Please {@link com.espertech.esper.view.DataWindowView} for details on views that meet data window requirements.
 */
public interface AsymetricDataWindowViewFactory extends DataWindowViewFactory {

}