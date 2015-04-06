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

package com.espertech.esper.epl.db;

import junit.framework.TestCase;
import com.espertech.esper.client.ConfigurationDBRef;
import com.espertech.esper.schedule.SchedulingService;
import com.espertech.esper.schedule.SchedulingServiceImpl;
import com.espertech.esper.schedule.ScheduleBucket;
import com.espertech.esper.support.epl.SupportDatabaseService;
import com.espertech.esper.timer.TimeSourceService;
import com.espertech.esper.timer.TimeSourceServiceImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class TestDatabaseServiceImpl extends TestCase
{
    private DatabaseConfigServiceImpl databaseServiceImpl;

    public void setUp()
    {
        Map<String, ConfigurationDBRef> configs = new HashMap<String, ConfigurationDBRef>();

        ConfigurationDBRef config = new ConfigurationDBRef();
        config.setDriverManagerConnection(SupportDatabaseService.DRIVER, "url", new Properties());
        configs.put("name1", config);

        config = new ConfigurationDBRef();
        config.setDataSourceConnection("context", new Properties());
        config.setLRUCache(10000);
        configs.put("name2", config);

        config = new ConfigurationDBRef();
        config.setDataSourceConnection("context", new Properties());
        config.setExpiryTimeCache(1, 3);
        configs.put("name3", config);

        SchedulingService schedulingService = new SchedulingServiceImpl(new TimeSourceServiceImpl());
        databaseServiceImpl = new DatabaseConfigServiceImpl(configs, schedulingService, new ScheduleBucket(1));
    }

    public void testGetConnection() throws Exception
    {
        DatabaseConnectionFactory factory = databaseServiceImpl.getConnectionFactory("name1");
        assertTrue(factory instanceof DatabaseDMConnFactory);

        factory = databaseServiceImpl.getConnectionFactory("name2");
        assertTrue(factory instanceof DatabaseDSConnFactory);
    }

    public void testGetCache() throws Exception
    {
        assertTrue(databaseServiceImpl.getDataCache("name1", null) instanceof DataCacheNullImpl);

        DataCacheLRUImpl lru = (DataCacheLRUImpl) databaseServiceImpl.getDataCache("name2", null);
        assertEquals(10000, lru.getCacheSize());

        DataCacheExpiringImpl exp = (DataCacheExpiringImpl) databaseServiceImpl.getDataCache("name3", null);
        assertEquals(1000, exp.getMaxAgeMSec());
        assertEquals(3000, exp.getPurgeIntervalMSec());
    }

    public void testInvalid()
    {
        try
        {
            databaseServiceImpl.getConnectionFactory("xxx");
            fail();
        }
        catch (DatabaseConfigException ex)
        {
            log.debug(ex);
            // expected
        }
    }

    private static Log log = LogFactory.getLog(TestDatabaseServiceImpl.class);
}
