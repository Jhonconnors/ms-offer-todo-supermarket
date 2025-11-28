package com.example.service;

import com.example.model.ProductListSupermarket;
import com.example.model.ProductSupermarket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class SupermarketService {

    @Autowired
    private LiderService liderService;

    @Autowired
    private SantaIsabelService santaIsabelService;

    // Este es el método que tu Controller no encuentra.
    // Al arreglar los imports y modelos arriba, este método se hará visible.
    public ProductListSupermarket searchInAllSupermarkets(String query, String comuna) {
        List<ProductSupermarket> combinedList = new ArrayList<>();

        // Llamadas asíncronas para no bloquear
        CompletableFuture<List<ProductSupermarket>> liderFuture = CompletableFuture.supplyAsync(() -> 
            liderService.searchProducts(query)
        ).exceptionally(ex -> new ArrayList<>()); 

        CompletableFuture<List<ProductSupermarket>> santaIsabelFuture = CompletableFuture.supplyAsync(() -> 
            santaIsabelService.searchProducts(query)
        ).exceptionally(ex -> new ArrayList<>());

        // Unir resultados
        try {
            combinedList.addAll(liderFuture.join());
            combinedList.addAll(santaIsabelFuture.join());
        } catch (Exception e) {
            // Loggear error si es necesario
            e.printStackTrace();
        }

        // Retornar el objeto que espera el controlador
        return ProductListSupermarket.builder()
                .items(combinedList)
                .build();
    }
}