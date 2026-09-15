package ru.arslanova.storageservice;

import io.grpc.*;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import ru.arslanova.grpc.car.CarAvailabilityServiceGrpc;
import ru.arslanova.grpc.car.ListAvailableCarsRequest;
import ru.arslanova.storageservice.infrastructure.grpc.CarAvailabilityGrpcService;
import ru.arslanova.storageservice.infrastructure.grpc.TraceIdServerInterceptor;
import ru.arslanova.storageservice.infrastructure.repository.CarModelRepository;

import javax.crypto.spec.SecretKeySpec;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "app.kafka.topics.auto-create=false"
})
public class TraceIdServerInterceptorIntegrationTest extends AbstractPostgresIntegrationTest{

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public JwtDecoder jwtDecoder() {
            byte[] key = "test-secret-test-secret-test-secret-key".getBytes();
            return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(key, "HmacSHA256")).build();
        }
    }

    static volatile String traceIdInsideHandler;

    static volatile String traceIdInResponseHeaders;

    @Autowired
    CarAvailabilityGrpcService grpcService;

    @Autowired
    CarModelRepository repository;

    @Autowired
    TraceIdServerInterceptor traceIdServerInterceptor;

    ManagedChannel channel;

    private Server server;

    private CarAvailabilityServiceGrpc.CarAvailabilityServiceBlockingStub stub;


    @BeforeEach
    void setUp() throws Exception{
        repository.deleteAll();
        traceIdInsideHandler = null;
        traceIdInResponseHeaders = null;

        server = NettyServerBuilder.forPort(0)
                .addService(io.grpc.ServerInterceptors.intercept(
                        grpcService, new MdcCapturingInterceptor()))
                .intercept(traceIdServerInterceptor)
                .build()
                .start();

        channel = NettyChannelBuilder.forAddress("localhost", server.getPort())
                .usePlaintext()
                .intercept(new ResponseHeaderCapturingInterceptor())
                .build();

        stub = CarAvailabilityServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (channel != null) channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        if (server != null) server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        repository.deleteAll();
        MDC.clear();
    }

    @Test
    void traceIdFromMetadataIsPutIntoMdcAndEchoedBack() {
        String traceId = "trace-from-order-service-" + UUID.randomUUID();

        Metadata metadata = new Metadata();
        metadata.put(TraceIdServerInterceptor.TRACE_KEY, traceId);

        CarAvailabilityServiceGrpc.CarAvailabilityServiceBlockingStub traceStub =
                stub.withInterceptors(io.grpc.stub.MetadataUtils.newAttachHeadersInterceptor(metadata));

        traceStub.listAvailableCars(ListAvailableCarsRequest.getDefaultInstance());

        assertThat(traceIdInsideHandler)
                .as("traceId должен быть доступен в MDC во время обработки вызова")
                .isEqualTo(traceId);
        assertThat(traceIdInResponseHeaders)
                .as("traceId должен вернуться клиенту в заголовок ответа")
                .isEqualTo(traceId);
    }

    @Test
    void traceIdIsGeneratedWhenCallerDidNotSendIt() {
        stub.listAvailableCars(ListAvailableCarsRequest.getDefaultInstance());

        assertThat(traceIdInsideHandler).isNotBlank();
        assertThat(UUID.fromString(traceIdInsideHandler)).isNotNull();
        assertThat(traceIdInResponseHeaders).isEqualTo(traceIdInsideHandler);
    }

    static class MdcCapturingInterceptor implements io.grpc.ServerInterceptor {
        @Override
        public <ReqT, RespT> io.grpc.ServerCall.Listener<ReqT> interceptCall(
                io.grpc.ServerCall<ReqT, RespT> call,
                Metadata headers,
                io.grpc.ServerCallHandler<ReqT, RespT> next) {

            io.grpc.ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);
            return new ForwardingServerCallListener
                    .SimpleForwardingServerCallListener<>(delegate) {
                @Override
                public void onHalfClose() {
                    traceIdInsideHandler = MDC.get(TraceIdServerInterceptor.MDC_KEY);
                    super.onHalfClose();
                }
            };
        }
    }

    static class ResponseHeaderCapturingInterceptor implements ClientInterceptor {
        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                MethodDescriptor<ReqT, RespT> method,
                CallOptions callOptions,
                Channel next) {

            return new ForwardingClientCall.SimpleForwardingClientCall<>(
                    next.newCall(method, callOptions)) {
                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    super.start(new ForwardingClientCallListener
                            .SimpleForwardingClientCallListener<>(responseListener) {
                        @Override
                        public void onHeaders(Metadata headers) {
                            traceIdInResponseHeaders =
                                    headers.get(TraceIdServerInterceptor.TRACE_KEY);
                            super.onHeaders(headers);
                        }
                    }, headers);
                }
            };
        }
    }

}
