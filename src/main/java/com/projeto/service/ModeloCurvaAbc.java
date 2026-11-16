package com.projeto.service;

import org.springframework.stereotype.Component;

@Component
public class ModeloCurvaAbc implements ModeloAnalitico {

	@Override
	public String codigo() {
		return CurvaAbc.CODIGO;
	}

	@Override
	public String nome() {
		return "Curva ABC (Pareto)";
	}
}
