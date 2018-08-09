package co.elastic.apm.aspect;

import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Span;
import co.elastic.apm.api.Transaction;
import co.elastic.apm.exception.ExceptionWrapper;
import co.elastic.apm.filter.ElasticApmFilter;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Aspect
public class SpanAspect {

    @Around("@annotation(co.elastic.apm.annotation.Span) && execution(public * *.*(..))")
    public Object trace(ProceedingJoinPoint jp) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) jp.getSignature();
        Class returnType = methodSignature.getReturnType();
        co.elastic.apm.annotation.Span annotation = AnnotationUtils.findAnnotation(
                jp.getTarget().getClass().getMethod(methodSignature.getName(), methodSignature.getParameterTypes()),
                co.elastic.apm.annotation.Span.class);

        AtomicReference<String> action = new AtomicReference<>();
        if (Mono.class.isAssignableFrom(returnType)) {
            Mono<Object> result = (Mono<Object>) jp.proceed();
            return Mono.subscriberContext().flatMap(context -> {
                action.set(context.getOrDefault("X-ACTION-ID", null));
                Transaction transaction = context.getOrDefault(ElasticApmFilter.TRANSACTION_ATTRIBUTE, null);
                if (transaction == null) {
                    return result;
                }

                Span span = transaction.createSpan();
                if (!annotation.name().isEmpty()) {
                    span.setName(annotation.name());
                } else {
                    span.setName(jp.getTarget().getClass().getSimpleName() + "#" + methodSignature.getName());
                }
                if (!annotation.type().isEmpty()) {
                    span.setType(annotation.type());
                } else {
                    span.setType("ext");
                }
                return result.doOnSuccess(nothing -> {
                        span.end();
                    }).doOnError(e -> {
                        if (action.get() != null) {
                            ElasticApm.captureException(new ExceptionWrapper(e, action.get()));
                        } else {
                            ElasticApm.captureException(e);
                        }
                        span.end();
                    }).doOnCancel(() -> {
                        span.end();
                    });
            });
        } else if (Flux.class.isAssignableFrom(returnType)) {
            Flux<Object> result = (Flux<Object>) jp.proceed();
            return Mono.subscriberContext().flatMapMany(context -> {
                action.set(context.getOrDefault("X-ACTION-ID", null));
                Transaction transaction = context.getOrDefault(ElasticApmFilter.TRANSACTION_ATTRIBUTE, null);
                if (transaction == null) {
                    return result;
                }

                Span span = transaction.createSpan();
                if (!annotation.name().isEmpty()) {
                    span.setName(annotation.name());
                } else {
                    span.setName(jp.getTarget().getClass().getSimpleName() + "#" + methodSignature.getName());
                }
                if (!annotation.type().isEmpty()) {
                    span.setType(annotation.type());
                } else {
                    span.setType("ext");
                }
                return result.doOnComplete(() -> {
                        span.end();
                    }).doOnError(e -> {
                        if (action.get() != null) {
                            ElasticApm.captureException(new ExceptionWrapper(e, action.get()));
                        } else {
                            ElasticApm.captureException(e);
                        }
                        span.end();
                    }).doOnCancel(() -> {
                        span.end();
                    });
            });
        }
        return jp.proceed();
    }
}
