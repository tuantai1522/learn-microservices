package sass.bookservice.grpc;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "analytics.grpc")
public record AnalyticsProperties(String host, int port, long timeoutMillis) {
}
