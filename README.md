# GraphQL_Payment-Gateway
Reference: https://www.graphql-java.com/tutorials/getting-started-with-spring-boot/

# Running test for request response for the Stripe's "/v1/charges" API
You can run the "src/test/java/com/graphqljava/example/paymentgateway/TestRequestResponse.java".
The "requestlog-charges.json" file used in the test is located at "src/main/resources/requestlog-charges.json" 

# Starting the PaymentGatewayApplication server 

You can run the main function in "PaymentGatewayApplication" class.
This will start the GraphQL server on localhost:8080 and the apis are accessible at "http://localhost:8080/graphql"
The easiest way to try this out is using : https://github.com/prisma/graphql-playground

ValidateRequest API:![Screen Shot 2022-02-21 at 3 01 27 PM](https://user-images.githubusercontent.com/4326831/155035998-502a1f74-7283-4e17-b46a-143b0241c0fd.png)

ChargeClient API:![Screen Shot 2022-02-21 at 3 01 43 PM](https://user-images.githubusercontent.com/4326831/155036022-1dc61a7f-6320-4bd0-b9fe-e9179f8b8973.png)

UpdateChargeWithTip API:![Screen Shot 2022-02-21 at 3 02 00 PM](https://user-images.githubusercontent.com/4326831/155036102-cc2d3c74-d576-4129-973d-7c4ba0bdeb40.png)

