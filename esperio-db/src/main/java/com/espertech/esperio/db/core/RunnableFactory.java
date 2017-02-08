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
package com.espertech.esperio.db.core;

import com.espertech.esper.adapter.BaseSubscription;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.epl.db.DatabaseConfigService;
import com.espertech.esper.epl.db.DatabaseConnectionFactory;
import com.espertech.esper.epl.db.DatabaseConfigException;
import com.espertech.esper.util.SQLTypeMapUtil;
import com.espertech.esperio.db.config.UpsertQuery;
import com.espertech.esperio.db.config.Column;

import java.util.UUID;
import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface RunnableFactory
{
    public Runnable makeRunnable(EventBean theEvent);
}
