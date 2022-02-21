package com.graphqljava.example.paymentgateway;

import Pojo.RequestPojo;
import Pojo.RequestResponsePojo;
import Pojo.ResponsePojo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.stripe.Stripe;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.PaymentIntentCreateParams;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GraphQLDataFetchers {
    private static Logger logger = LoggerFactory.getLogger(GraphQLDataFetchers.class);
    private final Map<Integer, RequestPojo> requests;

    public GraphQLDataFetchers() {
        requests = new HashMap<>();
        initializeRequests();
        Stripe.apiKey = "sk_test_4eC39HqLyjWDarjtT1zdp7dc";
    }

    public void initializeRequests() throws RuntimeException {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<RequestResponsePojo>>() {
        }.getType();
        try {
            List<RequestResponsePojo> storedRequestResponses = gson.fromJson(loadFileFromClasspath("requestlog-charges.json"), listType);
            for (RequestResponsePojo reqResp : storedRequestResponses) {
                RequestPojo requestPojo = reqResp.getRequest();
                this.requests.put(requestPojo.getId(), requestPojo);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String loadFileFromClasspath(String fileName) throws IOException {
        ClassLoader classLoader = PaymentGatewayApplication.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(fileName)) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }

    public DataFetcher getRequestValidator() {
        return new RequestValidator(this);
    }

    private class RequestValidator implements DataFetcher {
        private final GraphQLDataFetchers graphQLDataFetchers;
        private final Gson gson;
        private final CloseableHttpClient httpClient;

        public RequestValidator(GraphQLDataFetchers graphQLDataFetchers) {
            this.graphQLDataFetchers = graphQLDataFetchers;
            this.gson = new Gson();
            this.httpClient = HttpClients.createDefault();
        }

        @Override
        public Object get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
            Integer requestID = dataFetchingEnvironment.getArgument("requestID");
            if (this.graphQLDataFetchers.requests.containsKey(requestID)) {
                RequestPojo request = this.graphQLDataFetchers.requests.get(requestID);
                HttpResponse response = send(request);
                return convertHttpResponseToJson(response);
            }
            return "Error : Unknown requestID";
        }

        private HttpResponse send(RequestPojo request) throws IOException {
            String responseString = null;
            CloseableHttpResponse postResponse = null;
            HttpPost postRequest = new HttpPost("https://api.stripe.com/v1/charges");
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                postRequest.addHeader(header.getKey(), header.getValue());
            }
            postRequest.setEntity(new StringEntity(request.getBody()));
            postResponse = this.httpClient.execute(postRequest);
            return postResponse;
        }

        private String convertHttpResponseToJson(HttpResponse response) throws IOException {
            HttpEntity entity = response.getEntity();
            String responseString = "";
            if (entity != null) {
                String result = EntityUtils.toString(entity);
                ResponsePojo responsePojo = new ResponsePojo();
                responsePojo.setBody(result);
                Header[] headers = response.getAllHeaders();
                Map<String, String> responseHeaders = new HashMap<>();
                for (Header header : headers) {
                    responseHeaders.put(header.getName(), header.getValue());
                }
                responsePojo.setHeaders(responseHeaders);
                responsePojo.setCode(response.getStatusLine().getStatusCode());
                responseString = this.gson.toJson(responsePojo);
            }
            return responseString;
        }
    }

    public DataFetcher getChargerHolder() {
        return new ChargeHolder(this);
    }

    private class ChargeHolder implements DataFetcher {
        private final GraphQLDataFetchers graphQLDataFetchers;
        private final Gson gson;
        private final CloseableHttpClient httpClient;

        public ChargeHolder(GraphQLDataFetchers graphQLDataFetchers) {
            this.graphQLDataFetchers = graphQLDataFetchers;
            this.gson = new Gson();
            this.httpClient = HttpClients.createDefault();
        }

        @Override
        public Object get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
            Integer clientId = dataFetchingEnvironment.getArgument("clientId");
            Integer amount = dataFetchingEnvironment.getArgument("amount");
            // Create a customer
            Map<String, Object> customerMetadata = new HashMap<>();
            Map<String, Object> params = new HashMap<>();
            customerMetadata.put("clientId", clientId);
            params.put("metadata", customerMetadata);
            Customer customer = Customer.create(params);
            String customerId = customer.getId();

            // Create a PaymentIntent. The amount will be charged to the customer once the charge is updated wih tip.
            PaymentIntentCreateParams paymentIntentCreateParams =
                    PaymentIntentCreateParams.builder()
                            .addPaymentMethodType("card")
                            .setAmount(Long.valueOf(amount))
                            .setCurrency("usd")
                            .setPaymentMethod("pm_card_visa")
                            .setCustomer(customerId)
                            .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                            .setConfirm(true)
                            .build();

            PaymentIntent paymentIntent = PaymentIntent.create(paymentIntentCreateParams);
            logger.info("Created Payment Intent with id : {}, for customer : {}, internal customer_id : {}", paymentIntent.getId(), clientId, customerId);
            return paymentIntent.getId();
        }
    }

    public DataFetcher getChargeUpdater() {
        return new GraphQLDataFetchers.ChargeUpdater(this);
    }

    private class ChargeUpdater implements DataFetcher {
        private final GraphQLDataFetchers graphQLDataFetchers;
        private final Gson gson;
        private final CloseableHttpClient httpClient;

        public ChargeUpdater(GraphQLDataFetchers graphQLDataFetchers) {
            this.graphQLDataFetchers = graphQLDataFetchers;
            this.gson = new Gson();
            this.httpClient = HttpClients.createDefault();
        }

        @Override
        public Object get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
            String paymentIntentId = dataFetchingEnvironment.getArgument("chargeId");
            Integer tipAmount = dataFetchingEnvironment.getArgument("tipAmount");
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            if (paymentIntent.getStatus().equals("requires_capture")) {
                Long authorizedAmount = paymentIntent.getAmount();
                logger.info("Existing authorized amount : {}",authorizedAmount);
                logger.info("Final amount with tip : {}",(authorizedAmount + tipAmount));
                PaymentIntentCaptureParams paymentIntentCaptureParams =
                        PaymentIntentCaptureParams.builder().setAmountToCapture(authorizedAmount).build();
                PaymentIntent updatedPayment = paymentIntent.capture(paymentIntentCaptureParams);
                if (updatedPayment.getStatus().equals("succeeded")) {
                    logger.info("Payment succeeded for customer : {}",updatedPayment.getCustomer());
                    return true;
                }
            }  else if (paymentIntent.getStatus().equals("succeeded")) {
                logger.info("Payment was already applied Payment intent : {}",paymentIntent.getId());
                return true;
            }
            return false;
        }
    }
}

