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

public class SupportBeanVariantTwo implements Serializable {
    private ISupportBaseAB p0;
    private ISupportAImplSuperGImplPlus p1;
    private LinkedList p2;
    private List p3;
    private List p4;
    private Collection p5;
    private int[] indexed;
    private Map<String, String> mapped;
    private SupportBeanVariantOne.SupportBeanVariantOneInner inneritem;

    public SupportBeanVariantTwo() {
        indexed = new int[]{10, 20, 30};
        mapped = new HashMap<String, String>();
        mapped.put("a", "val2");
        inneritem = new SupportBeanVariantOne.SupportBeanVariantOneInner("i2");
    }

    public ISupportBaseAB getP0() {
        return p0;
    }

    public ISupportAImplSuperGImplPlus getP1() {
        return p1;
    }

    public LinkedList getP2() {
        return p2;
    }

    public List getP3() {
        return p3;
    }

    public List getP4() {
        return p4;
    }

    public Collection getP5() {
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

    public SupportBeanVariantOne.SupportBeanVariantOneInner getInneritem() {
        return inneritem;
    }
}
