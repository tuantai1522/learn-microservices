package sass.analyticsservice.grpc;

import java.io.IOException;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GrpcServerLifecycle {
    private final int port;
    private final AnalyticsGrpcService analyticsGrpcService;
    private Server server;
    private Thread awaitThread;

    public GrpcServerLifecycle(
            @Value("${grpc.server.port}") int port,
            AnalyticsGrpcService analyticsGrpcService) {
        this.port = port;
        this.analyticsGrpcService = analyticsGrpcService;
    }

    @PostConstruct
    void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(analyticsGrpcService)
                .build()
                .start();

        awaitThread = Thread.ofPlatform()
                .name("analytics-grpc-server")
                .start(() -> {
                    try {
                        server.awaitTermination();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                });
    }

    @PreDestroy
    void stop() {
        if (server != null) {
            server.shutdownNow();
        }
        if (awaitThread != null) {
            awaitThread.interrupt();
        }
    }
}
