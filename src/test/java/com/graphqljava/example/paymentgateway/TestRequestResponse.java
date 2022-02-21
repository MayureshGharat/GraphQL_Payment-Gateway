package com.graphqljava.example.paymentgateway;

import Pojo.RequestPojo;
import Pojo.RequestResponsePojo;
import Pojo.ResponsePojo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class TestRequestResponse {
    private List<RequestResponsePojo> testRequestResponses;

    @Before
    public void initializeRequests() throws RuntimeException {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<RequestResponsePojo>>() {}.getType();
        try {
            testRequestResponses = gson.fromJson(GraphQLDataFetchers.loadFileFromClasspath("requestlog-charges.json"), listType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testRequestResponse() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            for (RequestResponsePojo testData : testRequestResponses) {
                RequestPojo testRequest = testData.getRequest();
                ResponsePojo testResponse = testData.getResponse();
                HttpPost postRequest = new HttpPost("https://api.stripe.com/v1/charges");
                for (Map.Entry<String, String> header : testRequest.getHeaders().entrySet()) {
                    postRequest.addHeader(header.getKey(), header.getValue());
                }
                postRequest.setEntity(new StringEntity(testRequest.getBody()));
                CloseableHttpResponse postResponse = httpClient.execute(postRequest);
                if (postResponse.getStatusLine().getStatusCode() == testResponse.getCode()) {
                    System.out.println("...Test for RequestId : " + testRequest.getId() + " passed...");
                } else {
                    throw new RuntimeException("...Test for RequestId : " + testRequest.getId() + " failed...");
                }
                postResponse.close();
            }
        } finally {
            httpClient.close();
        }
    }
}
