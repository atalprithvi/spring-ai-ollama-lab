package com.stackifydev.springai.web;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.stackifydev.springai.model.ChatReply;
import com.stackifydev.springai.model.StudyTip;
import com.stackifydev.springai.tool.StudyTools;

import reactor.core.publisher.Flux;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Six small endpoints. Each one shows a different Spring AI idea.
 * Open http://localhost:8080 and use the page, or call these URLs directly.
 */
@RestController
public class LearnController {

	private final ChatClient tutorChatClient;
	private final ChatClient memoryChatClient;
	private final StudyTools studyTools;

	public LearnController(
			@Qualifier("tutorChatClient") ChatClient tutorChatClient,
			@Qualifier("memoryChatClient") ChatClient memoryChatClient,
			StudyTools studyTools) {
		this.tutorChatClient = tutorChatClient;
		this.memoryChatClient = memoryChatClient;
		this.studyTools = studyTools;
	}

	/** Lesson 1: one user message in, one string out. */
	@GetMapping("/api/chat")
	ChatReply chat(@RequestParam String message) {
		requireText(message, "message");
		String answer = tutorChatClient.prompt()
				.user(message)
				.call()
				.content();
		return new ChatReply(answer, null);
	}

	/** Lesson 2: a template with placeholders filled in at request time. */
	@GetMapping("/api/explain")
	ChatReply explain(
			@RequestParam String topic,
			@RequestParam(defaultValue = "beginner") String level) {
		requireText(topic, "topic");
		String answer = tutorChatClient.prompt()
				.user(user -> user.text("""
						Explain {topic} to a {level} Java developer.
						Use one everyday analogy, then one tiny Java snippet.
						""")
						.param("topic", topic)
						.param("level", level))
				.call()
				.content();
		return new ChatReply(answer, null);
	}

	/** Lesson 3: ask for a Java object instead of free text. */
	@GetMapping("/api/study-tip")
	StudyTip studyTip(@RequestParam String topic) {
		requireText(topic, "topic");
		return tutorChatClient.prompt()
				.user("Give a practical study tip for learning " + topic + ".")
				.options(ChatOptions.builder().temperature(0.2))
				.call()
				.entity(StudyTip.class);
	}

	/** Lesson 4: tokens arrive as server-sent events while the model is still writing. */
	@GetMapping(value = "/api/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	Flux<String> stream(@RequestParam String message) {
		requireText(message, "message");
		return tutorChatClient.prompt()
				.user(message)
				.stream()
				.content();
	}

	/** Lesson 5: the model may call methods on {@link StudyTools}. */
	@GetMapping("/api/assistant")
	ChatReply assistant(@RequestParam String message) {
		requireText(message, "message");
		String answer = tutorChatClient.prompt()
				.user(message)
				.tools(studyTools)
				.call()
				.content();
		return new ChatReply(answer, null);
	}

	/**
	 * Lesson 6: the same conversationId replays earlier messages on the next call.
	 * Try two requests with the same id: first tell it a name, then ask "what is my name?".
	 */
	@GetMapping("/api/memory")
	ChatReply memory(
			@RequestParam String message,
			@RequestParam(defaultValue = "study-session") String conversationId) {
		requireText(message, "message");
		String answer = memoryChatClient.prompt()
				.user(message)
				.advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
				.call()
				.content();
		return new ChatReply(answer, conversationId);
	}

	private static void requireText(String value, String name) {
		if (!StringUtils.hasText(value)) {
			throw new ResponseStatusException(BAD_REQUEST, name + " is required");
		}
	}

}
