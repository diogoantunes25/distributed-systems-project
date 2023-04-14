Quanto ao servidor, é relevante salientar as seguintes propriedades que ele guarda:
- ledger: conjunto de updates já recebidos pela réplica
- minStable: número máximo x tal que os primeiros x updates no ledger já foram executados
- replicaTS: vector clock relativo aos updates recebidos pela réplica
- valueTS: vector clock relativo aos updates executados pela réplica
- lock: uma lock para o objeto ledger
- condition: uma condition variable para o lock a cima
- worker: uma worker thread que está permanentemente em loop sobre o ledger, verificando se há updates por executar

Uma réplica funciona então da seguinte forma:
- quando recebe um pedido de atualização, aumenta a sua entrada na replicaTS (vai acontecer mais um update na réplica) e adiciona o update ao legder
- quando recebe uma mensagem gossip dá merge do log recebido com o seu log, e do timestamp recebido com o seu timestamp
- quando recebe um pedido de leitura
- a worker thread percorre repetidamente o ledger:
    - se na iteração corrente sobre o ledger occorrer alguma execução (o estado é atualizado), a thread volta a percorrer o ledger mal acabe esta iteração (alguma atualização pode ter sido desbloqueada)
    - caso contrário - nenhuma operação foi executada - a thread aguarda que haja atualizações sobre o ledger, nomeadamente, uma operação que é inserida no ledger

É fundamental garantir que as atualizações numa réplica são sempre executadas numa ordem que satisfaça a causalidade entre as mesmas.
Note-se que uma atualização recebida de um cliente é sempre executada depois daquelas das quais depende causalmente, por definição da arquitetura gossip.
No entanto, alguma cautela é necessária com as atualizações que são recebidas por uma réplica de um outra réplica numa mensagem gossip.
Como descrito na definição do algoritmo, é necessário garantir que uma atualização só é executada se todas as atualizações com dependências menores que a primeira já tiverem sido executadas também.
Para garantir, usamos um critério de ordenação total de operações que respeita a sua ordenação parcial.
Assim, é-nos possível ordenar as operações recebidas por gossip de forma a que sejam sempre executadas numa ordem correta.

É importante salientar que na nossa solução, optamos por fazer cada réplica associar as restantes ao seu target, e não a um qualificador. 
A abstração dos qualificadores pareceu-nos não só desnecessária ao nível da comunicação inter-réplicas, mas problemática. 
Nomeadamente, se uma réplica se registar com um qualificador que já tinha sido usado (por outra que já não exista), o nosso algoritmo teria problemas: várias réplicas poderiam partilhar uma só entrada num timestamp.

// TODO: testar se leitura bloqueia
// TODO: implementar table?
