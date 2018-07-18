package co.elastic.apm.aspect;

import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Transaction;
import co.elastic.apm.filter.ElasticApmFilter;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Aspect
public class TransactionAspect {
    @Around("@annotation(co.elastic.apm.annotation.Transaction) && execution(public * *.*(..))")
    public Object trace(ProceedingJoinPoint jp) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) jp.getSignature();
        Class returnType = methodSignature.getReturnType();
        co.elastic.apm.annotation.Transaction annotation = AnnotationUtils.findAnnotation(
                jp.getTarget().getClass().getMethod(methodSignature.getName(), methodSignature.getParameterTypes()),
                co.elastic.apm.annotation.Transaction.class);
        if (Mono.class.isAssignableFrom(returnType)) {
            Mono<Object> result = (Mono<Object>) jp.proceed();
            return Mono.subscriberContext().flatMap(context -> {
                Transaction transaction = context.getOrDefault(ElasticApmFilter.TRANSACTION_ATTRIBUTE, null);
                boolean shouldEnd = false;
                if (transaction == null) {
                    transaction = ElasticApm.startAsyncTransaction();
                    context.put(ElasticApmFilter.TRANSACTION_ATTRIBUTE, transaction);
                }
                if (!annotation.name().isEmpty()) {
                    transaction.setName(annotation.name());
                } else {
                    transaction.setName(
                            methodSignature.getDeclaringType().getSimpleName() + "#" + methodSignature.getName());
                }
                if (!annotation.type().isEmpty()) {
                    transaction.setType(annotation.type());
                } else {
                    transaction.setType(Transaction.TYPE_REQUEST);
                }
                Transaction transactionReference = transaction;
                if (shouldEnd) {
                    return result.doOnSuccess(nothing -> {
                        transactionReference.end();
                    }).doOnError(e -> {
                        ElasticApm.captureException(e);
                        transactionReference.end();
                    }).doOnCancel(() -> {
                        transactionReference.end();
                    });
                } else {
                    return result;
                }
            });
        } else if (Flux.class.isAssignableFrom(returnType)) {
            Flux<Object> result = (Flux<Object>) jp.proceed();
            return Mono.subscriberContext().flatMapMany(context -> {
                Transaction transaction = context.getOrDefault(ElasticApmFilter.TRANSACTION_ATTRIBUTE, null);
                boolean shouldEnd = false;
                if (transaction == null) {
                    transaction = ElasticApm.startAsyncTransaction();
                    context.put(ElasticApmFilter.TRANSACTION_ATTRIBUTE, transaction);
                }
                if (!annotation.name().isEmpty()) {
                    transaction.setName(annotation.name());
                } else {
                    transaction.setName(
                            methodSignature.getDeclaringType().getSimpleName() + "#" + methodSignature.getName());
                }
                if (!annotation.type().isEmpty()) {
                    transaction.setType(annotation.type());
                } else {
                    transaction.setType(Transaction.TYPE_REQUEST);
                }
                Transaction transactionReference = transaction;
                if (shouldEnd) {
                    return result.doOnComplete(() -> {
                        transactionReference.end();
                    }).doOnError(e -> {
                        ElasticApm.captureException(e);
                        transactionReference.end();
                    }).doOnCancel(() -> {
                        transactionReference.end();
                    });
                } else {
                    return result;
                }
            });
        }
        return jp.proceed();
    }
}
