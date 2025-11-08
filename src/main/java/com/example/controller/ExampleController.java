package com.example.controller;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.Enumeration;

@RestController
@RequestMapping("/v1")
@Slf4j
public class ExampleController {

    @PostMapping("/receive")
    public ResponseEntity<?> receiveRequest(@RequestBody Object body, HttpServletRequest request) {
        // 1. Log del body recibido
        log.info("Body recibido: {}", body);

        // 2. Obtener y mostrar todos los headers del request
        log.info("=== Headers recibidos ===");
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                log.info("{}: {}", headerName, headerValue);
            }
        }
        // 3. Crear la entidad HTTP que será enviada
        HttpEntity<Object> entity = new HttpEntity<>(body);

        // 4. Enviar al otro microservicio
        String url = "http://localhost:7690/v1/receive"; // <-- ajusta el endpoint destino si es necesario
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response;

        try {
            response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            log.info("Respuesta del microservicio destino: {}", response.getBody());
        } catch (Exception e) {
            log.error("Error al enviar la solicitud al microservicio destino: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al enviar al microservicio destino");
        }

        // 5. Devolver respuesta al cliente original
        return ResponseEntity.ok("Evento procesado y enviado correctamente");
    }
}