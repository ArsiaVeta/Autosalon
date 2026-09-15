package ru.arslanova.storageservice.infrastructure.grpc;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "grpc.server", name = "enabled", havingValue = "true")
public class GrpcServerRunner implements SmartLifecycle {

    private final int port;
    private final CarAvailabilityGrpcService carAvailabilityGrpcService;
    private final TraceIdServerInterceptor traceIdServerInterceptor;
    private Server server;
    private volatile boolean running = false;

    public GrpcServerRunner(@Value("${grpc.server.port:9090}") int port,
                            CarAvailabilityGrpcService carAvailabilityGrpcService,
                            TraceIdServerInterceptor traceIdServerInterceptor) {
        this.port = port;
        this.carAvailabilityGrpcService = carAvailabilityGrpcService;
        this.traceIdServerInterceptor = traceIdServerInterceptor;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        try {
            server = NettyServerBuilder.forPort(port)
                    .addService(carAvailabilityGrpcService)
                    .intercept(traceIdServerInterceptor)
                    .build()
                    .start();
            running = true;
            log.info("gRPC server started on port {}", port);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start gRPC server on port " + port, e);
        }
    }

    @Override
    public void stop() {
        running = false;
        if (server != null) {
            log.info("Stopping gRPC server on port {}", port);
            server.shutdown();
            server = null;
        }
    }

    @PreDestroy
    public void destroy() {
        stop();
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
