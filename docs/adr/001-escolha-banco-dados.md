# ADR 001: Escolha do PostgreSQL como Banco de Dados

## Status
✅ Aceito

## Contexto
O sistema de autorização de transações financeiras precisa de um banco de dados que garanta:
- **Consistência ACID** para operações financeiras
- **Controle de concorrência** para prevenir race conditions
- **Transações atômicas** para débitos e créditos
- **Integridade referencial** entre contas e transações
- **Suporte a locks otimistas** para alta concorrência

## Decisão
Escolhemos **PostgreSQL** como banco de dados principal.

## Justificativa

### 1. **Consistência sobre Disponibilidade (Teorema CAP)**
```
Em sistemas financeiros, preferimos CP (Consistency + Partition Tolerance) sobre AP.
PostgreSQL garante ACID completo, essencial para operações monetárias.
```

### 2. **Controle de Concorrência**
- Suporte nativo a **Optimistic Locking** via versionamento (`@Version` no JPA)
- MVCC (Multi-Version Concurrency Control) permite leituras sem bloqueio
- Isolamento de transações configurável (READ COMMITTED, SERIALIZABLE)

### 3. **Evita Problemas de Consistência Eventual**
Bancos NoSQL como DynamoDB ou MongoDB teriam:
- ❌ Risco de double-spending em transações simultâneas
- ❌ Complexidade adicional para garantir consistência
- ❌ Necessidade de sagas distribuídas

### 4. **Performance Adequada**
Configuração otimizada no `docker-compose.yml`:
```yaml
shared_buffers=256MB
effective_cache_size=1GB
max_connections=200
synchronous_commit=off  # Trade-off para dev/test
```

### 5. **Custos em Produção (AWS)**
- RDS PostgreSQL oferece backups automáticos
- Read Replicas para escalar leituras
- Multi-AZ para alta disponibilidade
- Menor custo operacional vs. bancos proprietários (Oracle)

## Consequências

### Positivas ✅
- **Integridade garantida**:  Impossível ter saldos negativos não autorizados
- **Simplicidade**: JPA/Hibernate reduz código de persistência
- **Debugging**: SQL logs e queries explicadas facilmente
- **Conformidade**: Auditoria de transações confiável

### Negativas ⚠️
- **Escalabilidade vertical limitada**: Precisará de sharding se > 100K TPS
- **Latência de escrita**: Commit síncrono adiciona ~5-10ms
- **Custo de locks**: Concorrência extrema pode causar deadlocks

### Mitigações
- Usar **Optimistic Locking** para evitar locks pessimistas
- Implementar **retry automático** (`@Retryable`) em conflitos
- Considerar **CQRS** (Command Query Responsibility Segregation) no futuro
- **Read Replicas** para consultas de saldo

## Alternativas Consideradas

| Banco         | Prós                            | Contras                          | Motivo da Rejeição |
|---------------|---------------------------------|----------------------------------|--------------------|
| MongoDB       | Schema flexível, escala horizontal | Sem ACID multi-documento (antes 4.0) | Risco de inconsistência |
| DynamoDB      | Baixa latência, serverless      | Consistência eventual, custo alto de strong consistency | Complexidade de modelagem |
| MySQL         | Amplamente usado                | InnoDB menos robusto que Postgres para JSON/extensões | PostgreSQL é mais completo |
| Redis         | Latência < 1ms                  | Dados em memória, sem ACID transacional completo | Não substitui DB principal |

## Referências
- [PostgreSQL MVCC](https://www.postgresql.org/docs/current/mvcc.html)
- [JPA Optimistic Locking](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#locking)
- [AWS RDS Best Practices](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_BestPractices.html)

---
**Data:** 2025-12-20  
**Autores:** @msvidal  
**Revisores:** Equipe de Arquitetura