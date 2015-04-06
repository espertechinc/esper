/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esperio.representation.axiom;

import com.espertech.esper.client.EventPropertyGetter;

/**
 * Interface for property getters also returning type information for the property.
 * @author pablo
 */
public interface TypedEventPropertyGetter extends EventPropertyGetter
{

	/**
	 * Returns type of event property.
	 * @return class of the objects returned by this getter
	 */
	public Class getResultClass();
}
