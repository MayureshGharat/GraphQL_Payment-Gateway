package com.graphqljava.example.paymentgateway;

import com.stripe.exception.StripeException;
import org.apache.commons.io.IOUtils;
import org.json.simple.parser.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class PaymentGatewayApplication {

    private static Logger logger = LoggerFactory.getLogger(PaymentGatewayApplication.class);

    public static String loadFileFromClasspath(String fileName) throws IOException {
        ClassLoader classLoader = PaymentGatewayApplication.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(fileName)) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }

    public static void main(String[] args) throws ParseException, IOException, StripeException {
		SpringApplication.run(PaymentGatewayApplication.class, args);
		logger.info("Server started and can be accessed at \"http://localhost:8080/graphql\", using \"https://github.com/graphql/graphql-playground\"");
    }
}
