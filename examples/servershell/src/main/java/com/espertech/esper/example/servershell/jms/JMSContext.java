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
package com.espertech.esper.example.servershell.jms;

import javax.jms.*;

public class JMSContext {
    private ConnectionFactory factory;
    private Connection connection;
    private Session session;
    private Destination destination;

    public JMSContext(ConnectionFactory factory, Connection connection, Session session, Destination destination) {
        this.factory = factory;
        this.connection = connection;
        this.session = session;
        this.destination = destination;
    }

    public void destroy() throws JMSException {
        session.close();
        connection.close();
    }

    public Connection getConnection() {
        return connection;
    }

    public Destination getDestination() {
        return destination;
    }

    public ConnectionFactory getFactory() {
        return factory;
    }

    public Session getSession() {
        return session;
    }
}
