package co.elastic.apm.configuration;

import co.elastic.apm.aspect.SpanAspect;
import co.elastic.apm.aspect.TransactionAspect;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class ApmAnnotationsConfiguration {
    @Bean
    public TransactionAspect transactionAspect() {
        return new TransactionAspect();
    }
    
    @Bean
    public SpanAspect spanAspect() {
        return new SpanAspect();
    }
}
