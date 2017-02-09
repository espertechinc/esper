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
package com.espertech.esper.supportunit.epl;

import com.espertech.esper.client.ConfigurationDBRef;
import com.espertech.esper.core.support.SupportEngineImportServiceFactory;
import com.espertech.esper.core.support.SupportSchedulingServiceImpl;
import com.espertech.esper.epl.db.DatabaseConfigServiceImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SupportDatabaseService {
    public final static String DBNAME_FULLURL = "mydb";
    public final static String DBNAME_PARTURL = "mydb_part";

    public final static String DBUSER = "root";
    public final static String DBPWD = "password";
    public final static String DRIVER = "com.mysql.jdbc.Driver";
    public final static String FULLURL = "jdbc:mysql://localhost/test?user=root&password=password";
    public final static String PARTURL = "jdbc:mysql://localhost/test";

    public static DatabaseConfigServiceImpl makeService() {
        Map<String, ConfigurationDBRef> configs = new HashMap<String, ConfigurationDBRef>();

        ConfigurationDBRef config = new ConfigurationDBRef();
        config.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configs.put(DBNAME_FULLURL, config);

        config = new ConfigurationDBRef();
        Properties properties = new Properties();
        properties.put("user", DBUSER);
        properties.put("password", DBPWD);
        config.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.PARTURL, properties);
        configs.put(DBNAME_PARTURL, config);

        return new DatabaseConfigServiceImpl(configs, new SupportSchedulingServiceImpl(), null, SupportEngineImportServiceFactory.make());
    }
}
