package co.elastic.apm.utils;

import co.elastic.apm.api.Transaction;
import co.elastic.apm.filter.ElasticApmFilter;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;

public class ApmService {
    private static final ThreadLocal<Transaction> CURRENT_TRANSACTION = new ThreadLocal<>();

    public static <T> Mono<T> addTags(Map<String, String> tags, Mono<T> result) {
        return Mono.subscriberContext().flatMap(context -> {
            Transaction transaction = context.getOrDefault(ElasticApmFilter.TRANSACTION_ATTRIBUTE, null);
            if (transaction != null) {
                for (Entry<String, String> entry : tags.entrySet()) {
                    transaction.addTag(entry.getKey(), entry.getValue());
                }
            }
            return result;
        });
    }
    
    public static <T> Mono<T> addTag(String key, String value, Mono<T> result) {
        return addTags(Collections.singletonMap(key, value), result);
    }
    
    public static <T> Mono<T> doSync(Supplier<T> action) {
        return Mono.subscriberContext().map(context -> {
            Transaction transaction = context.getOrDefault(ElasticApmFilter.TRANSACTION_ATTRIBUTE, NoopTransaction.INSTANCE);
            CURRENT_TRANSACTION.set(transaction);
            try {
                return action.get();
            } finally {
                CURRENT_TRANSACTION.set(null);
            }
        });
    }
    
    public static Transaction currentTransaction() {
        return CURRENT_TRANSACTION.get();
    }
}
