package com.azhim.produk.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.azhim.produk.model.Produk;
import com.azhim.produk.repository.ProdukRepository;

@Service
public class ProdukService {
    @Autowired ProdukRepository produkRepository;

    public Produk setProduk(Produk produk) {
        return produkRepository.save(produk);
    }

    public Produk getProdukById(Long id) {
        return produkRepository.findById(id).orElse(null);
    }

    public List<Produk> getAllProduk() {
        return produkRepository.findAll();
    }
}
