package ru.arslanova.orderservice.infrastructure.grpc;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Slf4j
@Component
public class TraceIdClientInterceptor implements ClientInterceptor {
    public static final String TRACE_HEADER = "x-trace-id";
    public static final String MDC_KEY = "traceId";

    public static final Metadata.Key<String> TRACE_KEY =
            Metadata.Key.of(TRACE_HEADER, Metadata.ASCII_STRING_MARSHALLER);


    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next){

        String current = MDC.get(MDC_KEY);
        String traceId = (current == null || current.isBlank())
                ? UUID.randomUUID().toString() : current;

        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> respTListener, Metadata headers) {
                headers.put(TRACE_KEY, traceId);
                log.debug("gRPC outgoing {} traceId={}", method.getFullMethodName(), traceId);
                super.start(respTListener, headers);
            }
        };
    }
}
