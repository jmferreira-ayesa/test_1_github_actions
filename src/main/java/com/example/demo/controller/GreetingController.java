package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Endpoint mínimo para comprobar, desde el frontend, que la llamada al
 * backend (y el CORS) funcionan correctamente antes de meterte con
 * lógica de negocio real.
 */
@RestController
public class GreetingController {

    @GetMapping("/api/v1/greeting")
    public Map<String, Object> greeting(@RequestParam(defaultValue = "mundo") String name) {
        return Map.of(
                "message", "¡Hola, " + name + "!",
                "timestamp", Instant.now().toString()
        );
    }
}
