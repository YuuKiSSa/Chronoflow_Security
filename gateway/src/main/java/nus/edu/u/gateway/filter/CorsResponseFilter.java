package nus.edu.u.gateway.filter;

import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class CorsResponseFilter implements WebFilter, Ordered {

    private static final Set<String> ALLOWED_ORIGINS =
            Set.of(
                    "https://chronoflow.site",
                    "https://api.chronoflow.site",
                    "https://www.chronoflow.site",
                    "http://chronoflow.site",
                    "http://api.chronoflow.site",
                    "http://www.chronoflow.site",
                    "http://localhost:5173",
                    "http://127.0.0.1:5173");

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String origin = exchange.getRequest().getHeaders().getOrigin();
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            headers.setAccessControlAllowOrigin(origin);
            headers.setAccessControlAllowCredentials(true);
            headers.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);
        }

        return chain.filter(exchange);
    }
}
