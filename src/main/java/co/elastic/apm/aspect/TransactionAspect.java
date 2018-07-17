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
public class TransactionAspect {
    public static final ThreadLocal<Transaction> CURRENT_TRANSACTION = new ThreadLocal<Transaction>();

    @Around("@annotation(co.elastic.apm.annotation.Transaction) && execution(public * *.*(..))")
    public Object trace(ProceedingJoinPoint jp) throws Throwable {
        Transaction transaction = ElasticApm.startAsyncTransaction();
        CURRENT_TRANSACTION.set(transaction);
        MethodSignature methodSignature = (MethodSignature) jp.getSignature();
        Class returnType = methodSignature.getReturnType();
        co.elastic.apm.annotation.Transaction annotation = AnnotationUtils.findAnnotation(jp.getTarget().getClass().getMethod(methodSignature.getName(), methodSignature.getParameterTypes()), co.elastic.apm.annotation.Transaction.class);
        if (!annotation.name().isEmpty()) {
            transaction.setName(annotation.name());
        } else {
            transaction.setName(methodSignature.getDeclaringType().getSimpleName() + "#" + methodSignature.getName());
        }
        if (!annotation.type().isEmpty()) {
            transaction.setType(annotation.type());
        } else {
            transaction.setType(Transaction.TYPE_REQUEST);
        }
        if (Mono.class.isAssignableFrom(returnType)) {
            try {
                Mono<Object> result = (Mono<Object>) jp.proceed();
                return result.doOnSuccess(nothing -> {
                    transaction.end();
                }).doOnError(e->{
                    ElasticApm.captureException(e);
                    transaction.end();
                }).doOnCancel(() -> {
                     transaction.end();
                });
            } catch (Exception e) {
                ElasticApm.captureException(e);
                transaction.end();
                throw e;
            } finally {
                CURRENT_TRANSACTION.set(null);
            }
        } else if (Flux.class.isAssignableFrom(returnType)) {
            try {
                Flux<Object> result = (Flux<Object>) jp.proceed();
                return result.doOnComplete(() -> {
                    transaction.end();
                }).doOnError(e->{
                    ElasticApm.captureException(e);
                    transaction.end();
                }).doOnCancel(() -> {
                    transaction.end();
                });
            } catch (Exception e) {
                ElasticApm.captureException(e);
                transaction.end();
                throw e;
            } finally {
                CURRENT_TRANSACTION.set(null);
            }
        }
        try {
            return jp.proceed();
        } finally {
            transaction.end();
            CURRENT_TRANSACTION.set(null);
        }
    }
    
    public static Transaction currentTransaction() {
        Transaction transaction = CURRENT_TRANSACTION.get();
        if (transaction == null) {
            return new Transaction() {
                
                @Override
                public void setUser(String id, String email, String username) {
                }
                
                @Override
                public void setType(String type) {
                    
                }
                
                @Override
                public void setName(String name) {
                    
                }
                
                @Override
                public void end() {
                }
                
                @Override
                public Span createSpan() {
                    return new Span() {
                        
                        @Override
                        public void setType(String type) {
                            
                        }
                        
                        @Override
                        public void setName(String name) {
                            
                        }
                        
                        @Override
                        public void end() {
                            
                        }
                        
                        @Override
                        public Span createSpan() {
                            return this;
                        }
                    };
                }
                
                @Override
                public void addTag(String key, String value) {
                    
                }
            };
        }
        return transaction;
    }

    
    public static void addTag(String key, String value) {
        Transaction transaction = CURRENT_TRANSACTION.get();
        if (transaction != null) {
            transaction.addTag(key, value);
        }
    }
}
