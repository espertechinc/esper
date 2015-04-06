/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.support.epl;

public class SupportChainFuncLib {

    public static SupportChainInner getInner(int one, int two) {
        return new SupportChainInner(one, two);
    }

    public static class SupportChainInner
    {
        private int sum;

        public SupportChainInner(int one, int two) {
            sum = one + two;
        }

        public SupportChainInner add(int one, int two) {
            return new SupportChainInner(sum, one + two);
        }

        public int getTotal() {
            return sum;
        }
    }
}
