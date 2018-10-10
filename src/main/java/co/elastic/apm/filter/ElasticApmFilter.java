package co.elastic.apm.filter;

import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Transaction;
import co.elastic.apm.exception.ExceptionWrapper;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Mono;

public class ElasticApmFilter implements WebFilter {
    public static final String TRANSACTION_ATTRIBUTE = "ELASTIC_APM_TRANSACTION";
    private final Set<Class<? extends Exception>> ignoreErrors;
    
    public ElasticApmFilter() {
        ignoreErrors = new HashSet<Class<? extends Exception>>(Arrays.asList(ClosedChannelException.class));
    }
    
    public ElasticApmFilter(Class<? extends Exception>... exceptionsToIgnore) {
        ignoreErrors = new HashSet<Class<? extends Exception>>(Arrays.asList(exceptionsToIgnore));
    }

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
                    if (!ignoreErrors.contains(e.getClass())) {
                        String url = request.getURI().getPath();
                        if (!StringUtils.isEmpty(request.getURI().getQuery())) {
                            url += "?" + request.getURI().getQuery();
                        }
                        if ("/".equals(url) || "/favicon.ico".equals(url)) {
                            return;
                        }
                        ElasticApm.captureException(new ExceptionWrapper(e, action.get(), request.getMethodValue() + " " + url));
                    }
                    transaction.end();
                }).doOnCancel(() -> {
                     transaction.end();
                });
    }

}
