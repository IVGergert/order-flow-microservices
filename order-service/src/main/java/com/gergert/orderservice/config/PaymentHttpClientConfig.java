package com.gergert.orderservice.config;

import com.gergert.orderservice.client.PaymentHttpClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class PaymentHttpClientConfig {

    @Value("${payment-service.base-url}")
    private String paymentServiceBaseUrl;

    @Bean
    RestClient paymentRestClient(){
        return RestClient.builder()
                .baseUrl(paymentServiceBaseUrl)

                .requestInterceptor(((request, body, execution) -> {
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

                    if (attributes != null) {
                        HttpServletRequest servletRequest = attributes.getRequest();
                        String authHeader = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);

                        if (authHeader != null) {
                            request.getHeaders().add(HttpHeaders.AUTHORIZATION, authHeader);
                        }
                    }

                    return execution.execute(request, body);
                }))
                .build();
    }

    @Bean
    PaymentHttpClient paymentHttpClient(RestClient restClient){
        return HttpServiceProxyFactory.builder()
                .exchangeAdapter(RestClientAdapter.create(restClient))
                .build()
                .createClient(PaymentHttpClient.class);
    }
}
