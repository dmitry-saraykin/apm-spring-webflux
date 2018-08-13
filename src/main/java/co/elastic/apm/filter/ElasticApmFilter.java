package co.elastic.apm.filter;

import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Transaction;
import co.elastic.apm.exception.ExceptionWrapper;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Mono;

public class ElasticApmFilter implements WebFilter {
    public static final String TRANSACTION_ATTRIBUTE = "ELASTIC_APM_TRANSACTION";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }
        Transaction transaction = ElasticApm.startAsyncTransaction();
        transaction.setType(Transaction.TYPE_REQUEST);
        transaction.setName(exchange.getRequest().getMethod().name() + " " + exchange.getRequest().getURI().getPath());
        AtomicReference<String> action = new AtomicReference<>();
        return chain.filter(exchange)
                .subscriberContext(context -> { 
                    context.put(TRANSACTION_ATTRIBUTE, transaction);
                    action.set(context.getOrDefault("X-ACTION-ID", null));
                    return context;
                })
                .doOnSuccess(nothing -> {
                    transaction.end();
                }).doOnError(e->{
                    ElasticApm.captureException(new ExceptionWrapper(e, action.get(), request.getMethodValue() + " " + request.getURI()));
                    transaction.end();
                }).doOnCancel(() -> {
                     transaction.end();
                });
    }

}
