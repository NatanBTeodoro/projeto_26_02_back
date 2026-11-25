package com.projeto.analise;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.projeto.dto.PainelAdminDTO;
import com.projeto.entity.Pedido;
import com.projeto.entity.PedidoItem;
import com.projeto.entity.Restaurante;
import com.projeto.entity.enums.ClasseMenu;
import com.projeto.entity.enums.StatusPedido;

public final class PainelAdmin {

	private static final BigDecimal CEM = new BigDecimal("100");
	private static final Locale PT = new Locale("pt", "BR");

	private PainelAdmin() {
	}

	public record Cardapio(long restauranteId, int ativos, int semCusto) {
	}

	public static PainelAdminDTO.Response montar(
			List<Restaurante> restaurantes,
			List<Pedido> atuais,
			List<Pedido> anteriores,
			List<Pedido> recentes,
			List<Cardapio> cardapios,
			boolean semPeriodoAnterior,
			java.time.LocalDate de,
			java.time.LocalDate ate,
			java.time.LocalDate deAnterior,
			java.time.LocalDate ateAnterior) {
		Map<Long, Bloco> blocos = new HashMap<>();
		Map<Long, Bloco> blocosAnteriores = new HashMap<>();
		for (Restaurante restaurante : restaurantes) {
			blocos.put(restaurante.getId(), new Bloco(restaurante.getId(), restaurante.getNome()));
			blocosAnteriores.put(restaurante.getId(), new Bloco(restaurante.getId(), restaurante.getNome()));
		}
		Totais agora = ler(atuais, blocos);
		Totais antes = semPeriodoAnterior ? new Totais() : ler(anteriores, blocosAnteriores);
		Set<Long> comVendaRecente = restaurantesComVenda(recentes, blocos.keySet());

		BigDecimal margem = razao(agora.lucro, agora.faturamentoComCusto);
		BigDecimal margemAntes = semPeriodoAnterior ? null : razao(antes.lucro, antes.faturamentoComCusto);
		BigDecimal cobertura = razao(agora.faturamentoComCusto, agora.faturamento);
		String complementoMargem = cobertura == null || cobertura.compareTo(CEM) >= 0
				? null
				: "Margem calculada sobre " + numero(cobertura, 0) + "% do faturamento";

		List<String> semVenda = blocos.values().stream()
				.filter(bloco -> bloco.pedidos == 0)
				.map(bloco -> bloco.nome)
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.toList();
		boolean atencaoSemVenda = !semVenda.isEmpty();

		List<PainelAdminDTO.Kpi> kpis = List.of(
				kpi("faturamento", "Faturamento", "MOEDA", agora.faturamento, null, null, false, List.of(),
						percentual(agora.faturamento, antes.faturamento, semPeriodoAnterior)),
				kpi("lucro", "Lucro bruto", "MOEDA", agora.lucro, null, null, false, List.of(),
						percentual(agora.lucro, antes.lucro, semPeriodoAnterior)),
				kpi("custo", "Custo bruto", "MOEDA", agora.custo, null,
						cobertura == null ? null : "Cobertura de custo " + numero(cobertura, 0) + "%",
						false, List.of(),
						percentual(agora.custo, antes.custo, semPeriodoAnterior)),
				kpi("margem", "Margem média", "PERCENTUAL", margem, null, complementoMargem, false, List.of(),
						pontos(margem, margemAntes, semPeriodoAnterior)),
				kpi("pedidos", "Pedidos", "INTEIRO", BigDecimal.valueOf(agora.pedidos), null, null, false, List.of(),
						percentual(BigDecimal.valueOf(agora.pedidos), BigDecimal.valueOf(antes.pedidos), semPeriodoAnterior)),
				kpi("ticket", "Ticket médio", "MOEDA", ticket(agora), null, null, false, List.of(),
						percentual(ticket(agora), ticket(antes), semPeriodoAnterior)),
				kpi("restaurantes", "Restaurantes com venda", "TEXTO", BigDecimal.valueOf(agora.restaurantes),
						agora.restaurantes + " de " + restaurantes.size() + " ativos",
						null,
						atencaoSemVenda,
						semVenda,
						percentual(BigDecimal.valueOf(agora.restaurantes), BigDecimal.valueOf(antes.restaurantes), semPeriodoAnterior)));

		Map<Long, Cardapio> porCardapio = new HashMap<>();
		for (Cardapio cardapio : cardapios) {
			porCardapio.put(cardapio.restauranteId(), cardapio);
		}

		EnumMap<ClasseMenu, Integer> produtosClasse = new EnumMap<>(ClasseMenu.class);
		EnumMap<ClasseMenu, BigDecimal> lucroClasse = new EnumMap<>(ClasseMenu.class);
		for (ClasseMenu classe : ClasseMenu.values()) {
			produtosClasse.put(classe, 0);
			lucroClasse.put(classe, BigDecimal.ZERO);
		}
		int naConsolidacao = 0;
		List<PainelAdminDTO.RestauranteSaude> saude = new ArrayList<>();
		boolean amostraPlataformaPequena = agora.pedidos < LimitesPainelAdmin.MIN_PEDIDOS;
		List<PainelAdminDTO.Alerta> alertas = new ArrayList<>();

		Map<Long, String> faixaAtual = faixas(blocos);
		Map<Long, String> faixaAnterior = semPeriodoAnterior ? Map.of() : faixas(blocosAnteriores);

		for (Bloco bloco : blocos.values()) {
			boolean amostraPequena = bloco.pedidos < LimitesPainelAdmin.MIN_PEDIDOS
					|| bloco.vendidos.size() < LimitesPainelAdmin.MIN_PRODUTOS;
			ClassificadorCardapio.Resultado resultado = ClassificadorCardapio.resultado(bloco.linhas);
			Map<Long, ClasseMenu> classePorProduto = new HashMap<>();
			EnumMap<ClasseMenu, Integer> contagem = new EnumMap<>(ClasseMenu.class);
			for (ClasseMenu classe : ClasseMenu.values()) {
				contagem.put(classe, 0);
			}
			for (ClassificadorCardapio.Item item : resultado.itens()) {
				classePorProduto.put(item.produtoId(), item.classe());
				contagem.merge(item.classe(), 1, Integer::sum);
			}
			EnumMap<ClasseMenu, BigDecimal> lucroDoRestaurante = lucroPorClasse(bloco, classePorProduto);
			if (!amostraPequena) {
				naConsolidacao++;
				for (ClasseMenu classe : ClasseMenu.values()) {
					produtosClasse.merge(classe, contagem.get(classe), Integer::sum);
					lucroClasse.merge(classe, lucroDoRestaurante.getOrDefault(classe, BigDecimal.ZERO), BigDecimal::add);
				}
			}
			int totalItens = resultado.itens().size();
			BigDecimal estrelas = totalItens == 0
					? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
					: razao(BigDecimal.valueOf(contagem.get(ClasseMenu.ESTRELA)), BigDecimal.valueOf(totalItens));
			List<PainelAdminDTO.Parte> partes = new ArrayList<>();
			for (ClasseMenu classe : ClasseMenu.values()) {
				BigDecimal percentual = totalItens == 0
						? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
						: razao(BigDecimal.valueOf(contagem.get(classe)), BigDecimal.valueOf(totalItens));
				partes.add(new PainelAdminDTO.Parte(classe.name(), contagem.get(classe), percentual));
			}
			Cardapio cardapio = porCardapio.get(bloco.id);
			BigDecimal vendidosSemCusto = bloco.vendidos.isEmpty()
					? null
					: razao(BigDecimal.valueOf(bloco.semCusto.size()), BigDecimal.valueOf(bloco.vendidos.size()));
			BigDecimal cardapioSemCusto = cardapio == null || cardapio.ativos() == 0
					? null
					: razao(BigDecimal.valueOf(cardapio.semCusto()), BigDecimal.valueOf(cardapio.ativos()));
			saude.add(new PainelAdminDTO.RestauranteSaude(
					bloco.id,
					bloco.nome,
					amostraPequena,
					bloco.vendidos.size(),
					bloco.pedidos,
					estrelas,
					vendidosSemCusto,
					cardapioSemCusto,
					partes,
					dinheiro(bloco.custo),
					razao(bloco.faturamentoComCusto, bloco.faturamento)));

			if (!amostraPequena) {
				avisarCardapio(alertas, bloco, contagem, totalItens, estrelas);
				avisarMargem(alertas, bloco, margem);
			}
			avisarCusto(alertas, bloco, vendidosSemCusto);
			if (!amostraPlataformaPequena && !semPeriodoAnterior) {
				avisarQueda(alertas, bloco, blocosAnteriores.get(bloco.id));
				avisarFaixa(alertas, bloco, faixaAtual.get(bloco.id), faixaAnterior.get(bloco.id));
			}
		}

		saude.sort(Comparator
				.comparing(PainelAdminDTO.RestauranteSaude::percentualEstrelas, Comparator.nullsFirst(Comparator.reverseOrder()))
				.thenComparing(PainelAdminDTO.RestauranteSaude::nome, String.CASE_INSENSITIVE_ORDER));

		if (!amostraPlataformaPequena && agora.restaurantes >= 2 && agora.lucro.signum() > 0) {
			avisarDependencia(alertas, blocos, agora.lucro);
		}
		avisarSemVendaRecente(alertas, restaurantes, comVendaRecente);
		if (amostraPlataformaPequena) {
			alertas.add(new PainelAdminDTO.Alerta(
					"INFORMATIVO",
					"Poucos pedidos no período: as análises podem não ser representativas.",
					BigDecimal.valueOf(LimitesPainelAdmin.MIN_PEDIDOS - agora.pedidos)));
		}

		alertas.sort(Comparator
				.comparingInt((PainelAdminDTO.Alerta alerta) -> gravidade(alerta.severidade()))
				.thenComparing(PainelAdminDTO.Alerta::impacto, Comparator.reverseOrder()));

		int produtosConsolidados = produtosClasse.values().stream().mapToInt(Integer::intValue).sum();
		BigDecimal lucroConhecido = lucroClasse.entrySet().stream()
				.filter(entrada -> entrada.getKey() != ClasseMenu.DADO_INCOMPLETO)
				.map(Map.Entry::getValue)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		List<PainelAdminDTO.Fatia> distribuicao = new ArrayList<>();
		for (ClasseMenu classe : ClasseMenu.values()) {
			boolean incompleto = classe == ClasseMenu.DADO_INCOMPLETO;
			BigDecimal lucro = incompleto ? null : dinheiro(lucroClasse.get(classe));
			distribuicao.add(new PainelAdminDTO.Fatia(
					classe.name(),
					produtosClasse.get(classe),
					produtosConsolidados == 0 ? null : razao(BigDecimal.valueOf(produtosClasse.get(classe)), BigDecimal.valueOf(produtosConsolidados)),
					lucro,
					incompleto || lucroConhecido.signum() == 0 ? null : razao(lucroClasse.get(classe), lucroConhecido)));
		}

		return new PainelAdminDTO.Response(
				de,
				ate,
				semPeriodoAnterior ? null : deAnterior,
				semPeriodoAnterior ? null : ateAnterior,
				semPeriodoAnterior,
				kpis,
				distribuicao,
				naConsolidacao,
				saude,
				alertas);
	}

	private static void avisarDependencia(List<PainelAdminDTO.Alerta> alertas, Map<Long, Bloco> blocos, BigDecimal lucroTotal) {
		for (Bloco bloco : blocos.values()) {
			if (bloco.pedidos == 0 || bloco.lucro.signum() <= 0) {
				continue;
			}
			BigDecimal share = razao(bloco.lucro, lucroTotal);
			if (share.compareTo(LimitesPainelAdmin.DEPENDENCIA_ATENCAO) < 0) {
				continue;
			}
			String severidade = share.compareTo(LimitesPainelAdmin.DEPENDENCIA_CRITICO) >= 0 ? "CRITICO" : "ATENCAO";
			alertas.add(new PainelAdminDTO.Alerta(
					severidade,
					bloco.nome + " concentra " + numero(share, 1) + "% do lucro: risco de dependência.",
					share));
		}
	}

	private static void avisarQueda(List<PainelAdminDTO.Alerta> alertas, Bloco atual, Bloco anterior) {
		if (anterior == null || anterior.lucro.signum() <= 0 || anterior.pedidos == 0) {
			return;
		}
		if (atual.lucro.compareTo(anterior.lucro) >= 0) {
			return;
		}
		BigDecimal queda = anterior.lucro.subtract(atual.lucro).multiply(CEM)
				.divide(anterior.lucro, 2, RoundingMode.HALF_UP);
		if (queda.compareTo(LimitesPainelAdmin.QUEDA_ATENCAO) < 0) {
			return;
		}
		String severidade = queda.compareTo(LimitesPainelAdmin.QUEDA_CRITICO) >= 0 ? "CRITICO" : "ATENCAO";
		alertas.add(new PainelAdminDTO.Alerta(
				severidade,
				atual.nome + " perdeu " + numero(queda, 0) + "% de lucro vs. período anterior.",
				queda));
	}

	private static void avisarFaixa(List<PainelAdminDTO.Alerta> alertas, Bloco bloco, String atual, String anterior) {
		if (atual == null || anterior == null || ordemFaixa(atual) <= ordemFaixa(anterior)) {
			return;
		}
		alertas.add(new PainelAdminDTO.Alerta(
				"ATENCAO",
				bloco.nome + " caiu da faixa " + anterior + " para " + atual + ".",
				BigDecimal.valueOf(ordemFaixa(atual) - ordemFaixa(anterior))));
	}

	private static void avisarCusto(List<PainelAdminDTO.Alerta> alertas, Bloco bloco, BigDecimal percentual) {
		if (percentual == null || percentual.compareTo(LimitesPainelAdmin.DADOS_INCOMPLETOS) < 0) {
			return;
		}
		alertas.add(new PainelAdminDTO.Alerta(
				"ATENCAO",
				bloco.nome + " tem " + numero(percentual, 0) + "% dos produtos vendidos sem custo cadastrado.",
				percentual));
	}

	private static void avisarMargem(List<PainelAdminDTO.Alerta> alertas, Bloco bloco, BigDecimal margemPlataforma) {
		BigDecimal margem = razao(bloco.lucro, bloco.faturamentoComCusto);
		if (margem == null || margemPlataforma == null) {
			return;
		}
		BigDecimal distancia = margemPlataforma.subtract(margem);
		if (distancia.compareTo(LimitesPainelAdmin.MARGEM_ABAIXO) < 0) {
			return;
		}
		alertas.add(new PainelAdminDTO.Alerta(
				"ATENCAO",
				bloco.nome + " opera com margem de " + numero(margem, 0) + "%, "
						+ numero(distancia, 0) + " p.p. abaixo da média.",
				distancia));
	}

	private static void avisarCardapio(
			List<PainelAdminDTO.Alerta> alertas,
			Bloco bloco,
			Map<ClasseMenu, Integer> contagem,
			int total,
			BigDecimal estrelas) {
		if (total == 0) {
			return;
		}
		BigDecimal vacas = razao(BigDecimal.valueOf(contagem.get(ClasseMenu.VACA_LEITEIRA)), BigDecimal.valueOf(total));
		boolean semEstrela = estrelas.signum() == 0;
		boolean muitaVaca = vacas.compareTo(LimitesPainelAdmin.VACA_LIMITE) >= 0;
		if (!semEstrela && !muitaVaca) {
			return;
		}
		String texto = semEstrela
				? bloco.nome + " não tem produtos Estrela: revise preços e custos."
				: bloco.nome + " tem " + numero(vacas, 0) + "% de Vaca leiteira: revise preços e custos.";
		alertas.add(new PainelAdminDTO.Alerta("ATENCAO", texto, semEstrela ? CEM : vacas));
	}

	private static void avisarSemVendaRecente(
			List<PainelAdminDTO.Alerta> alertas,
			List<Restaurante> restaurantes,
			Set<Long> comVendaRecente) {
		List<String> parados = restaurantes.stream()
				.filter(restaurante -> !comVendaRecente.contains(restaurante.getId()))
				.map(Restaurante::getNome)
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.toList();
		if (parados.isEmpty()) {
			return;
		}
		alertas.add(new PainelAdminDTO.Alerta(
				"ATENCAO",
				parados.size() + (parados.size() == 1 ? " restaurante sem vendas" : " restaurantes sem vendas")
						+ " nos últimos " + LimitesPainelAdmin.DIAS_SEM_VENDA + " dias: " + juntar(parados) + ".",
				BigDecimal.valueOf(parados.size())));
	}

	private static Map<Long, String> faixas(Map<Long, Bloco> blocos) {
		List<CurvaAbc.Entrada> entradas = blocos.values().stream()
				.map(bloco -> new CurvaAbc.Entrada(
						String.valueOf(bloco.id),
						bloco.nome,
						dinheiro(bloco.lucro),
						!bloco.semCusto.isEmpty()))
				.toList();
		Map<Long, String> faixas = new HashMap<>();
		for (CurvaAbc.Item item : CurvaAbc.calcular(entradas).itens()) {
			faixas.put(Long.valueOf(item.id()), item.faixa());
		}
		return faixas;
	}

	private static EnumMap<ClasseMenu, BigDecimal> lucroPorClasse(Bloco bloco, Map<Long, ClasseMenu> classes) {
		EnumMap<ClasseMenu, BigDecimal> lucro = new EnumMap<>(ClasseMenu.class);
		for (ClasseMenu classe : ClasseMenu.values()) {
			lucro.put(classe, BigDecimal.ZERO);
		}
		for (ClassificadorCardapio.Linha linha : bloco.linhas) {
			if (linha.custo() == null) {
				continue;
			}
			ClasseMenu classe = classes.get(linha.produtoId());
			if (classe == null || classe == ClasseMenu.DADO_INCOMPLETO) {
				continue;
			}
			BigDecimal valor = linha.preco().subtract(linha.custo()).multiply(BigDecimal.valueOf(linha.quantidade()));
			lucro.merge(classe, valor, BigDecimal::add);
		}
		return lucro;
	}

	private static Totais ler(List<Pedido> pedidos, Map<Long, Bloco> blocos) {
		Totais totais = new Totais();
		if (pedidos == null) {
			return totais;
		}
		for (Pedido pedido : pedidos) {
			if (pedido.getStatus() != StatusPedido.PAGO || pedido.getRestaurante() == null) {
				continue;
			}
			Bloco bloco = blocos.get(pedido.getRestaurante().getId());
			if (bloco == null) {
				continue;
			}
			totais.pedidos++;
			bloco.pedidos++;
			for (PedidoItem item : pedido.getItens()) {
				BigDecimal receita = item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade()));
				totais.faturamento = totais.faturamento.add(receita);
				bloco.faturamento = bloco.faturamento.add(receita);
				long produtoId = item.getProduto().getId();
				bloco.vendidos.add(produtoId);
				bloco.linhas.add(new ClassificadorCardapio.Linha(
						produtoId,
						item.getProduto().getNome(),
						item.getQuantidade(),
						item.getPrecoUnitario(),
						item.getCustoUnitario(),
						""));
				if (item.getCustoUnitario() == null) {
					bloco.semCusto.add(produtoId);
					continue;
				}
				BigDecimal lucro = item.getPrecoUnitario()
						.subtract(item.getCustoUnitario())
						.multiply(BigDecimal.valueOf(item.getQuantidade()));
				totais.lucro = totais.lucro.add(lucro);
				totais.faturamentoComCusto = totais.faturamentoComCusto.add(receita);
				totais.custo = totais.custo.add(item.getCustoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())));
				bloco.lucro = bloco.lucro.add(lucro);
				bloco.faturamentoComCusto = bloco.faturamentoComCusto.add(receita);
				bloco.custo = bloco.custo.add(item.getCustoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())));
			}
		}
		totais.faturamento = dinheiro(totais.faturamento);
		totais.lucro = dinheiro(totais.lucro);
		totais.faturamentoComCusto = dinheiro(totais.faturamentoComCusto);
		totais.custo = dinheiro(totais.custo);
		totais.restaurantes = (int) blocos.values().stream().filter(bloco -> bloco.pedidos > 0).count();
		return totais;
	}

	private static Set<Long> restaurantesComVenda(List<Pedido> pedidos, Set<Long> conhecidos) {
		Set<Long> ids = new HashSet<>();
		if (pedidos == null) {
			return ids;
		}
		for (Pedido pedido : pedidos) {
			if (pedido.getStatus() == StatusPedido.PAGO
					&& pedido.getRestaurante() != null
					&& conhecidos.contains(pedido.getRestaurante().getId())) {
				ids.add(pedido.getRestaurante().getId());
			}
		}
		return ids;
	}

	private static BigDecimal ticket(Totais totais) {
		if (totais.pedidos == 0) {
			return null;
		}
		return totais.faturamento.divide(BigDecimal.valueOf(totais.pedidos), 2, RoundingMode.HALF_UP);
	}

	private static PainelAdminDTO.Variacao percentual(BigDecimal atual, BigDecimal anterior, boolean semPeriodo) {
		if (semPeriodo) {
			return neutro("Sem período anterior", anterior);
		}
		BigDecimal agora = atual == null ? BigDecimal.ZERO : atual;
		BigDecimal antes = anterior == null ? BigDecimal.ZERO : anterior;
		if (antes.signum() == 0) {
			return agora.signum() == 0 ? neutro("—", antes) : neutro("Novo", antes);
		}
		BigDecimal delta = agora.subtract(antes).multiply(CEM).divide(antes.abs(), 2, RoundingMode.HALF_UP);
		if (delta.abs().compareTo(LimitesPainelAdmin.TETO_VARIACAO) > 0) {
			return new PainelAdminDTO.Variacao(sentido(delta), "> 999%", null, null, dinheiro(antes));
		}
		return new PainelAdminDTO.Variacao(sentido(delta), null, delta, null, escala(antes, atual));
	}

	private static PainelAdminDTO.Variacao pontos(BigDecimal atual, BigDecimal anterior, boolean semPeriodo) {
		if (semPeriodo) {
			return neutro("Sem período anterior", anterior);
		}
		if (atual == null && (anterior == null || anterior.signum() == 0)) {
			return neutro("—", anterior);
		}
		if (anterior == null) {
			return atual == null ? neutro("—", null) : neutro("Novo", null);
		}
		if (atual == null) {
			return neutro("—", anterior);
		}
		BigDecimal delta = atual.subtract(anterior).setScale(2, RoundingMode.HALF_UP);
		return new PainelAdminDTO.Variacao(sentido(delta), null, null, delta, anterior.setScale(2, RoundingMode.HALF_UP));
	}

	private static PainelAdminDTO.Variacao neutro(String texto, BigDecimal anterior) {
		return new PainelAdminDTO.Variacao("NEUTRO", texto, null, null, anterior == null ? null : anterior.setScale(2, RoundingMode.HALF_UP));
	}

	private static String sentido(BigDecimal delta) {
		int sinal = delta.signum();
		if (sinal > 0) {
			return "SOBE";
		}
		if (sinal < 0) {
			return "DESCE";
		}
		return "NEUTRO";
	}

	private static BigDecimal escala(BigDecimal antes, BigDecimal atual) {
		int casas = atual != null && atual.scale() == 0 ? 0 : 2;
		return antes.setScale(casas, RoundingMode.HALF_UP);
	}

	private static PainelAdminDTO.Kpi kpi(
			String codigo,
			String rotulo,
			String tipo,
			BigDecimal valor,
			String texto,
			String complemento,
			boolean atencao,
			List<String> detalhes,
			PainelAdminDTO.Variacao variacao) {
		return new PainelAdminDTO.Kpi(codigo, rotulo, tipo, valor, texto, complemento, atencao, detalhes, variacao);
	}

	private static BigDecimal razao(BigDecimal parte, BigDecimal total) {
		if (parte == null || total == null || total.signum() == 0) {
			return null;
		}
		return parte.multiply(CEM).divide(total, 2, RoundingMode.HALF_UP);
	}

	private static BigDecimal dinheiro(BigDecimal valor) {
		return valor.setScale(2, RoundingMode.HALF_UP);
	}

	private static int ordemFaixa(String faixa) {
		if ("A".equals(faixa)) {
			return 0;
		}
		if ("B".equals(faixa)) {
			return 1;
		}
		return 2;
	}

	private static int gravidade(String severidade) {
		if ("CRITICO".equals(severidade)) {
			return 0;
		}
		if ("ATENCAO".equals(severidade)) {
			return 1;
		}
		return 2;
	}

	private static String juntar(List<String> nomes) {
		if (nomes.size() == 1) {
			return nomes.get(0);
		}
		if (nomes.size() == 2) {
			return nomes.get(0) + " e " + nomes.get(1);
		}
		return String.join(", ", nomes.subList(0, nomes.size() - 1)) + " e " + nomes.get(nomes.size() - 1);
	}

	private static String numero(BigDecimal valor, int casas) {
		DecimalFormatSymbols simbolos = DecimalFormatSymbols.getInstance(PT);
		DecimalFormat formato = new DecimalFormat(casas == 0 ? "#,##0" : "#,##0.0", simbolos);
		formato.setRoundingMode(RoundingMode.HALF_UP);
		return formato.format(valor);
	}

	private static final class Totais {
		private BigDecimal faturamento = BigDecimal.ZERO;
		private BigDecimal faturamentoComCusto = BigDecimal.ZERO;
		private BigDecimal lucro = BigDecimal.ZERO;
		private BigDecimal custo = BigDecimal.ZERO;
		private long pedidos;
		private int restaurantes;
	}

	private static final class Bloco {
		private final long id;
		private final String nome;
		private long pedidos;
		private final Set<Long> vendidos = new HashSet<>();
		private final Set<Long> semCusto = new HashSet<>();
		private final List<ClassificadorCardapio.Linha> linhas = new ArrayList<>();
		private BigDecimal faturamento = BigDecimal.ZERO;
		private BigDecimal faturamentoComCusto = BigDecimal.ZERO;
		private BigDecimal lucro = BigDecimal.ZERO;
		private BigDecimal custo = BigDecimal.ZERO;

		private Bloco(long id, String nome) {
			this.id = id;
			this.nome = nome;
		}
	}
}
