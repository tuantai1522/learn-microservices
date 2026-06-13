package sass.bookservice.grpc;

import java.time.Instant;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sass.analytics.contract.AnalyzeBookBorrowRequest;
import sass.analytics.contract.AnalyzeBookBorrowResponse;
import sass.analytics.contract.AnalyticsServiceGrpc;
import sass.bookservice.models.Book;

@Component
public class AnalyticsClient {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsClient.class);

    private final AnalyticsServiceGrpc.AnalyticsServiceBlockingStub blockingStub;

    public AnalyticsClient(
            @Value("${analytics.grpc.host}") String serverAddress,
            @Value("${analytics.grpc.port}") int serverPort) {
        log.info("Connecting to Analytics Service GRPC service at {}:{}", serverAddress, serverPort);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress, serverPort)
                .usePlaintext()
                .build();
        this.blockingStub = AnalyticsServiceGrpc.newBlockingStub(channel);
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
            AnalyzeBookBorrowResponse response = blockingStub
                    .analyzeBookBorrow(request);

            log.info("Analytics gRPC call succeeded for borrowed book with message: {}", response.getMessage());
        } catch (StatusRuntimeException exception) {
            log.warn("Analytics gRPC call failed for borrowed book {} with exception {}", book.getId(), exception.getMessage());
        }
    }
}
