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

import java.math.BigDecimal;
import java.util.*;

public class SupportCollection {
    private static String sampleStaticCSV;

    private Collection<String> strvals;
    private Collection<String> strvalstwo;
    private Collection<Integer> intvals;
    private Collection<BigDecimal> bdvals;
    private Collection<Boolean> boolvals;
    private int[] intarray;
    private Iterable<Integer> intiterable;

    public static SupportCollection makeString(String csvlist) {
        SupportCollection bean = new SupportCollection();
        bean.strvals = toListString(csvlist);
        bean.strvalstwo = toListString(csvlist);
        return bean;
    }

    public static SupportCollection makeString(String csvlist, String csvlisttwo) {
        SupportCollection bean = new SupportCollection();
        bean.strvals = toListString(csvlist);
        bean.strvalstwo = toListString(csvlisttwo);
        return bean;
    }

    public static SupportCollection makeNumeric(String csvlist) {
        SupportCollection bean = new SupportCollection();
        Collection<String> list = toListString(csvlist);
        bean.intvals = toInt(list);
        bean.bdvals = toBigDecimal(list);

        if (bean.intvals != null) {
            bean.intarray = new int[bean.intvals.size()];
            int count = 0;
            for (Integer val : bean.intvals) {
                bean.intarray[count++] = val == null ? Integer.MIN_VALUE : val;
            }

            final Collection<Integer> iteratable = bean.intvals;
            bean.intiterable = new Iterable<Integer>() {
                public Iterator<Integer> iterator() {
                    return iteratable.iterator();
                }
            };
        }

        return bean;
    }

    public static SupportCollection makeBoolean(String csvlist) {
        SupportCollection bean = new SupportCollection();
        Collection<String> list = toListString(csvlist);
        bean.boolvals = toBoolean(list);
        return bean;
    }

    public Collection<String> getStrvals() {
        return strvals;
    }

    public Collection<String> getStrvalstwo() {
        return strvalstwo;
    }

    public Collection<Integer> getIntvals() {
        return intvals;
    }

    public Collection<BigDecimal> getBdvals() {
        return bdvals;
    }

    public Collection<Boolean> getBoolvals() {
        return boolvals;
    }

    public int[] getIntarray() {
        return intarray;
    }

    public Iterable<Integer> getIntiterable() {
        return intiterable;
    }

    private static List<String> toListString(String csvlist) {
        if (csvlist == null) {
            return null;
        } else if (csvlist.isEmpty()) {
            return Collections.emptyList();
        } else {
            String[] items = csvlist.split(",");
            List<String> list = new ArrayList<String>();
            for (int i = 0; i < items.length; i++) {
                if (items[i].equals("null")) {
                    list.add(null);
                } else {
                    list.add(items[i]);
                }
            }
            return list;
        }
    }

    private static Collection<BigDecimal> toBigDecimal(Collection<String> one) {
        if (one == null) {
            return null;
        }
        List<BigDecimal> result = new ArrayList<BigDecimal>();
        for (String element : one) {
            if (element == null) {
                result.add(null);
                continue;
            }
            result.add(new BigDecimal(Double.parseDouble(element)));
        }
        return result;
    }

    private static Collection<Integer> toInt(Collection<String> one) {
        if (one == null) {
            return null;
        }
        List<Integer> result = new ArrayList<Integer>();
        for (String element : one) {
            if (element == null) {
                result.add(null);
                continue;
            }
            result.add(Integer.parseInt(element));
        }
        return result;
    }

    private static Collection<Boolean> toBoolean(Collection<String> one) {
        if (one == null) {
            return null;
        }
        List<Boolean> result = new ArrayList<Boolean>();
        for (String element : one) {
            if (element == null) {
                result.add(null);
                continue;
            }
            result.add(Boolean.parseBoolean(element));
        }
        return result;
    }

    public static void setSampleCSV(String input) {
        sampleStaticCSV = input;
    }

    public static List<String> makeSampleListString() {
        return toListString(sampleStaticCSV);
    }

    public static String[] makeSampleArrayString() {
        List<String> list = toListString(sampleStaticCSV);
        if (list == null) {
            return null;
        }
        return list.toArray(new String[list.size()]);
    }

    public static void setSampleStaticCSV(String sampleStaticCSV) {
        SupportCollection.sampleStaticCSV = sampleStaticCSV;
    }

    public void setStrvals(Collection<String> strvals) {
        this.strvals = strvals;
    }

    public void setStrvalstwo(Collection<String> strvalstwo) {
        this.strvalstwo = strvalstwo;
    }

    public void setIntvals(Collection<Integer> intvals) {
        this.intvals = intvals;
    }

    public void setBdvals(Collection<BigDecimal> bdvals) {
        this.bdvals = bdvals;
    }

    public void setBoolvals(Collection<Boolean> boolvals) {
        this.boolvals = boolvals;
    }

    public void setIntarray(int[] intarray) {
        this.intarray = intarray;
    }

    public void setIntiterable(Iterable<Integer> intiterable) {
        this.intiterable = intiterable;
    }
}
