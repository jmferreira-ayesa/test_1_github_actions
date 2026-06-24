package com.example.demo.controller;

import com.example.demo.model.Item;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CRUD mínimo en memoria. El objetivo es tener un endpoint realista
 * (con GET de colección, GET por id y POST) que un frontend Angular/Vue
 * pueda consumir con peticiones típicas (listado en tabla, alta de
 * registro). Cuando conectes Aurora, esta clase pasaría a ser un
 * @Service que delega en un @Repository de Spring Data JPA: la firma
 * de los endpoints REST no cambiaría.
 */
@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

    // CopyOnWriteArrayList: válido para un ejemplo en memoria con poca
    // concurrencia de escritura. En producción esto sería la base de datos.
    private final List<Item> items = new CopyOnWriteArrayList<>(List.of(
            new Item(1L, "Teclado mecánico", new BigDecimal("89.90")),
            new Item(2L, "Monitor 27 pulgadas", new BigDecimal("249.00")),
            new Item(3L, "Webcam HD", new BigDecimal("59.50"))
    ));

    private final AtomicLong nextId = new AtomicLong(4);

    @GetMapping
    public List<Item> findAll() {
        return items;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> findById(@PathVariable Long id) {
        return items.stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Item> create(@RequestBody Map<String, Object> body) {
        Item nuevo = new Item(
                nextId.getAndIncrement(),
                (String) body.get("name"),
                new BigDecimal(body.get("price").toString())
        );
        items.add(nuevo);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }
}
