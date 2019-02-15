package br.com.caelum.dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;

import br.com.caelum.model.Loja;
import br.com.caelum.model.Produto;

@Repository
public class ProdutoDao {

	@PersistenceContext
	private EntityManager em;

	public List<Produto> getProdutos() {
		return em.createQuery("select distinct p from Produto p", Produto.class)
				.setHint("org.hibernate.cacheable", "true")
				.setHint("javax.persistence.loadgraph", em.getEntityGraph("produtoComCategoria")).getResultList();
	}

	public Produto getProduto(Integer id) {
		Produto produto = em.find(Produto.class, id);
		return produto;
	}

	public List<Produto> getProdutos(String nome, Integer categoriaId, Integer lojaId) {
		/*
		 * Usando criteria API para poder escrever esse método de forma dinamica e
		 * elegante. No caso do criteria, vamos utilizar um tipo de query montado a
		 * partir dessa api, atraves da interface CriteriaQuery
		 *
		 * Esse criteriaBuilder tem vários métodos auxiliares para a query, e é bom
		 * instancia-lo dessa maneira, já que ele é mt importante, é bom deixar ele em
		 * uma váriavel especifica para ele.
		 * 
		 * CriteriaBuilder é uma fábrica auxiliar para criar expressões sobre as funções
		 * que utilizaremos na busca. A fábrica não executa a query, ela apenas ajuda a
		 * criá-la.
		 */
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();

		/*
		 * Utilizando a CriteriaQuery descrevemos a busca como algo parecido ao JPQL.
		 * Quando escrevermos JPQL, usaremos palavras chaves como select, from, where ou
		 * groupBy. As mesmas palavras chaves aparecem na CriteriaQuery, mas aqui são
		 * nomes de métodos. Resumindo, encontramos os seguintes métodos na interface
		 * CriteriaQuery select, from, where, orderBy, groupBy, having
		 * 
		 * A CriteriaQuery é fortemente tipada, isso quer dizer que ao chamar um método
		 * ele vai retornar o tipo que especifiquei, ou uma lista desse
		 */
		CriteriaQuery<Produto> query = criteriaBuilder.createQuery(Produto.class);
		/*
		 * Temos que, explicitamente, informar de onde deve ser feito o select. Já vimos
		 * que existe um método no CriteriaQuery chamado from. Através deste método,
		 * podemos passar a classe que vai servir de base na consulta (Produto.class)
		 * 
		 * Agora que sabemos como buscar todos os produtos, vamos colocar alguns filtros
		 * na nossa query. Para isso, precisaremos pegar referencias dos atributos da
		 * classe produto, referencias para o nome, categoria e loja para poder fazer as
		 * comparações. Para poder pegar as referencias desses atributos, o método from
		 * retorna um objeto do tipo Root, uma interface da JPA, a partir desse root que
		 * conseguimos traçar os caminhos para cada um dos atributos do produto
		 */
		Root<Produto> root = query.from(Produto.class);

		/*
		 * Esse método get é generico, então devemos passar um tipo antes. Esse get
		 * retorna um objeto do tipo Path, que é o caminho traçado a partir do root para
		 * cada atributo. A partir do root a gente traça o caminho para chegar ao
		 * atributo nome
		 */

		Path<String> pathNome = root.<String>get("nome");
		Path<Integer> pathLoja = root.<Loja>get("loja").<Integer>get("id");
		/*
		 * Como a categoria é manyToMany precisaremos de um join, a partir do produto
		 */
		Path<Integer> pathCategoria = root.join("categorias").<Integer>get("id");

		/*
		 * Com esse caminho, podemos pedir para a JPA fazer uma comparação entre o nome
		 * passado na busca e as entidades. a JPA chama essas caracteristicas,
		 * igualdades de predicados, e podem ser encontrados vários desses com o
		 * criteria builder
		 * 
		 * Para passar o predicates para o where somente se o não for nulo ou vazio,
		 * teremos esse ifs elses checando os parametros
		 */
		List<Predicate> predicates = new ArrayList<>();

		if (!nome.isEmpty()) {
			Predicate nomeIgual = criteriaBuilder.like(pathNome, "%" + nome + "%");
			predicates.add(nomeIgual);
		}
		if (categoriaId != null) {
			Predicate categoriaIgual = criteriaBuilder.equal(pathCategoria, categoriaId);
			predicates.add(categoriaIgual);
		}
		if (lojaId != null) {
			Predicate lojaIgual = criteriaBuilder.equal(pathLoja, lojaId);
			predicates.add(lojaIgual);
		}
		/*
		 * tendo esses predicates, vamos ter que colocar ele na query, com um método
		 * where, da query
		 */
		query.where(predicates.toArray(new Predicate[0]));

		/*
		 * Lembrando que no final das contas, temos que transformar esse criteria em um
		 * typed query
		 */

		TypedQuery<Produto> typedQuery = em.createQuery(query);
		typedQuery.setHint("org.hibernate.cacheable", "true");
		return typedQuery.getResultList();

		/*
		 * A classe CriteriaBuilder permite montar um Predicate mais avançado, como por
		 * exemplo:
		 * 
		 * Predicate conjuncao = builder.conjunction();
		 * 
		 * if (!nome.isEmpty()) { Path<String> nomeProduto = produtoRoot.<String>
		 * get("nome"); Predicate nomeIgual = builder.like(nomeProduto, "%" + nome +
		 * "%");
		 * 
		 * conjuncao = builder.and(nomeIgual); }
		 * 
		 * if (categoriaId != null) { Join<Produto, List<Categoria>> join =
		 * produtoRoot.join("categorias"); Path<Integer> categoriaProduto =
		 * join.get("id");
		 * 
		 * conjuncao = builder.and(conjuncao, builder.equal(categoriaProduto,
		 * categoriaId)); }
		 * 
		 * if (lojaId != null) { Path<Loja> loja = produtoRoot.<Loja> get("loja");
		 * Path<Integer> id = loja.<Integer> get("id");
		 * 
		 * conjuncao = builder.and(conjuncao, builder.equal(id, lojaId)); }
		 * 
		 * TypedQuery<Produto> typedQuery = em.createQuery(query.where(conjuncao));
		 */
	}

	public void insere(Produto produto) {
		if (produto.getId() == null)
			em.persist(produto);
		else
			em.merge(produto);
	}

}
