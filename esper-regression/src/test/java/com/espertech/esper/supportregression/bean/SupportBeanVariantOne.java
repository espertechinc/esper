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
package com.espertech.esper.supportregression.bean;

import java.io.Serializable;
import java.util.*;

public class SupportBeanVariantOne implements Serializable {
    private ISupportB p0;
    private ISupportAImplSuperG p1;
    private ArrayList p2;
    private List p3;
    private Collection p4;
    private List p5;
    private int[] indexed;
    private Map<String, String> mapped;
    private SupportBeanVariantOneInner inneritem;

    public SupportBeanVariantOne() {
        indexed = new int[]{1, 2, 3};
        mapped = new HashMap<String, String>();
        mapped.put("a", "val1");
        inneritem = new SupportBeanVariantOneInner("i1");
    }

    public ISupportB getP0() {
        return new ISupportABCImpl("a", "b", "baseAB", "c");
    }

    public ISupportAImplSuperG getP1() {
        return new ISupportAImplSuperGImpl("g", "a", "baseAB");
    }

    public ArrayList getP2() {
        return p2;
    }

    public List getP3() {
        return p3;
    }

    public Collection getP4() {
        return p4;
    }

    public List getP5() {
        return p5;
    }

    public int[] getIndexed() {
        return indexed;
    }

    public int getIndexArr(int index) {
        return indexed[index];
    }

    public Map getMapped() {
        return mapped;
    }

    public String getMappedKey(String key) {
        return mapped.get(key);
    }

    public SupportBeanVariantOneInner getInneritem() {
        return inneritem;
    }

    public static class SupportBeanVariantOneInner {
        private String val;

        public SupportBeanVariantOneInner(String val) {
            this.val = val;
        }

        public String getVal() {
            return val;
        }
    }
}
