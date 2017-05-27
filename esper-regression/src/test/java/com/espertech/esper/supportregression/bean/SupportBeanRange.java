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

public class SupportBeanRange implements Serializable {
    private String id;
    private String key;
    private Integer rangeStart;
    private Integer rangeEnd;
    private String rangeStartStr;
    private String rangeEndStr;
    private Long rangeStartLong;
    private Long rangeEndLong;
    private Long keyLong;

    public SupportBeanRange() {
    }

    public SupportBeanRange(Long keyLong) {
        this.keyLong = keyLong;
    }

    public Long getKeyLong() {
        return keyLong;
    }

    public void setKeyLong(Long keyLong) {
        this.keyLong = keyLong;
    }

    public static SupportBeanRange makeKeyLong(String id, Long keyLong, int rangeStart, int rangeEnd) {
        SupportBeanRange sbr = new SupportBeanRange(id, rangeStart, rangeEnd);
        sbr.setKeyLong(keyLong);
        return sbr;
    }

    public static SupportBeanRange makeLong(String id, String key, Long rangeStartLong, Long rangeEndLong) {
        SupportBeanRange bean = new SupportBeanRange();
        bean.id = id;
        bean.key = key;
        bean.rangeStartLong = rangeStartLong;
        bean.rangeEndLong = rangeEndLong;
        return bean;
    }

    public SupportBeanRange(String id, Integer rangeStart, Integer rangeEnd) {
        this.id = id;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }

    public SupportBeanRange(String id, String key, String rangeStartStr, String rangeEndStr) {
        this.id = id;
        this.key = key;
        this.rangeStartStr = rangeStartStr;
        this.rangeEndStr = rangeEndStr;
    }

    public SupportBeanRange(String id, String key, Integer rangeStart, Integer rangeEnd) {
        this.id = id;
        this.key = key;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }

    public Long getRangeStartLong() {
        return rangeStartLong;
    }

    public Long getRangeEndLong() {
        return rangeEndLong;
    }

    public String getKey() {
        return key;
    }

    public String getId() {
        return id;
    }

    public Integer getRangeStart() {
        return rangeStart;
    }

    public Integer getRangeEnd() {
        return rangeEnd;
    }

    public String getRangeStartStr() {
        return rangeStartStr;
    }

    public String getRangeEndStr() {
        return rangeEndStr;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setRangeStart(Integer rangeStart) {
        this.rangeStart = rangeStart;
    }

    public void setRangeEnd(Integer rangeEnd) {
        this.rangeEnd = rangeEnd;
    }

    public void setRangeStartStr(String rangeStartStr) {
        this.rangeStartStr = rangeStartStr;
    }

    public void setRangeEndStr(String rangeEndStr) {
        this.rangeEndStr = rangeEndStr;
    }

    public void setRangeStartLong(Long rangeStartLong) {
        this.rangeStartLong = rangeStartLong;
    }

    public void setRangeEndLong(Long rangeEndLong) {
        this.rangeEndLong = rangeEndLong;
    }

    public static SupportBeanRange makeLong(String id, String key, Long keyLong, Long rangeStartLong, Long rangeEndLong) {
        SupportBeanRange range = new SupportBeanRange();
        range.setId(id);
        range.setKey(key);
        range.setKeyLong(keyLong);
        range.setRangeStartLong(rangeStartLong);
        range.setRangeEndLong(rangeEndLong);
        return range;
    }
}
