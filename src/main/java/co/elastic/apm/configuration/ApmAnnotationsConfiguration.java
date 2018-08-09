package co.elastic.apm.configuration;

import co.elastic.apm.aspect.SpanAspect;
import co.elastic.apm.aspect.TransactionAspect;
import co.elastic.apm.filter.ElasticApmFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.WebFilter;

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
    
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    @Bean
    public WebFilter elasticApmFilter() {
        return new ElasticApmFilter();
    }
}
