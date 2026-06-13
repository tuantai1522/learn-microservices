# Book Borrow Analytics gRPC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a new `analytics-service` module and make `book-service` call it over gRPC after a successful borrow operation while keeping borrow successful if analytics fails.

**Architecture:** `book-service` remains a REST + JPA application and becomes a gRPC client for a single downstream analytics RPC. `analytics-service` is a small Spring Boot gRPC server that accepts a borrow event payload, writes a log entry, and returns an acknowledgement without owning business state.

**Tech Stack:** Java 26, Spring Boot 4, Maven, Protocol Buffers, gRPC Java, PostgreSQL for `book-service`

---

## File Structure

### New files

- `analytics-service/pom.xml`
- `analytics-service/src/main/java/sass/analyticsservice/AnalyticsServiceApplication.java`
- `analytics-service/src/main/java/sass/analyticsservice/grpc/AnalyticsGrpcService.java`
- `analytics-service/src/main/resources/application.properties`
- `analytics-service/src/main/proto/analytics.proto`
- `book-service/src/main/proto/analytics.proto`
- `book-service/src/main/java/sass/bookservice/grpc/AnalyticsProperties.java`
- `book-service/src/main/java/sass/bookservice/grpc/AnalyticsChannelFactory.java`
- `book-service/src/main/java/sass/bookservice/grpc/AnalyticsClient.java`

### Modified files

- `book-service/pom.xml`
- `book-service/src/main/resources/application.properties`
- `book-service/src/main/java/sass/bookservice/services/BookService.java`

### Responsibility map

- `analytics.proto`: shared wire contract for the borrow analytics RPC
- `AnalyticsGrpcService`: gRPC server implementation that logs incoming borrow events
- `AnalyticsProperties`: binds host, port, and timeout settings in `book-service`
- `AnalyticsChannelFactory`: creates and owns the gRPC managed channel
- `AnalyticsClient`: maps book data to the generated gRPC request and handles downstream exceptions
- `BookService`: triggers analytics after the borrow state has been saved

### Task 1: Add gRPC and protobuf build support to `book-service`

**Files:**
- Modify: `book-service/pom.xml`

- [x] **Step 1: Add protobuf and gRPC version properties**

Insert under `<properties>`:

```xml
<java.version>26</java.version>
<grpc.version>1.76.0</grpc.version>
<protobuf.version>4.32.0</protobuf.version>
<protobuf.plugin.version>0.6.1</protobuf.plugin.version>
```

- [x] **Step 2: Add runtime dependencies for the gRPC client**

Insert under `<dependencies>`:

```xml
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty-shaded</artifactId>
    <version>${grpc.version}</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-protobuf</artifactId>
    <version>${grpc.version}</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-stub</artifactId>
    <version>${grpc.version}</version>
</dependency>
<dependency>
    <groupId>javax.annotation</groupId>
    <artifactId>javax.annotation-api</artifactId>
    <version>1.3.2</version>
</dependency>
```

- [x] **Step 3: Add protobuf code generation plugins**

Replace the `<build>` section with:

```xml
<build>
    <extensions>
        <extension>
            <groupId>kr.motd.maven</groupId>
            <artifactId>os-maven-plugin</artifactId>
            <version>1.7.1</version>
        </extension>
    </extensions>
    <plugins>
        <plugin>
            <groupId>org.xolstice.maven.plugins</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <version>${protobuf.plugin.version}</version>
            <configuration>
                <protocArtifact>com.google.protobuf:protoc:${protobuf.version}:exe:${os.detected.classifier}</protocArtifact>
                <pluginId>grpc-java</pluginId>
                <pluginArtifact>io.grpc:protoc-gen-grpc-java:${grpc.version}:exe:${os.detected.classifier}</pluginArtifact>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                        <goal>compile-custom</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

- [x] **Step 4: Verify the `book-service` build file is structurally ready**

Run: `mvn -q -DskipTests compile` from `book-service`  
Expected: Maven reaches dependency resolution and protobuf codegen setup without POM validation errors.

- [x] **Step 5: Commit**

```bash
git add book-service/pom.xml
git commit -m "build: add grpc client support to book-service"
```

### Task 2: Define the shared borrow analytics protobuf contract

**Files:**
- Create: `book-service/src/main/proto/analytics.proto`
- Create: `analytics-service/src/main/proto/analytics.proto`

- [x] **Step 1: Create the shared contract in `book-service`**

Create `book-service/src/main/proto/analytics.proto`:

```proto
syntax = "proto3";

option java_multiple_files = true;
option java_package = "sass.analytics.contract";
option java_outer_classname = "AnalyticsProto";

package analytics;

service AnalyticsService {
  rpc AnalyzeBookBorrow (AnalyzeBookBorrowRequest) returns (AnalyzeBookBorrowResponse);
}

message AnalyzeBookBorrowRequest {
  string book_id = 1;
  string book_name = 2;
  string description = 3;
  string status = 4;
  string borrowed_at = 5;
}

message AnalyzeBookBorrowResponse {
  bool accepted = 1;
  string message = 2;
}
```

- [x] **Step 2: Copy the same contract into `analytics-service`**

Create `analytics-service/src/main/proto/analytics.proto` with the exact same content so both modules generate the same Java contract classes during this first iteration.

- [x] **Step 3: Verify the contract shape**

Run: `mvn -q -DskipTests compile` from `book-service`  
Expected: generated classes appear under `target/generated-sources/protobuf`.

- [x] **Step 4: Commit**

```bash
git add book-service/src/main/proto/analytics.proto analytics-service/src/main/proto/analytics.proto
git commit -m "feat: define analytics grpc contract"
```

### Task 3: Scaffold the new `analytics-service` module

**Files:**
- Create: `analytics-service/pom.xml`
- Create: `analytics-service/src/main/java/sass/analyticsservice/AnalyticsServiceApplication.java`
- Create: `analytics-service/src/main/resources/application.properties`

- [x] **Step 1: Create the Maven build**

Create `analytics-service/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.6</version>
        <relativePath/>
    </parent>
    <groupId>sass</groupId>
    <artifactId>analytics-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>analytics-service</name>
    <description>analytics-service</description>
    <properties>
        <java.version>26</java.version>
        <grpc.version>1.76.0</grpc.version>
        <protobuf.version>4.32.0</protobuf.version>
        <protobuf.plugin.version>0.6.1</protobuf.plugin.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-netty-shaded</artifactId>
            <version>${grpc.version}</version>
        </dependency>
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-protobuf</artifactId>
            <version>${grpc.version}</version>
        </dependency>
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-stub</artifactId>
            <version>${grpc.version}</version>
        </dependency>
        <dependency>
            <groupId>javax.annotation</groupId>
            <artifactId>javax.annotation-api</artifactId>
            <version>1.3.2</version>
        </dependency>
    </dependencies>
    <build>
        <extensions>
            <extension>
                <groupId>kr.motd.maven</groupId>
                <artifactId>os-maven-plugin</artifactId>
                <version>1.7.1</version>
            </extension>
        </extensions>
        <plugins>
            <plugin>
                <groupId>org.xolstice.maven.plugins</groupId>
                <artifactId>protobuf-maven-plugin</artifactId>
                <version>${protobuf.plugin.version}</version>
                <configuration>
                    <protocArtifact>com.google.protobuf:protoc:${protobuf.version}:exe:${os.detected.classifier}</protocArtifact>
                    <pluginId>grpc-java</pluginId>
                    <pluginArtifact>io.grpc:protoc-gen-grpc-java:${grpc.version}:exe:${os.detected.classifier}</pluginArtifact>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>compile</goal>
                            <goal>compile-custom</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [x] **Step 2: Create the application entrypoint**

Create `analytics-service/src/main/java/sass/analyticsservice/AnalyticsServiceApplication.java`:

```java
package sass.analyticsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AnalyticsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
```

- [x] **Step 3: Add minimal application configuration**

Create `analytics-service/src/main/resources/application.properties`:

```properties
spring.application.name=analytics-service
server.port=8082
grpc.server.port=9091
logging.level.sass.analyticsservice=INFO
```

- [x] **Step 4: Verify the new module boots structurally**

Run: `mvn -q -DskipTests compile` from `analytics-service`  
Expected: project compiles and generates protobuf sources successfully.

- [x] **Step 5: Commit**

```bash
git add analytics-service/pom.xml analytics-service/src/main/java/sass/analyticsservice/AnalyticsServiceApplication.java analytics-service/src/main/resources/application.properties
git commit -m "feat: scaffold analytics-service module"
```

### Task 4: Implement the gRPC server in `analytics-service`

**Files:**
- Create: `analytics-service/src/main/java/sass/analyticsservice/grpc/AnalyticsGrpcService.java`

- [x] **Step 1: Implement the service class**

Create `analytics-service/src/main/java/sass/analyticsservice/grpc/AnalyticsGrpcService.java`:

```java
package sass.analyticsservice.grpc;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import sass.analytics.contract.AnalyzeBookBorrowRequest;
import sass.analytics.contract.AnalyzeBookBorrowResponse;
import sass.analytics.contract.AnalyticsServiceGrpc;

@Component
public class AnalyticsGrpcService extends AnalyticsServiceGrpc.AnalyticsServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsGrpcService.class);

    @Override
    public void analyzeBookBorrow(
            AnalyzeBookBorrowRequest request,
            StreamObserver<AnalyzeBookBorrowResponse> responseObserver) {

        log.info(
                "Received borrow analytics event: bookId={}, bookName={}, status={}, borrowedAt={}",
                request.getBookId(),
                request.getBookName(),
                request.getStatus(),
                request.getBorrowedAt());

        AnalyzeBookBorrowResponse response = AnalyzeBookBorrowResponse.newBuilder()
                .setAccepted(true)
                .setMessage("analytics event accepted")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
```

- [x] **Step 2: Add a simple gRPC server bootstrap decision**

If plain gRPC server auto-configuration is not present, add a small manual bootstrap bean in the same module instead of introducing another framework. The implementation should create and start a server on `${grpc.server.port}` and register `AnalyticsGrpcService`.

- [x] **Step 3: Verify server compilation**

Run: `mvn -q -DskipTests compile` from `analytics-service`  
Expected: the generated `AnalyticsServiceGrpc` base class resolves and the module compiles.

- [x] **Step 4: Commit**

```bash
git add analytics-service/src/main/java/sass/analyticsservice/grpc/AnalyticsGrpcService.java
git commit -m "feat: add analytics grpc server"
```

### Task 5: Add gRPC client configuration to `book-service`

**Files:**
- Create: `book-service/src/main/java/sass/bookservice/grpc/AnalyticsProperties.java`
- Create: `book-service/src/main/java/sass/bookservice/grpc/AnalyticsChannelFactory.java`
- Modify: `book-service/src/main/resources/application.properties`

- [x] **Step 1: Create configuration properties**

Create `book-service/src/main/java/sass/bookservice/grpc/AnalyticsProperties.java`:

```java
package sass.bookservice.grpc;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "analytics.grpc")
public record AnalyticsProperties(String host, int port, long timeoutMillis) {
}
```

- [x] **Step 2: Create a managed channel factory**

Create `book-service/src/main/java/sass/bookservice/grpc/AnalyticsChannelFactory.java`:

```java
package sass.bookservice.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties(AnalyticsProperties.class)
public class AnalyticsChannelFactory {
    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel analyticsManagedChannel(AnalyticsProperties properties) {
        return ManagedChannelBuilder
                .forAddress(properties.host(), properties.port())
                .usePlaintext()
                .build();
    }
}
```

- [x] **Step 3: Add analytics client settings**

Append to `book-service/src/main/resources/application.properties`:

```properties
analytics.grpc.host=localhost
analytics.grpc.port=9091
analytics.grpc.timeout-millis=2000
```

- [x] **Step 4: Verify property binding setup**

Run: `mvn -q -DskipTests compile` from `book-service`  
Expected: Spring configuration classes compile and bind cleanly.

- [x] **Step 5: Commit**

```bash
git add book-service/src/main/java/sass/bookservice/grpc/AnalyticsProperties.java book-service/src/main/java/sass/bookservice/grpc/AnalyticsChannelFactory.java book-service/src/main/resources/application.properties
git commit -m "feat: configure analytics grpc client"
```

### Task 6: Implement the analytics client in `book-service`

**Files:**
- Create: `book-service/src/main/java/sass/bookservice/grpc/AnalyticsClient.java`

- [x] **Step 1: Create the client adapter**

Create `book-service/src/main/java/sass/bookservice/grpc/AnalyticsClient.java`:

```java
package sass.bookservice.grpc;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import sass.analytics.contract.AnalyzeBookBorrowRequest;
import sass.analytics.contract.AnalyticsServiceGrpc;
import sass.bookservice.models.Book;

@Component
public class AnalyticsClient {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsClient.class);

    private final AnalyticsServiceGrpc.AnalyticsServiceBlockingStub blockingStub;
    private final AnalyticsProperties properties;

    public AnalyticsClient(ManagedChannel analyticsManagedChannel, AnalyticsProperties properties) {
        this.blockingStub = AnalyticsServiceGrpc.newBlockingStub(analyticsManagedChannel);
        this.properties = properties;
    }

    public void analyzeBorrow(Book book) {
        AnalyzeBookBorrowRequest request = AnalyzeBookBorrowRequest.newBuilder()
                .setBookId(book.getId().toString())
                .setBookName(book.getName())
                .setDescription(book.getDescription() == null ? "" : book.getDescription())
                .setStatus(book.getBookStatus().name())
                .setBorrowedAt(Instant.now().toString())
                .build();

        try {
            blockingStub
                    .withDeadlineAfter(properties.timeoutMillis(), TimeUnit.MILLISECONDS)
                    .analyzeBookBorrow(request);
        } catch (StatusRuntimeException exception) {
            log.warn("Analytics gRPC call failed for borrowed book {}", book.getId(), exception);
        }
    }
}
```

- [x] **Step 2: Keep the client boundary narrow**

Do not return the analytics response to the controller. The adapter is fire-and-observe only for this feature.

- [x] **Step 3: Verify client compilation**

Run: `mvn -q -DskipTests compile` from `book-service`  
Expected: generated gRPC types resolve and the client compiles.

- [x] **Step 4: Commit**

```bash
git add book-service/src/main/java/sass/bookservice/grpc/AnalyticsClient.java
git commit -m "feat: add analytics grpc client adapter"
```

### Task 7: Trigger analytics after a successful borrow

**Files:**
- Modify: `book-service/src/main/java/sass/bookservice/services/BookService.java`

- [x] **Step 1: Inject the analytics client**

Update the constructor and fields in `BookService`:

```java
private final BookRepository bookRepository;
private final AnalyticsClient analyticsClient;

public BookService(BookRepository bookRepository, AnalyticsClient analyticsClient) {
    this.bookRepository = bookRepository;
    this.analyticsClient = analyticsClient;
}
```

- [x] **Step 2: Call analytics after persistence succeeds**

Update `borrowBook`:

```java
public BookResponseDto borrowBook(UUID id) {
    Book book = bookRepository.findById(id).orElseThrow(
            () -> new BookNotFoundException("Book not found with ID: " + id));

    book.borrowBook();
    Book savedBook = bookRepository.save(book);

    analyticsClient.analyzeBorrow(savedBook);

    return BookMapper.toDto(savedBook);
    
}
```

- [x] **Step 3: Preserve business priority**

Do not wrap the `bookRepository.save` call inside analytics error handling. Only downstream analytics failures should be tolerated.

- [x] **Step 4: Verify the service compiles**

Run: `mvn -q -DskipTests compile` from `book-service`  
Expected: `BookService` compiles with the new dependency and borrow flow remains valid.

- [x] **Step 5: Commit**

```bash
git add book-service/src/main/java/sass/bookservice/services/BookService.java
git commit -m "feat: call analytics service after borrow"
```

### Task 8: Run the two-service local demo and document the workflow

**Files:**
- Modify: `book-service/src/main/resources/application.properties` if port conflicts appear
- Modify: `analytics-service/src/main/resources/application.properties` if port conflicts appear

- [x] **Step 1: Start `analytics-service`**

Run: `mvn spring-boot:run` from `analytics-service`  
Expected: service starts and listens on `grpc.server.port=9091`.

- [x] **Step 2: Start `book-service`**

Run: `mvn spring-boot:run` from `book-service`  
Expected: REST service starts and connects on demand to the analytics host and port.

- [x] **Step 3: Trigger the borrow flow**

Call:

```bash
curl http://localhost:8080/books/borrow/<book-id>
```

Expected:
- `book-service` returns the borrowed book payload
- `analytics-service` logs the borrow event receipt

- [x] **Step 4: Verify failure tolerance**

Stop `analytics-service`, then call the same borrow endpoint with another available book.  
Expected:
- `book-service` still returns success for the borrow
- `book-service` writes a warning for the failed gRPC call

- [x] **Step 5: Commit**

```bash
git add book-service/src/main/resources/application.properties analytics-service/src/main/resources/application.properties
git commit -m "docs: validate local grpc borrow analytics flow"
```

## Self-Review

- Spec coverage: the plan creates `analytics-service`, defines the gRPC contract, wires the downstream call after borrow, preserves borrow success on analytics failure, and includes local demo steps.
- Placeholder scan: no `TBD`, `TODO`, or deferred implementation notes remain in the task steps.
- Type consistency: all tasks use `AnalyticsService`, `AnalyzeBookBorrow`, `AnalyticsClient`, `AnalyticsProperties`, and the same property names consistently.

Plan complete and saved to `docs/superpowers/plans/2026-06-12-book-borrow-analytics-grpc-implementation-plan.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
