package sass.analyticsservice.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.grpc.stub.StreamObserver;
import sass.analytics.contract.AnalyzeBookBorrowRequest;
import sass.analytics.contract.AnalyzeBookBorrowResponse;

class AnalyticsGrpcServiceTests {

    @Test
    void analyzeBookBorrowAcceptsBorrowEvent() {
        AnalyticsGrpcService service = new AnalyticsGrpcService();
        List<AnalyzeBookBorrowResponse> responses = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        boolean[] completed = new boolean[1];

        service.analyzeBookBorrow(
                AnalyzeBookBorrowRequest.newBuilder()
                        .setBookId("book-1")
                        .setBookName("DDD")
                        .setStatus("BORROWED")
                        .setBorrowedAt("2026-06-13T00:00:00Z")
                        .build(),
                new StreamObserver<>() {
                    @Override
                    public void onNext(AnalyzeBookBorrowResponse value) {
                        responses.add(value);
                    }

                    @Override
                    public void onError(Throwable t) {
                        errors.add(t);
                    }

                    @Override
                    public void onCompleted() {
                        completed[0] = true;
                    }
                });

        assertTrue(errors.isEmpty());
        assertEquals(1, responses.size());
        assertTrue(responses.get(0).getAccepted());
        assertEquals("analytics event accepted", responses.get(0).getMessage());
        assertTrue(completed[0]);
    }
}
