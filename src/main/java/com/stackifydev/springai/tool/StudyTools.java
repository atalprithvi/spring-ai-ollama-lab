package com.stackifydev.springai.tool;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Ordinary Java methods the model can call. The model does not run them;
 * Spring AI executes the method and sends the return value back.
 */
@Component
public class StudyTools {

	private static final Map<String, String> GLOSSARY = Map.of(
			"bean", "A bean is an object Spring creates, configures, and manages for you.",
			"chatclient", "ChatClient is the fluent API for sending prompts to a model and reading the reply.",
			"ollama", "Ollama runs open models on your machine and exposes them at http://localhost:11434.",
			"prompt", "A prompt is the instruction text, plus optional system guidance, sent to the model.",
			"tool", "A tool is a Java method the model may ask Spring AI to run, such as a calculator or lookup.");

	@Tool(description = "Current local date and time. Use this when the user asks for the date, time, or day.")
	public String currentDateTime() {
		return ZonedDateTime.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy, HH:mm z"));
	}

	@Tool(description = "Add two numbers. Use this instead of doing arithmetic yourself.")
	public double add(
			@ToolParam(description = "First number") double a,
			@ToolParam(description = "Second number") double b) {
		return a + b;
	}

	@Tool(description = "Look up a short definition from the local Spring glossary. Terms: bean, chatclient, ollama, prompt, tool.")
	public String glossary(@ToolParam(description = "Glossary term, for example bean or ollama") String term) {
		String key = term == null ? "" : term.toLowerCase(Locale.ROOT).replace(" ", "");
		return GLOSSARY.getOrDefault(key, "No glossary entry for '" + term + "'. Known terms: " + GLOSSARY.keySet());
	}

}
