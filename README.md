# Spring AI Ollama Lab

A small local learning lab for [Spring AI](https://docs.spring.io/spring-ai/reference/). Six lessons each expose one HTTP endpoint and call a chat model running in [Ollama](https://ollama.com) on your machine. No API key.

Suggested GitHub repository name: `spring-ai-ollama-lab`.

Java sources live in `com.stackifydev.springai` ([stackifydev.com](https://stackifydev.com)).

## What it uses

| Piece | Where it comes from |
| --- | --- |
| Spring Boot 4.1.2 | `org.springframework.boot` plugin in `build.gradle` |
| Spring AI 2.0.1 | `spring-ai-bom` and `spring-ai-starter-model-ollama` |
| Gradle 9.8.2 | Wrapper in `gradle/wrapper/gradle-wrapper.properties`. Use `gradlew` / `gradlew.bat`. A global Gradle install is not required. |
| Java 27 toolchain | `java.toolchain.languageVersion` in `build.gradle`. Spring Boot 4 runs on newer JDKs such as 21 and 27. This build is pinned to 27, so install JDK 27, or change `languageVersion` if you build with another JDK 21 or newer. |
| Ollama | Local only. Default URL `http://localhost:11434` (`spring.ai.ollama.base-url`). |
| Chat model `qwen3:4b` | `spring.ai.ollama.chat.model` in `application.properties`. Change that property to use another pulled model, for example `qwen3:1.7b`. |
| Embedded Tomcat, Spring Web MVC | `spring-boot-starter-webmvc`, port 8080 (`server.port`). |

The app does not download a model on startup (`spring.ai.ollama.init.pull-model-strategy=never`). Pull it with Ollama first. `spring.ai.ollama.chat.think=false` turns off Qwen's long reasoning trace, and the default temperature is `0.4`.

## Lessons

| Lesson | Endpoint | What it does |
| --- | --- | --- |
| 1. One user message | `GET /api/chat?message=` | `ChatClient` sends the text and returns a `ChatReply`. |
| 2. Prompt template | `GET /api/explain?topic=&level=` | Fills `{topic}` and `{level}` in a fixed prompt. `level` defaults to `beginner`. |
| 3. Structured output | `GET /api/study-tip?topic=` | `entity(StudyTip.class)` maps the reply onto `title`, `steps`, and `whyItMatters`. This call uses temperature `0.2`. |
| 4. Streaming | `GET /api/stream?message=` | `stream().content()` as `text/event-stream`. |
| 5. Tool calling | `GET /api/assistant?message=` | The model may call `StudyTools`: local date and time, addition, and a small glossary (`bean`, `chatclient`, `ollama`, `prompt`, `tool`). |
| 6. Conversation memory | `GET /api/memory?message=&conversationId=` | `MessageChatMemoryAdvisor` keeps history for that id (default `study-session`). Memory is in-process and holds the last 12 messages. The same id on a later call can recall earlier facts, such as a name. |

A blank `message` or `topic` returns HTTP 400. The page at `http://localhost:8080` calls these same endpoints.

Code to read, in order:

1. `src/main/resources/application.properties` — Ollama URL and model
2. `src/main/java/com/stackifydev/springai/config/ChatClientConfig.java` — system prompt and memory
3. `src/main/java/com/stackifydev/springai/web/LearnController.java` — the six calls
4. `src/main/java/com/stackifydev/springai/tool/StudyTools.java` — methods the model can call
5. `src/main/java/com/stackifydev/springai/model/ChatReply.java` and `StudyTip.java` — response records

## Run

Install JDK 27 so it matches the toolchain in `build.gradle`. Spring Boot 4 also runs on newer JDKs such as 21; if you use one of those, set `languageVersion` in `build.gradle` to that version before building.

Install Ollama, then pull the model and start the Ollama app (or `ollama serve`):

```bash
ollama pull qwen3:4b
```

From the project directory:

```powershell
.\gradlew.bat bootRun
```

On macOS or Linux:

```bash
./gradlew bootRun
```

Open [http://localhost:8080](http://localhost:8080).

On Windows, `.\run.ps1` is an optional helper. It selects a JDK 21 or newer (`JAVA_HOME`, then `java` on `PATH`, then a JDK already installed on the machine), checks that Ollama answers at `http://localhost:11434`, and runs `gradlew.bat bootRun`. Gradle still needs a JDK that satisfies the Java 27 toolchain.

The first chat after startup can take a while. Ollama loads `qwen3:4b` into memory on that request. `qwen3:4b` follows tool calls and JSON shape more reliably than `qwen3:1.7b`.

## Try it

```bash
curl "http://localhost:8080/api/chat?message=What%20is%20a%20bean"
curl "http://localhost:8080/api/assistant?message=What%20time%20is%20it%20and%20what%20is%2017%20plus%2025"
```

Memory is per conversation id. The second call should still know the name:

```bash
curl "http://localhost:8080/api/memory?conversationId=demo&message=My%20name%20is%20Alex"
curl "http://localhost:8080/api/memory?conversationId=demo&message=What%20is%20my%20name"
```

In Windows PowerShell, call `curl.exe` so `curl` is not aliased to `Invoke-WebRequest`.

## Port 8080 already in use

Stop the process that is listening, or set another port in `src/main/resources/application.properties`:

```properties
server.port=8081
```

Windows:

```powershell
netstat -ano | findstr :8080
taskkill /PID <pid> /F
```

macOS or Linux:

```bash
lsof -i :8080
kill <pid>
```
