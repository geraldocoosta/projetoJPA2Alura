package br.com.caelum.teste;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import br.com.caelum.JpaConfigurator;

public class TesteCache {

	/*
	 * O cache serve para melhorar a performance da nossa aplicação. O sistema
	 * primeiro consulta o cache e verifica se ele já contêm a informação que
	 * buscamos, depois, se necessário, acessa o banco de dados. O acesso ao banco é
	 * uma operação computacionalmente custosa.
	 * 
	 * A forma mais comum de trabalhar com o cache é utilizar o que já vem por
	 * padrão nos EntityManager. O cache de primeiro nível, como é conhecido, impede
	 * que carreguemos a entidade duas vezes do banco. Por exemplo, ao executarmos o
	 * método find duas vezes para o mesmo id, somente a primeira chamada irá
	 * disparar uma query. Portanto, ao executarmos o find pela segunda vez em busca
	 * do mesmo objeto usando o mesmo EntityManager ele saberá que aquela entidade
	 * já foi carregada e irá reutilizar o resultado sem realizar uma nova busca ao
	 * banco de dados.
	 */

	public static void main(String[] args) {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(JpaConfigurator.class);

		EntityManagerFactory emf = ctx.getBean(EntityManagerFactory.class);
		EntityManager em = emf.createEntityManager();

		/*
		 * No caso de dois entity managers tiverem que fazer a requisição a uma mesma
		 * entidade, será utilizada o cache de nivel 1, que é um cache do mesmo
		 * EntityManager, mas como utilizaremos o cache para mais de 1 EntityManager, um
		 * cache compartilhado, então o código comentado abaixo dá bom
		 */

//		Produto produto = em.find(Produto.class, 1);
//		Produto produto2 = em.find(Produto.class, 1);
//		System.out.println("nome" + produto.getNome());
//		System.out.println("nome" + produto2.getNome());

		/*
		 * Mas no caso de ter EntityManagers diferentes,a query é realizada duas vezes
		 * 
		 * No código abaixo é verificado que é realizado dois selects
		 * 
		 * 
		 * O que é o cache de primeiro nível e qual problema de utilizá-lo em uma
		 * aplicação Web ?
		 * 
		 * O cache de primeiro nível é o cache que vem por padrão nos EntityManagers.
		 * Ele nos impede de carregar duas vezes a mesma entidade do banco e, dessa
		 * forma, evita um acesso desnecessário.
		 * 
		 * O problema de utilizarmos o cache de primeiro nível da nossa aplicação é que
		 * nós já configuramos que seja criado um novo EntityManager a cada requisição.
		 * Ou seja, como cada requisição possuí o seu próprio EntityManager e cada um
		 * destes o seu próprio cache, os dados do cache acabam se perdendo quando a
		 * requisição termina, além de, não serem reaproveitados entre requisições.
		 */

//		EntityManager em2 = emf.createEntityManager();
//		
//		Produto produto = em.find(Produto.class, 1);
//		Produto produto2 = em2.find(Produto.class, 1);
//		System.out.println("nome" + produto.getNome());
//		System.out.println("nome" + produto2.getNome());

		/*
		 * O que precisamos é de um espaço de "cache" que seja compartilhado entre os
		 * vários EntityManagers da nossa aplicação e que seja utilizado quando o cache
		 * de primeiro nível não detiver a informação desejada. Esse espaço chamamos de
		 * cache de segundo nível.
		 * 
		 * Em geral, lidar com um cache de segundo nível é bem mais complexo do que
		 * tratar com um de primeiro, uma vez que a possibilidade de trabalhar com dados
		 * desatualizados (stale) é bem maior. Os objetos desse cache são invalidados
		 * quando há alguma operação de escrita na entidade (como update). Se houver
		 * algum outro sistema atualizando os dados no banco sem passar pela JPA seu uso
		 * pode se tornar inexecutável.
		 * 
		 * Por padrão, o cache de segundo nível vem desabilitado. Para ativá-lo,
		 * precisamos adicionar uma chave a mais na configuração do Hibernate. Em nosso
		 * caso, na classe JpaConfigurator
		 * 
		 * Se estivermos utilizando o persistence.xml, adicionamos uma tag com a mesma
		 * propriedade:
		 * 
		 * <property name="hibernate.cache.use_second_level_cache" value="true" />
		 * 
		 * Além disso, é necessário informar ao Hibernate qual será o provedor de cache
		 * que usaremos. O JBoss Wildfly já possui um provider embarcado, ele se chama
		 * infinispan. Em nosso projeto, usaremos o EhCache que é um dos providers mais
		 * comuns de se trabalhar com Hibernate.
		 * 
		 * Para configurá-lo como provider, adicionaremos a propriedade
		 * hibernate.cache.region.factory_class apontando para a implementação no
		 * persistence.xml, ficaria assim:
		 * 
		 * <property name="hibernate.cache.region.factory_class"
		 * value="org.hibernate.cache.ehcache.SingletonEhCacheRegionFactory" />
		 * 
		 * Com essa configuração já podemos começarmos a desfrutar do poder do cache de
		 * segundo nível. Porém, precisamos configurar nossas classes para usar cache.
		 * 
		 * Antes de realmente configurarmos o cache, vamos analisar o comportamento
		 * atual do nosso projeto. Para isso, reiniciamos o Tomcat e abrimos nossa
		 * aplicação. Para testar, acessamos a página de edição de algum produto, por
		 * exemplo: [http://localhost:8080/projeto_jpa_avancado/produto/1/form][1].
		 * 
		 * Podemos observar, através do console, que o Hibernate logou duas queries. A
		 * primeira buscará o produto e a segunda as categorias relacionadas ao produto
		 * que procuramos.
		 * 
		 * Se atualizarmos a página veremos que as mesmas queries são disparadas
		 * novamente contra o banco. Agora, vamos configurar a classe Produto para ser
		 * armazenada no Cache de segundo nível. Para isso, basta anotá-la com @Cache
		 * 
		 * Ao escrevermos Produto para anotar a entidade, recebemos um erro de
		 * compilação! Isso acontece porque, ao usarmos dessa maneira, não especificamos
		 * qual a estratégia de concorrência que o cache deve adotar. E essa é uma
		 * informação obrigatória ao usar @Cache!
		 * 
		 * Existem algumas estratégias para se lidar com eventuais situações de
		 * concorrência pois, afinal, quando trabalhamos com dados em sistemas
		 * distribuídos temos várias formas de lidar com a concorrência. Um problema
		 * muito comum é manter a consistência do estado. Lembra-se que fizemos isso no
		 * capítulo de "Lock"? Repare que se o produto não pudesse ser alterado não
		 * precisaríamos do lock.
		 * 
		 * Se não fosse possível alterar o produto, poderíamos utilizar a estratégia
		 * READ_ONLY que abre mão dos locks e sincronizações por não permitir alterações
		 * no estado do objeto. Essa é forma mais barata, computacionalmente, de se
		 * trabalhar com cache de segundo nível.
		 * 
		 * Em situações em que alterações de estado são necessárias e há grandes chances
		 * de que elas ocorram simultaneamente, podemos adotar a estratégia READ_WRITE
		 * que consome muitos recursos para garantir que a última versão da entidade no
		 * banco seja a mesma que está no cache.
		 * 
		 * Porém, alterações ocorrendo ao mesmo tempo são incomuns. Ainda que não
		 * precisemos de todos os recursos usados pela estratégia READ_WRITE, faz-se
		 * necessário modificar o estado da entidade. Nessa situação, podemos usar a
		 * estratégia NON_STRICT_READ_WRITE ideal, ou seja, quando não há problemas em
		 * ler dados inconsistentes caso hajam alterações simultâneas.
		 * 
		 * Em ambientes JTA, por exemplo, os servidores de aplicação podem optar pela
		 * estratégia TRANSACTIONAL. Em nosso projeto iremos utilizar a estratégia
		 * NON_STRICT_READ_WRITE:
		 * 
		 * Para cachear também as queryes, temos que adicionar uma anotação no xml ou
		 * nas propriedades e marcar a query com uma dica, com as linhas abaixo
		 * respectivamente
		 * 
		 * props.setProperty("hibernate.cache.use_query_cache", "true");
		 * 
		 * queryQualquer.setHint("org.hibernate.cacheable", "true");
		 * 
		 * 
		 * A estratégia READ_ONLY deve ser utilizada quando uma entidade não deve ser
		 * modificada.
		 * 
		 * A estratégia READ_WRITE deve ser utilizada quando uma entidade pode ser
		 * modificada e há grandes chances que modificações em seu estado ocorram
		 * simultaneamente. Essa estratégia é a que mais consome recursos.
		 * 
		 * A estratégia NONSTRICT_READ_WRITE deve ser utilizada quando uma entidade pode
		 * ser modificada, mas é incomum que as alterações ocorram ao mesmo tempo. Ela
		 * consome menos recursos que a estratégia READ_WRITE e é ideal quando não há
		 * problemas de dados inconsistentes serem lidos quando ocorrem alterações
		 * simultâneas.
		 * 
		 * A estratégia TRANSACTIONAL deve ser utilizada em ambientes JTA, como por
		 * exemplo em servidores de aplicação. Como utilizamos Tomcat com Spring (sem
		 * JTA) essa opção não funcionará.
		 */
	}
}
