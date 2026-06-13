package sass.bookservice.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
