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
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;

public abstract class JMSContextFactory {
    public static JMSContext createContext(String ctxFactory, String url, String connectionFactoryName, String user, String pwd, String jmsDestination, boolean isTopic) throws JMSException, NamingException {
        ConnectionFactory factory = getConnectionFactory(ctxFactory, url, connectionFactoryName, user, pwd);
        Connection connection = factory.createConnection(user, pwd);
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Destination destination;
        if (isTopic) {
            destination = session.createTopic(jmsDestination);
        } else {
            destination = session.createQueue(jmsDestination);
        }

        return new JMSContext(factory, connection, session, destination);
    }

    @SuppressWarnings("unchecked")
    private static ConnectionFactory getConnectionFactory(String ctxFactory, String url, String connectionFactoryName, String user, String pwd) throws NamingException {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, ctxFactory);
        env.put(Context.PROVIDER_URL, url);
        env.put(Context.SECURITY_PRINCIPAL, user);
        env.put(Context.SECURITY_CREDENTIALS, pwd);
        InitialContext jndiContext = new InitialContext(env);
        return (ConnectionFactory) jndiContext.lookup(connectionFactoryName);
    }


}
