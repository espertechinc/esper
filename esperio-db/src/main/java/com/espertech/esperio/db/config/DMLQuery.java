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
package com.espertech.esperio.db.config;

import java.util.ArrayList;
import java.util.List;

public class DMLQuery {

    private String sql;
    private String connection;
    private String stream;
    private String name;
    private String executorName;
    private Integer retry;
    private Double retryIntervalSec;
    private List<BindingParameter> bindings;

    public DMLQuery() {
        bindings = new ArrayList<BindingParameter>();
    }

    public List<BindingParameter> getBindings() {
        return bindings;
    }

    public void setBindings(List<BindingParameter> bindings) {
        this.bindings = bindings;
    }

    public Integer getRetry() {
        return retry;
    }

    public void setRetry(Integer retry) {
        this.retry = retry;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getConnection() {
        return connection;
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExecutorName() {
        return executorName;
    }

    public void setExecutorName(String executorName) {
        this.executorName = executorName;
    }

    public Double getRetryIntervalSec() {
        return retryIntervalSec;
    }

    public void setRetryIntervalSec(Double retryIntervalSec) {
        this.retryIntervalSec = retryIntervalSec;
    }
}