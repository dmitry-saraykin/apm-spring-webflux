package co.elastic.apm.filter;

import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Transaction;

import org.springframework.http.HttpMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

public class ElasticApmFilter implements WebFilter {
    public static final String TRANSACTION_ATTRIBUTE = "ELASTIC_APM_TRANSACTION";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }
        Transaction transaction = ElasticApm.startAsyncTransaction();
        transaction.setType(Transaction.TYPE_REQUEST);
        transaction.setName(exchange.getRequest().getMethod().name() + " " + exchange.getRequest().getURI().getPath());
        return chain.filter(exchange)
                .subscriberContext(context -> context.put(TRANSACTION_ATTRIBUTE, transaction))
                    .doOnSuccess(nothing -> {
                        transaction.end();
                    }).doOnError(e->{
                        ElasticApm.captureException(e);
                        transaction.end();
                    }).doOnCancel(() -> {
                         transaction.end();
                    });
    }

}
