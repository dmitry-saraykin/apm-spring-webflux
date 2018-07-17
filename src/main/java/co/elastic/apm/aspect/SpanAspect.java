package co.elastic.apm.aspect;

import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Span;
import co.elastic.apm.api.Transaction;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Aspect
public class SpanAspect {

    @Around("@annotation(co.elastic.apm.annotation.Span) && execution(public * *.*(..))")
    public Object trace(ProceedingJoinPoint jp) throws Throwable {
        Transaction transaction = TransactionAspect.CURRENT_TRANSACTION.get();
        if (transaction == null) {
            return jp.proceed();
        }
        MethodSignature methodSignature = (MethodSignature) jp.getSignature();
        Span span = transaction.createSpan();
        Class returnType = methodSignature.getReturnType();
        co.elastic.apm.annotation.Span annotation = AnnotationUtils.findAnnotation(jp.getTarget().getClass().getMethod(methodSignature.getName(), methodSignature.getParameterTypes()), co.elastic.apm.annotation.Span.class);
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
        if (Mono.class.isAssignableFrom(returnType)) {
            try {
                Mono<Object> result = (Mono<Object>) jp.proceed();
                 return result.doOnSuccess(nothing -> {
                    span.end();
                    TransactionAspect.CURRENT_TRANSACTION.set(transaction);
                }).doOnError(e->{
                    ElasticApm.captureException(e);
                    span.end();
                    TransactionAspect.CURRENT_TRANSACTION.set(transaction);
                }).doOnCancel(() -> {
                    TransactionAspect.CURRENT_TRANSACTION.set(transaction);
                    span.end();
                });
            } catch (Exception e) {
                ElasticApm.captureException(e);
                span.end();
                throw e;
            }
        } else if (Flux.class.isAssignableFrom(returnType)) {
            try {
                Flux<Object> result = (Flux<Object>) jp.proceed();
                return result.doOnComplete(() -> {
                    span.end();
                    TransactionAspect.CURRENT_TRANSACTION.set(transaction);
                }).doOnError(e->{
                    ElasticApm.captureException(e);
                    span.end();
                    TransactionAspect.CURRENT_TRANSACTION.set(transaction);
                }).doOnCancel(() -> {
                    span.end();
                    TransactionAspect.CURRENT_TRANSACTION.set(transaction);
                });
            } catch (Exception e) {
                ElasticApm.captureException(e);
                span.end();
                TransactionAspect.CURRENT_TRANSACTION.set(transaction);
                throw e;
            }
        }
        try {
            return jp.proceed();
        } finally {
            span.end();
        }
    }
}
