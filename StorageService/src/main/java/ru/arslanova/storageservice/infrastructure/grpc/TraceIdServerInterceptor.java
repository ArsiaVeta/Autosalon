package ru.arslanova.storageservice.infrastructure.grpc;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Slf4j
@Component
public class TraceIdServerInterceptor implements ServerInterceptor {
    public static final String TRACE_HEADER = "x-trace-id";
    public static final String MDC_KEY = "traceId";

    public static final Metadata.Key<String> TRACE_KEY =
            Metadata.Key.of(TRACE_HEADER, Metadata.ASCII_STRING_MARSHALLER);


    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String incoming = headers.get(TRACE_KEY);
        String traceId = (incoming == null || incoming.isBlank())
                ? UUID.randomUUID().toString()
                : incoming;

        ServerCall<ReqT, RespT> tracedCall =
                new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
                    @Override
                    public void sendHeaders(Metadata responseHeaders) {
                        responseHeaders.put(TRACE_KEY, traceId);
                        super.sendHeaders(responseHeaders);
                    }
                };
        ServerCall.Listener<ReqT> delegate;
        MDC.put(MDC_KEY, traceId);
        try {
            log.debug("gRPC incoming {} traceId={}",
                    call.getMethodDescriptor().getFullMethodName(), traceId);
            delegate = next.startCall(tracedCall, headers);
        } finally {
            MDC.remove(MDC_KEY);
        }

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {
            @Override
            public void onMessage(ReqT message) {
                MDC.put(MDC_KEY, traceId);
                try {
                    super.onMessage(message);
                } finally {
                    MDC.remove(MDC_KEY);
                }
            }

            @Override
            public void onHalfClose(){
                MDC.put(MDC_KEY, traceId);
                try {
                    super.onHalfClose();
                } finally {
                    MDC.remove(MDC_KEY);
                }
            }

            @Override
            public void onCancel(){
                MDC.put(MDC_KEY, traceId);
                try {
                    super.onCancel();
                } finally {
                    MDC.remove(MDC_KEY);
                }
            }

            @Override
            public void onComplete(){
                MDC.put(MDC_KEY, traceId);
                try {
                    super.onComplete();
                } finally {
                    MDC.remove(MDC_KEY);
                }
            }

            @Override
            public void onReady(){
                MDC.put(MDC_KEY, traceId);
                try {
                    super.onReady();
                } finally {
                    MDC.remove(MDC_KEY);
                }
            }
        };
    }
}
