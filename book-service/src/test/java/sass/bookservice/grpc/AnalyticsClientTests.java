package sass.bookservice.grpc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import sass.analytics.contract.AnalyzeBookBorrowRequest;
import sass.analytics.contract.AnalyticsServiceGrpc;
import sass.bookservice.models.Book;

@ExtendWith(MockitoExtension.class)
class AnalyticsClientTests {

    @Mock
    private AnalyticsServiceGrpc.AnalyticsServiceBlockingStub blockingStub;

    @Test
    void analyzeBorrowSwallowsGrpcFailures() {
        Book book = new Book("Clean Architecture", "A guide");
        book.setId(UUID.randomUUID());

        when(blockingStub.withDeadlineAfter(2000L, java.util.concurrent.TimeUnit.MILLISECONDS)).thenReturn(blockingStub);
        when(blockingStub.analyzeBookBorrow(any(AnalyzeBookBorrowRequest.class)))
                .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

        AnalyticsClient client = new AnalyticsClient(blockingStub, new AnalyticsProperties("localhost", 9091, 2000L));

        assertDoesNotThrow(() -> client.analyzeBorrow(book));
        verify(blockingStub).analyzeBookBorrow(any(AnalyzeBookBorrowRequest.class));
    }
}
