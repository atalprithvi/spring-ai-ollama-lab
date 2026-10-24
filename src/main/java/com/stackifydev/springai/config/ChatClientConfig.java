package com.stackifydev.springai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

	/**
	 * A fresh {@link ChatClient.Builder} is a prototype. Call {@code getObject()}
	 * once per client so system prompts and advisors do not leak between them.
	 */
	@Bean
	ChatClient tutorChatClient(ObjectProvider<ChatClient.Builder> builders) {
		return builders.getObject()
				.defaultSystem("""
						You are a patient Java and Spring tutor.
						Answer in plain language, then give one short example.
						Stay under 120 words unless the student asks for more.
						""")
				.build();
	}

	@Bean
	ChatMemory chatMemory() {
		return MessageWindowChatMemory.builder()
				.chatMemoryRepository(new InMemoryChatMemoryRepository())
				.maxMessages(12)
				.build();
	}

	@Bean
	ChatClient memoryChatClient(ObjectProvider<ChatClient.Builder> builders, ChatMemory chatMemory) {
		return builders.getObject()
				.defaultSystem("""
						You are a study partner. Remember names, goals, and facts
						the student tells you, and use them in later replies.
						""")
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
				.build();
	}

}
