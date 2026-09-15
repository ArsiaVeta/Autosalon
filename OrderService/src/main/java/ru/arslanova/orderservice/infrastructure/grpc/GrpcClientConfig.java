package ru.arslanova.orderservice.infrastructure.grpc;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.arslanova.grpc.car.CarAvailabilityServiceGrpc;

@Slf4j
@Configuration
public class GrpcClientConfig {

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel storageChannel(
            @Value("${grpc.client.storage.host:localhost}") String host,
            @Value("${grpc.client.storage.port:9090}") int port,
            TraceIdClientInterceptor traceIdClientInterceptor) {
        log.info("Configuring gRPC channel to StorageService at {}:{}", host, port);
        return NettyChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .intercept(traceIdClientInterceptor)
                .build();
    }

    @Bean
    public CarAvailabilityServiceGrpc.CarAvailabilityServiceBlockingStub carAvailabilityStub(
            ManagedChannel storageChannel) {
        return CarAvailabilityServiceGrpc.newBlockingStub(storageChannel);
    }
}