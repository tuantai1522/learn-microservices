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
