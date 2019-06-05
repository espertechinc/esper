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
package com.espertech.esper.regressionlib.support.client;

import com.espertech.esper.common.client.annotation.BusEventType;
import com.espertech.esper.common.client.annotation.Public;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Public()
@BusEventType
@Retention(RetentionPolicy.RUNTIME)
public @interface MyAnnotationAPIEventType {
}