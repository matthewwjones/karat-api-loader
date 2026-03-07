# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**karat-api-loader** is a Java 21 SpringBoot 4.0 
application. Group: `com.mattjoneslondon`. Build system: Gradle 9.0.0. Test framework: JUnit 5 (Jupiter).

## Commands

```bash
# Build
./gradlew build          # Compile, test, and package
./gradlew clean build    # Clean then build

# Test
./gradlew test                              # Run all tests
./gradlew test --tests "ClassName"          # Run a single test class
./gradlew test --tests "ClassName.method"   # Run a single test method
./gradlew test --info                       # Run tests with verbose output

# Compile only
./gradlew compileJava
```

## Architecture

Early-stage Java project. Source layout follows standard Maven/Gradle conventions:

- `src/main/java/` — application source (package root: `com.mattjoneslondon`)
- `src/main/resources/` — runtime resources and configuration
- `src/test/java/` — JUnit 5 tests mirroring the main package structure
- `src/test/resources/` — test fixtures and configuration

### Package Conventions

All records and POJOs must be placed in the `domain` package, never as nested classes:

| Type | Package |
|------|---------|
| Domain records/POJOs (e.g. `KaratUser`, `KaratGroup`) | `com.mattjoneslondon.domain` |
| API response/request records (e.g. `UsersPage`, `UserResponse`) | `com.mattjoneslondon.domain.client` |

No linting or code formatting tools are currently configured. However, IDE diagnostics (warnings and suggestions) must always be resolved before finishing any task.

## Conventions

- **Dependency versions** must always be managed in `gradle/libs.versions.toml`. Never hardcode versions directly in `build.gradle`.

## Code Quality

Follow the principles in *Clean Code* by Robert C. Martin.

### Naming
- Names must reveal intent — if a name requires a comment, it is not good enough.
- Use pronounceable, searchable names. Avoid encodings, prefixes, and noise words (`data`, `info`, `Manager`).
- Class names: nouns. Method names: verbs.
- Avoid magic numbers — use named constants.
- Make meaningful distinctions — `InterviewData`, `InterviewInfo`, `InterviewRecord` are indistinguishable noise.
- Avoid mental mapping — don't make the reader translate `i` to `interviewIndex`. Single-letter variables are only acceptable as loop counters in tiny scopes.

```java
// wrong
int d; // elapsed time in days
static final int MAX = 86400;
String s; // the interview status

// correct
int elapsedTimeInDays;
static final int SECONDS_PER_DAY = 86400;
String interviewStatus;

// wrong — noise words, abbreviations requiring mental mapping
class InterviewData { ... }
Interview getIntrvw(String intId) { ... }

// correct
class Interview { ... }
Interview findInterviewById(String interviewId) { ... }
```

### Functions
- Small — do one thing, do it well, do it only.
- One level of abstraction per function. Don't mix high-level orchestration with low-level detail in the same method.
- Fewer arguments is better; three or more arguments should be questioned. Avoid flag (boolean) arguments — they signal the function does two things.
- No side effects — a function that says it does one thing must not secretly do another.
- Command/Query Separation: a function should either do something (command) or answer something (query), never both.

```java
// wrong — flag argument, mixed abstraction levels
void processInterview(Interview interview, boolean sendEmail) {
    interview.setStatus("complete");
    interviewRepository.save(interview);
    if (sendEmail) {
        String body = "Dear " + interview.getCandidateName() + ", ...";
        emailClient.send(interview.getCandidateEmail(), body);
    }
}

// wrong — violates command/query separation: mutates AND returns
Interview saveAndReturn(Interview interview) {
    repository.save(interview);
    return interview;
}

// correct — each method does one thing at one level of abstraction
void completeInterview(Interview interview) {
    markAsComplete(interview);
    notifyCandidate(interview);
}

private void markAsComplete(Interview interview) {
    interview.setStatus("complete");
    interviewRepository.save(interview);
}

private void notifyCandidate(Interview interview) {
    emailClient.send(buildCompletionEmail(interview));
}

// correct — command and query are separate
void save(Interview interview) { repository.save(interview); }
boolean exists(String interviewId) { return repository.existsById(interviewId); }
```

### Comments
- Prefer self-documenting code over comments. A comment is a failure to express intent in code.
- Delete commented-out code — version control remembers it.
- Acceptable: legal notices, intent explanation for genuinely complex decisions, warnings, TODO notes (but act on them promptly).
- Never use a comment when you can use a well-named variable or method instead.

```java
// wrong — comment restates what the code says
// get the candidate's full name
String name = candidate.getFullName();

// wrong — commented-out dead code
// interview.setStatus("pending");
interview.setStatus("complete");

// wrong — comment compensating for a bad name
// check if the interview is eligible for scoring
if (i.getS() > 0 && i.getC() != null) { ... }

// correct — rename so the comment is unnecessary
if (interview.isEligibleForScoring()) { ... }

// correct — comment explains a non-obvious domain decision
// Karat's API returns scores as integers 0–100 but the UI displays them as 0.0–10.0
double displayScore = rawScore / 10.0;
```

### Classes
- Small — measured by responsibilities, not lines.
- Single Responsibility Principle: one reason to change.
- Open/Closed Principle: open for extension, closed for modification.
- Prefer composition over inheritance.
- Cohesion: instance variables should be used by most methods. When a subset of methods uses only a subset of variables, that subset wants to be its own class.
- Dependencies should be injected (via constructor), not created internally — this makes classes testable and decoupled.

```java
// wrong — one class has multiple reasons to change:
// changes to the API, the data model, and the DB schema all require editing it
class InterviewService {
    void fetchFromApi() { ... }
    Interview parseResponse(String json) { ... }
    void saveToDatabase(Interview interview) { ... }
}

// wrong — creates its own dependency, impossible to test in isolation
class InterviewLoader {
    private final KaratApiClient client = new KaratApiClient();
}

// correct — each class has one responsibility; dependencies are injected
class KaratApiClient {
    List<Interview> fetchInterviews() { ... }
}

class InterviewRepository {
    void save(Interview interview) { ... }
}

class InterviewLoader {
    private final KaratApiClient client;
    private final InterviewRepository repository;

    InterviewLoader(KaratApiClient client, InterviewRepository repository) {
        this.client = client;
        this.repository = repository;
    }

    void load() {
        client.fetchInterviews().forEach(repository::save);
    }
}
```

### Error Handling
- Use exceptions, not return codes. Error codes force callers to handle errors immediately and clutter logic.
- Never return or pass `null`. Return `Optional` for absent values; throw for unexpected failures.
- Use unchecked exceptions (extend `RuntimeException`) unless the caller can genuinely recover.
- Provide enough context in exception messages to diagnose the problem — include the operation that failed and relevant IDs.
- Don't swallow exceptions with an empty catch block.

```java
// wrong — return code, caller can forget to check it
int loadInterviews() {
    if (apiClient.isUnavailable()) return -1;
    return 0;
}

// wrong — null return, every caller must null-check
Interview findById(String id) {
    if (!store.containsKey(id)) return null;
    return store.get(id);
}

// wrong — swallowed exception hides failures completely
try {
    apiClient.fetchInterviews();
} catch (Exception e) {
    // do nothing
}

// correct — throw with context, use Optional for absent values
void loadInterviews() {
    if (apiClient.isUnavailable()) {
        throw new KaratApiException("Failed to load interviews: Karat API is unreachable");
    }
}

Optional<Interview> findById(String interviewId) {
    return Optional.ofNullable(store.get(interviewId));
}

// correct — catch, log with context, rethrow or wrap
try {
    apiClient.fetchInterviews();
} catch (HttpClientErrorException e) {
    throw new KaratApiException("Failed to fetch interviews from Karat API: " + e.getStatusCode(), e);
}
```

### General
- DRY: Don't Repeat Yourself.
- Boy Scout Rule: always leave the code cleaner than you found it.
- Code is read far more than it is written — optimise for the reader.
- Never use ternary expressions (`? :`). Always use `if`/`else` — they are easier to read.

### Formatting
- All code must be formatted to IntelliJ defaults and imports optimised (unused imports removed, imports ordered per IntelliJ conventions) before saving.
- No blank lines between field declarations:

```java
// correct
public class KaratClient {
    private static final String INTERVIEWS_QUERY = "...";
    private final GraphQlClient graphQlClient;
    private final String apiKey;

// wrong
public class KaratClient {

    private static final String INTERVIEWS_QUERY = "...";
    private final GraphQlClient graphQlClient;

    private final String apiKey;
```

- Align continuation parameters with the opening parenthesis, not indented on a new line:

```java
// correct
public KaratClient(@Value("${karat.base-url}") String baseUrl,
                   @Value("${karat.api-key}") String apiKey) {

// wrong
public KaratClient(
        @Value("${karat.base-url}") String baseUrl,
        @Value("${karat.api-key}") String apiKey
) {
```

### Testing
- Write unit tests for all code that contains logic. POJOs with only getters/setters do not need tests.
- Always use Hamcrest matchers (`assertThat`, `is`, `hasSize`, `empty`, etc.) for assertions, except in Spring Boot controller tests where MockMvc's fluent assertions are preferred.
- When a test has multiple assertions, always wrap them in `assertAll()` (JUnit 5). The closing `);` must be on its own line, indented to align with `assertAll`:
```java
// correct
assertAll(
        () -> assertEquals("Alice", users.get(0).name()),
        () -> assertEquals("Bob", users.get(1).name())
);

// wrong
assertAll(
        () -> assertEquals("Alice", users.get(0).name()),
        () -> assertEquals("Bob", users.get(1).name()));
```
- Tests must be FIRST: Fast, Independent, Repeatable, Self-Validating, Timely.
- One concept per test. Test code deserves the same care as production code.
- Structure tests as Arrange / Act / Assert with a blank line separating each phase.
- Test names should read as sentences describing behaviour: `loadedInterviewHasCorrectStatus`, not `testLoad` or `test1`.

```java
// wrong — poor name, no assertAll, no AAA structure
@Test
void test1() {
    Interview interview = loader.load("abc123");
    assertEquals("abc123", interview.getId());
    assertEquals("John Smith", interview.getCandidateName());
    assertEquals("COMPLETE", interview.getStatus());
}

// wrong — tests two unrelated concepts in one test
@Test
void testLoadAndError() {
    assertNotNull(loader.load("abc123"));
    assertThrows(KaratApiException.class, () -> loader.load(null));
}

// correct — clear name, AAA structure, assertAll for related assertions
@Test
void loadedInterviewHasCorrectFields() {
    // Arrange
    String interviewId = "abc123";

    // Act
    Interview interview = loader.load(interviewId);

    // Assert
    assertAll(
        () -> assertEquals("abc123", interview.getId()),
        () -> assertEquals("John Smith", interview.getCandidateName()),
        () -> assertEquals("COMPLETE", interview.getStatus())
    );
}

// correct — separate test for the error case
@Test
void loadThrowsWhenInterviewIdIsNull() {
    assertThrows(KaratApiException.class, () -> loader.load(null));
}
```