package com.example.service;

import com.example.model.Precio;
import com.example.model.ProductSupermarket;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

@Service
public class LiderService {

    private final String URL = "https://super.lider.cl/orchestra/graphql/search";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public List<ProductSupermarket> searchProducts(String query) {
        // --- LOG INICIAL (Estilo Dr. Simi) ---
        System.out.println("--- CONECTANDO A API DIRECTA LIDER: " + query + " ---");
        System.out.println("URL: " + URL);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");
            // Headers obligatorios
            headers.set("x-o-bu", "LIDER-CL");
            headers.set("x-o-platform", "rweb");
            headers.set("x-o-mart", "B2C");
            headers.set("x-o-segment", "oaoh");
            headers.set("x-o-vertical", "OD");
            headers.set("x-apollo-operation-name", "Search");
            headers.set("tenant", "CHILE_GLASS");
            headers.set("origin", "https://super.lider.cl");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36");

            // --- CORRECCIÓN CRÍTICA: Usamos "... on Product" ---
            // Esto le dice a GraphQL: "Si el ítem es un Producto, dame estos campos".
            String graphqlQuery = "{"
                    + "\"query\": \"query Search($query: String, $tenant: String!, $prg: Prg!) { search(query: $query, tenant: $tenant, prg: $prg) { searchResult { itemStacks { itemsV2 { ... on Product { name priceInfo { currentPrice { price } } imageInfo { thumbnailUrl } canonicalUrl } } } } } }\","
                    + "\"variables\": {"
                    + "  \"query\": \"" + query + "\","
                    + "  \"tenant\": \"CHILE_GLASS\","
                    + "  \"prg\": \"desktop\""
                    + "}"
                    + "}";

            HttpEntity<String> entity = new HttpEntity<>(graphqlQuery, headers);
            
            // Hacemos la llamada (recibimos bytes por si viene comprimido)
            byte[] responseBytes = restTemplate.postForObject(URL, entity, byte[].class);
            
            // Descomprimir respuesta
            String responseString = decompressGzip(responseBytes);
            
            List<ProductSupermarket> resultados = parseLiderResponse(responseString);

            // --- LOG FINAL ---
            System.out.println("Productos encontrados Lider: " + resultados.size());
            return resultados;

        } catch (HttpClientErrorException e) {
            System.err.println("❌ Error Lider " + e.getStatusCode());
            String errorBody = decompressGzip(e.getResponseBodyAsByteArray());
            System.err.println("Cuerpo del error (Decodificado): " + errorBody);
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("❌ Error General Lider: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Método para descomprimir GZIP
    private String decompressGzip(byte[] compressed) {
        if (compressed == null || compressed.length == 0) return "";
        try {
            if (compressed[0] == (byte) 0x1f && compressed[1] == (byte) 0x8b) {
                try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
                     BufferedReader br = new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    return sb.toString();
                }
            } else {
                return new String(compressed, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return "Error descomprimiendo: " + e.getMessage();
        }
    }

    private List<ProductSupermarket> parseLiderResponse(String jsonResponse) {
        List<ProductSupermarket> products = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(jsonResponse);

            if (root.has("errors")) {
                System.err.println("⚠️ Lider respondió con errores GraphQL: " + root.path("errors").toString());
                return products;
            }

            JsonNode itemStacks = root.path("data").path("search").path("searchResult").path("itemStacks");

            if (itemStacks.isArray()) {
                for (JsonNode stack : itemStacks) {
                    JsonNode items = stack.path("itemsV2");
                    if (items.isArray()) {
                        for (JsonNode item : items) {
                            if (item.has("name")) {
                                String nombre = item.path("name").asText("Sin nombre");
                                String urlImg = item.path("imageInfo").path("thumbnailUrl").asText("");
                                String urlProd = item.path("canonicalUrl").asText("");
                                double precio = item.path("priceInfo").path("currentPrice").path("price").asDouble(0);

                                products.add(ProductSupermarket.builder()
                                        .nombre(nombre)
                                        .descripcion("Lider")
                                        .imagen(urlImg)
                                        .supermercadoUrl("https://www.lider.cl/supermercado/product/" + urlProd)
                                        .precioUnitario(new Precio(precio, "CLP"))
                                        .build());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parseando Lider: " + e.getMessage());
        }
        return products;
    }
}