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
package com.espertech.esper.example.transaction.sim;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Just so we can swap between Random and SecureRandom.
 *
 * @author Hans Gilde
 */
public class RandomUtil {
    public static Random getNewInstance() {
        return new SecureRandom();
    }
}
