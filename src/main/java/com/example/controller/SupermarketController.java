package com.example.controller;

import com.example.model.ProductListSupermarket;
import com.example.service.SupermarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
// 1. IMPORTAR ESTO
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/supermarkets")
@CrossOrigin(origins = "*") 
public class SupermarketController {

    @Autowired
    private SupermarketService supermarketService;

    @GetMapping("/product")
    @PreAuthorize("hasAuthority('read:data')")
    public ResponseEntity<ProductListSupermarket> searchProducts(
            @RequestParam(name = "q") String query,
            @RequestParam(name = "comuna", required = false) String comuna) {
        
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ProductListSupermarket result = supermarketService.searchInAllSupermarkets(query, comuna);
        return ResponseEntity.ok(result);
    }
}