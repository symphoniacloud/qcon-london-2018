package io.symphonia;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.HashMap;
import java.util.UUID;

public class Lambda1 {

    private static String LAMBDA_ID = UUID.randomUUID().toString();
    private static Integer TIMEOUT_SECONDS = Integer.parseInt(System.getenv("TIMEOUT_SECONDS"));

    private static String PRODUCT_TABLE = System.getenv("PRODUCT_TABLE");
    private static String PRODUCT_ID = "what_is_serverless";

    private AmazonDynamoDB dynamoDBClient = AmazonDynamoDBClientBuilder.defaultClient();
    private Integer invocationCount = 0;

    public APIGatewayProxyResponseEvent handler(APIGatewayProxyRequestEvent event, Context context) {

        HashMap<String, AttributeValue> updateKey = new HashMap<>();
        updateKey.put("productId", new AttributeValue(PRODUCT_ID));

        HashMap<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":q", new AttributeValue().withN("1"));
        expressionValues.put(":z", new AttributeValue().withN("0"));

        UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                .withTableName(PRODUCT_TABLE)
                .withKey(updateKey)
                .withUpdateExpression("SET quantity = quantity - :q")
                .withExpressionAttributeValues(expressionValues)
                .withConditionExpression("quantity > :z");

        boolean success = false;

        try {
            dynamoDBClient.updateItem(updateItemRequest);
            success = true;
        } catch (ConditionalCheckFailedException exception) {
            // NOP
        }

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);

        HashMap<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Content-type", "text/html; charset=utf-8");
        response.setHeaders(responseHeaders);

        response.setBody(
                String.format("<html><body>%s<hr><p>Lambda: %s</p><p>Request: %s</p><p>Invocations: %d</p><p>Duration: %d milliseconds</p></body></html>",
                        success ? "<h1>SUCCESS</h1><p>You've won a physical copy of 'What is Serverless?'!</p>" : "<h1>SOLD OUT</h1><p>No more physical copies are available, but you can download a digital copy of 'What is Serverless?' <a href=\"https://www.oreilly.com/programming/free/what-is-serverless.csp\">here</a>.</p>",
                        LAMBDA_ID, context.getAwsRequestId(), invocationCount++,
                        (TIMEOUT_SECONDS * 1000) - context.getRemainingTimeInMillis()));

        return response;
    }

}
