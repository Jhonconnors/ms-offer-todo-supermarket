package com.example.config.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// @Component: Permite que Spring detecte esta clase y la use automáticamente
@Component
public class IpFilter extends OncePerRequestFilter {

    // Dependencia que contiene la lista de IPs permitidas (se cargará después)
    private final IpSecurityProperties ipProperties;

    public IpFilter(IpSecurityProperties ipProperties) {
        this.ipProperties = ipProperties;
    }

    // Este método se ejecuta CADA VEZ que alguien intenta entrar a tu API
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws IOException, ServletException {

        // Paso 1: "Identifíquese". Obtenemos la dirección IP del cliente.
        String ip = request.getRemoteAddr();

        // Paso 2: "¿Está usted en la lista?".
        // Consultamos si la IP está en la lista blanca de 'ipProperties'
        if (!ipProperties.getAllowedIps().contains(ip)) {

            // Paso 3 (Rechazo): Si NO está en la lista...
            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // Ponemos semáforo rojo (Error 403)
            response.getWriter().write("IP not allowed"); // Le decimos "No puedes pasar"
            return; // ¡IMPORTANTE! Aquí se detiene todo. Nadie entra.
        }

        // Paso 4 (Aprobado): Si la IP sí estaba en la lista...
        // filterChain.doFilter significa "Pase usted, continúe al siguiente control".
        filterChain.doFilter(request, response);
    }
}
