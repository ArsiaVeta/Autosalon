package ru.arslanova.orderservice;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import ru.arslanova.orderservice.infrastructure.grpc.TraceIdClientInterceptor;

import static ru.arslanova.orderservice.HealthAndTraceIdIntegrationTest.lastSeenTraceId;

public class TraceCapturingInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        lastSeenTraceId = headers.get(TraceIdClientInterceptor.TRACE_KEY);
        return next.startCall(call, headers);
    }
}
