package com.projeto.service;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ModelosAnaliticos {

	private final List<ModeloAnalitico> modelos;

	public ModelosAnaliticos(List<ModeloAnalitico> modelos) {
		this.modelos = List.copyOf(modelos);
	}

	public List<ModeloAnalitico> listar() {
		return modelos;
	}
}
