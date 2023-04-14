Uma réplica funciona da seguinte forma:
- Quando recebe um pedido de atualização de um cliente:
    1. Atualiza a sua replicaTS (incrementando a sua entrada no vector clock);
    2. Adiciona a atualização à ledger;
    3. Se for possível, tenta executar a operação (atualizando consecutivamente o valueTS).
- Quando recebe um pedido gossip de outra réplica:
    1. Adiciona todas as atualizações que ainda nao conheça à ledger;
    2. Atualiza a sua replicaTS (dá merge com o timestamp recebido por gossip);
    3. Ordena as operações na ledger, evitando aquelas que sabe já estarem estáveis para otimizar a ordenação. É importante notar que:
        - Guardar o valor minStable permite-nos, para ledgers muito grandes, truncar a maioria das operações reduzindo drasticamente o tamanho do ledger que temos de ordenar (levando a uma grande otimização).
        - Para ordenar operações, nós estendemos a o critério de ordem parcial entre timestamps para um critério de ordem total (que respeita o critério parcial). Assim, é-nos sempre possível ordenar operações usando os algoritmos de ordenação que requerem uma ordem total, sem afetar a ordenação parcial entre as operações.
    4. Tenta executar cada operação que ainda não tenha sido executada (atualizando consecutivamente o valueTS).
- Quando recebe um pedido de leitura de um cliente:
    1. Se a réplica ainda não tiver executado todas as operações que precisa para responder ao pedido, adormece, aguardando pelos gossips necessários para executar a operação.
    2. Quando uma operação de atualização é executada, todas as threads com operações de escrita são acordadas, verificando outra vez se já podem. Aquando disto, a operação é adicionada ao conjunto de operações executadas. A manutenção desta estrutura permite-nos uma verificação mais eficiente de se uma operação está ou não executada (doutra forma teríamos de percorrer o ledger todo).

Para permitir a entrada de mais servidores (para além dos dois inicias), os nossos vector clocks têm tamanho virtualmente infinito, já que são representados como mapas cujas entradas não presentes são tomadas como nulas.
É importante salientar que optamos por associar cada réplica ao seu target e não a um qualificador, pois a sua utilização pareceu-nos não só desnecessária ao nível da comunicação inter-réplicas, mas problemática. 
Nomeadamente, se uma réplica se registar com um qualificador que já tinha sido usado (por outra que já não exista), o nosso algoritmo teria problemas: várias réplicas poderiam partilhar uma só entrada num timestamp.
Como são utilizadas caches, foi necessário implementar uma chamada grpc invalidateCache, que o namingServer manda a cada réplica sempre que uma réplica se regista.
Sem este mecansimo, as réplicas pré-existentes continuariam a conferenciar entre si, ignorando a existência da recém adicionada.

É ainda relevante mencionar que cada réplica mantém, para além do mencionado, o número de transações que já enviou para cada uma das restantes réplicas.
Como o ledger é append-only, isto permite-nos truncar o ledger antes de o enviar a uma dada réplica, poupando bandwidth que doutra forma seria gasta a enviar transações que a réplica destino certamente já conhecia. 