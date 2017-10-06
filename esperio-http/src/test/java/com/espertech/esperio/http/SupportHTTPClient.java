package com.espertech.esperio.http;

import com.espertech.esperio.http.core.URIUtil;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

public class SupportHTTPClient {
    private final static Logger log = LoggerFactory.getLogger(SupportHTTPClient.class);

    private final int port;
    private HttpClient httpclient;

    public SupportHTTPClient(int port) {
        this.port = port;
        httpclient = new DefaultHttpClient();
    }

    public void request(int port, String document, String... parameters) throws Exception {
        String uri = "http://localhost:" + port + "/" + document;
        URI requestURI = URIUtil.withQuery(new URI(uri), parameters);
        log.info("Requesting from URI " + requestURI);
        HttpGet httpget = new HttpGet(requestURI);

        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = null;
        try {
            responseBody = httpclient.execute(httpget, responseHandler);
        } catch (IOException e) {
            throw new RuntimeException("Error executing request:" + e.getMessage());
        }
    }


}
