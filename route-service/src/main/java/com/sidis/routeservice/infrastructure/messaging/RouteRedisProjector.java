package com.sidis.routeservice.infrastructure.messaging;

import com.sidis.routeservice.Route.domain.RouteReadModel;
import com.sidis.routeservice.Route.infrastructure.RouteReadRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RouteRedisProjector {

    private final RouteReadRepository routeReadRepository;

    public RouteRedisProjector(RouteReadRepository routeReadRepository) {
        this.routeReadRepository = routeReadRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.ROUTE_SYNC_QUEUE, concurrency = "1")
    public void project(RouteReadModel route) {
        if (route.version() == null) {
            throw new IllegalArgumentException("Route event must include a version");
        }

        var existing = routeReadRepository.findById(route.routeId());
        if (existing.isPresent()) {
            Long storedVersion = existing.get().version();
            if (storedVersion != null && storedVersion >= route.version()) {
                return;
            }
        }

        routeReadRepository.save(route);
    }
}
