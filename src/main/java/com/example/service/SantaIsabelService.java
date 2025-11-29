package com.example.service;

import com.example.model.Precio;
import com.example.model.ProductSupermarket;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class SantaIsabelService {

    private final String BASE_URL_TEMPLATE = "https://sm-web-api.ecomm.cencosud.com/catalog/api/v2/pedrofontova/search/%s?page=1";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public List<ProductSupermarket> searchProducts(String query) {
        // --- LOG INICIAL ---
        System.out.println("--- CONECTANDO A API DIRECTA SANTA ISABEL: " + query + " ---");
        
        try {
            String url = String.format(BASE_URL_TEMPLATE, query);
            System.out.println("URL: " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", "WlVnnB7c1BblmgUPOfg");
            headers.set("x-e-commerce", "santa-isabel");
            headers.set("User-Agent", "Mozilla/5.0");

            String requestBody = "{\"selectedFacets\": [{\"key\": \"trade-policy\",\"value\": \"1\"}],\"orderBy\": \"OrderByScoreDESC\"}";

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(url, entity, String.class);
            
            List<ProductSupermarket> resultados = parseSantaResponse(response);
            
            // --- LOG FINAL ---
            System.out.println("Productos encontrados Santa Isabel: " + resultados.size());
            return resultados;

        } catch (Exception e) {
            System.err.println("❌ Error Santa Isabel: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<ProductSupermarket> parseSantaResponse(String jsonResponse) {
        List<ProductSupermarket> products = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(jsonResponse);
            JsonNode items = root.path("products");

            if (items.isArray()) {
                for (JsonNode item : items) {
                    String nombre = item.path("productName").asText("Producto Santa Isabel");
                    String linkText = item.path("linkText").asText();
                    String urlProducto = "https://www.santaisabel.cl/" + linkText + "/p";

                    String imagen = "https://www.santaisabel.cl/img/placeholders/product-placeholder.jpg"; 
                    double precio = 0;

                    if (item.has("items") && item.path("items").isArray() && item.path("items").size() > 0) {
                        JsonNode firstSku = item.path("items").get(0);
                        if (firstSku.has("images") && firstSku.path("images").size() > 0) {
                            imagen = firstSku.path("images").get(0).path("imageUrl").asText();
                        }
                        JsonNode sellers = firstSku.path("sellers");
                        if (sellers.isArray() && sellers.size() > 0) {
                            precio = sellers.get(0).path("commertialOffer").path("Price").asDouble(0);
                        }
                    }

                    products.add(ProductSupermarket.builder()
                            .nombre(nombre)
                            .descripcion(item.path("brand").asText(""))
                            .imagen(imagen)
                            .supermercadoUrl(urlProducto)
                            .precioUnitario(new Precio(precio, "CLP"))
                            .build());
                }
            }
        } catch (Exception e) {
            System.err.println("Error parseando JSON Santa Isabel: " + e.getMessage());
        }
        return products;
    }
}