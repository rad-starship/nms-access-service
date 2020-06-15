package com.rad.server.access.Configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfiguration
{
	@Bean
	public NewTopic topicExample()
	{
		return TopicBuilder.name("events").
				partitions(1). //the number of partitions for the topic
				replicas(1). //the number of replicas for the topic
				build();
	}
}