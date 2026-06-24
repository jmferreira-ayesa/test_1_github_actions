package com.example.demo.model;

import java.math.BigDecimal;

/**
 * Record inmutable de ejemplo. En una app real este sería tu entidad JPA
 * (con @Entity) mapeada contra la tabla en Aurora/RDS, pero aquí lo
 * mantenemos en memoria para centrarnos en la parte de despliegue en ECS.
 */
public record Item(Long id, String name, BigDecimal price) {
}
