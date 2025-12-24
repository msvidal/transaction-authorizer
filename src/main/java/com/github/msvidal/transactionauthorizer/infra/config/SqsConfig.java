package com.github.msvidal.transactionauthorizer.infra.config;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;

import java.net.URI;

@Slf4j
@Configuration
@Profile({"listener","local","api"})
public class SqsConfig {

    @Value("${aws.sqs.endpoint:}")
    private String sqsEndpoint;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        SqsAsyncClientBuilder builder = SqsAsyncClient.builder()
                .region(Region.of(awsRegion));

        if (sqsEndpoint != null && !sqsEndpoint.isEmpty()) {
            log.info("Configuring SQS client with LocalStack endpoint: {}", sqsEndpoint);
            builder.endpointOverride(URI.create(sqsEndpoint))
                   .credentialsProvider(StaticCredentialsProvider.create(
                           AwsBasicCredentials.create("test", "test")
                   ));
        } else {
            log.info("Configuring SQS client with AWS endpoint in region: {}", awsRegion);
        }

        return builder.build();
    }

    @Bean
    public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
        log.info("Initializing SQS Template");
        return SqsTemplate.newTemplate(sqsAsyncClient);
    }

    @Bean
    public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(
            SqsAsyncClient sqsAsyncClient) {
        log.info("Initializing SQS Message Listener Container Factory");
        return SqsMessageListenerContainerFactory
                .builder()
                .sqsAsyncClient(sqsAsyncClient)
                .build();
    }
}
