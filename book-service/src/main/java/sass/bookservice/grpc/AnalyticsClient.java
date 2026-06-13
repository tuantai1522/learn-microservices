package sass.bookservice.grpc;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sass.analytics.contract.AnalyzeBookBorrowRequest;
import sass.analytics.contract.AnalyticsServiceGrpc;
import sass.bookservice.models.Book;

@Component
public class AnalyticsClient {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsClient.class);

    private final AnalyticsServiceGrpc.AnalyticsServiceBlockingStub blockingStub;
    private final AnalyticsProperties properties;

    @Autowired
    public AnalyticsClient(ManagedChannel analyticsManagedChannel, AnalyticsProperties properties) {
        this(AnalyticsServiceGrpc.newBlockingStub(analyticsManagedChannel), properties);
    }

    AnalyticsClient(
            AnalyticsServiceGrpc.AnalyticsServiceBlockingStub blockingStub,
            AnalyticsProperties properties) {
        this.blockingStub = blockingStub;
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
