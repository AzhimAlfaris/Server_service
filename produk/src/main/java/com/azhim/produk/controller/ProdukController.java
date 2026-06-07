package com.azhim.produk.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.azhim.produk.model.Produk;
import com.azhim.produk.service.ProdukService;

@RestController
@RequestMapping("/produk")
public class ProdukController {
    @Autowired ProdukService produkService;

    @PostMapping
    public Produk setProduk(@RequestBody Produk produk) {
        return produkService.setProduk(produk);
    }

    @GetMapping("/{id}")
    public Produk getProdukById(@PathVariable Long id) {
        return produkService.getProdukById(id);
    }

    @GetMapping
    public List<Produk> getAllProduk() {
        return produkService.getAllProduk();
    }
}
