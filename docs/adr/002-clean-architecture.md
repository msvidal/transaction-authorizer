# ADR 002: Adoção de Clean Architecture

## Status
✅ Aceito

## Contexto
O sistema de autorização de transações financeiras precisa:
- **Isolar regras de negócio** de frameworks e tecnologias externas
- **Facilitar testes** sem dependência de infraestrutura
- **Permitir evolução** sem acoplamento ao Spring Boot ou PostgreSQL
- **Escalabilidade de equipe** com separação clara de responsabilidades
- **Conformidade** com auditoria (regras de negócio explícitas)

## Decisão
Adotamos **Clean Architecture** (Robert C. Martin) com 4 camadas concêntricas:

```
┌─────────────────────────────────────────────┐
│        4. Frameworks & Drivers              │  ← Web, DB, Queue
│    (Spring Boot, JPA, SQS, Controllers)     │
├─────────────────────────────────────────────┤
│     3. Interface Adapters                   │  ← DTOs, Mappers
│    (API DTOs, Persistence Adapters)         │
├─────────────────────────────────────────────┤
│     2. Application Business Rules           │  ← Use Cases
│    (ProcessTransaction, CreateAccount)      │
├─────────────────────────────────────────────┤
│     1. Enterprise Business Rules            │  ← Entities
│    (Account, Transaction, MonetaryAmount)   │
└─────────────────────────────────────────────┘
        ↑ DEPENDENCY RULE ↑
   (Todas as setas apontam para dentro)
```

## Estrutura do Código

```
src/main/java/com/github/msvidal/transactionauthorizer/

├── domain/                          # 🟢 CAMADA 1: ENTITIES
│   ├── model/                       # Entidades de negócio PURAS
│   │   ├── Account.java             # ✅ Record imutável (Java 17)
│   │   ├── Transaction.java         # ✅ Record imutável
│   │   ├── MonetaryAmount.java      # ✅ Value Object
│   │   ├── TransactionType.java     # ✅ Enum
│   │   └── Status.java              # ✅ Enum
│   │
│   ├── exception/                   # Exceções de domínio
│   │   └── InsufficientBalanceException.java
│   │
│   ├── repository/                  # 🔌 INTERFACES (Gateways)
│   │   ├── AccountRepository.java   # ✅ Contrato (não implementação)
│   │   └── TransactionRepository.java
│   │
│   └── service/                     # Lógica de domínio pura
│       ├── TransactionOperation.java
│       ├── DebitOperation.java
│       ├── CreditOperation.java
│       └── TransactionStrategyFactory.java
│
├── application/                     # 🔵 CAMADA 2: USE CASES
│   └── usecase/
│       ├── ProcessTransactionUseCase.java  # ✅ Orquestra regras
│       └── CreateAccountUseCase.java       # ✅ Orquestra regras
│
└── infra/                           # 🟠 CAMADAS 3 e 4: ADAPTERS + FRAMEWORKS
    ├── api/                         # Interface Adapter (Web)
    │   ├── TransactionController.java  # Framework (Spring MVC)
    │   ├── GlobalExceptionHandler.java
    │   └── dto/                        # DTOs de entrada/saída
    │       ├── TransactionRequest.java
    │       ├── TransactionResponse.java
    │       └── TransactionAmountRequest.java
    │
    ├── persistence/                 # Interface Adapter (Database)
    │   ├── account/
    │   │   ├── AccountEntity.java      # Framework (JPA)
    │   │   ├── AccountJpaRepository.java
    │   │   └── AccountPersistenceAdapter.java  # Implementa AccountRepository
    │   └── transaction/
    │       ├── TransactionEntity.java
    │       ├── TransactionJpaRepository.java
    │       └── TransactionPersistenceAdapter.java
    │
    ├── messaging/                   # Interface Adapter (Queue)
    │   ├── AccountSqsListener.java     # Framework (Spring Cloud AWS)
    │   └── dto/
    │       └── AccountDto.java
    │
    └── config/                      # Framework (Spring Configuration)
        ├── SqsConfig.java
        └── OpenApiConfig.java
```

## Princípios da Clean Architecture Aplicados

### 1. **Dependency Rule (Regra de Dependência)**

> **"O código fonte só pode apontar para DENTRO"**

```
Externo → Interno (permitido ✅)
Interno → Externo (PROIBIDO ❌)
```

#### Exemplo no Código:

```java
// ❌ ERRADO:  Domain depende de framework
package com.github.msvidal. transactionauthorizer.domain. model;

import jakarta.persistence. Entity;  // 💥 JPA no domain! 

@Entity
public class Transaction {
    // ... 
}
```

```java
// ✅ CERTO: Domain puro (sem imports externos)
package com.github.msvidal.transactionauthorizer. domain.model;

public record Transaction(
    UUID id,
    UUID accountId,
    MonetaryAmount amount,
    TransactionType operation,
    Status status,
    OffsetDateTime createdAt
) {
    public Transaction markSucceeded() { /* ... */ }
    public Transaction markFailed() { /* ... */ }
}
```

```java
// ✅ Adapter converte Domain ↔ Framework
package com.github.msvidal.transactionauthorizer.infra.persistence;

import jakarta.persistence.*;  // ✅ JPA só no adapter! 

@Entity
@Table(name = "transactions")
public class TransactionEntity {
    @Id private UUID id;
    @Column private UUID accountId;
    // ... 
    
    // Método de conversão
    public Transaction toDomain() {
        return new Transaction(id, accountId, ... );
    }
}
```

---

### 2. **Entities (Camada 1) - Regras de Negócio Críticas**

**Características:**
- ✅ **Independentes de frameworks** (sem anotações `@Entity`, `@Column`)
- ✅ **Imutáveis** (usando `record` do Java 17)
- ✅ **Contém lógica de negócio** (ex:  validações, cálculos)

#### Exemplo:

```java
// domain/model/Account.java
public record Account(
    UUID id,
    String owner,
    MonetaryAmount balance,
    OffsetDateTime createdAt,
    String status
) {
    // ✅ Regra de negócio no próprio domain
    public Account debit(MonetaryAmount amount) {
        if (balance.isLessThan(amount)) {
            throw new InsufficientBalanceException(
                "Saldo insuficiente:  " + balance + " < " + amount
            );
        }
        return new Account(id, owner, balance.subtract(amount), createdAt, status);
    }

    public Account credit(MonetaryAmount amount) {
        return new Account(id, owner, balance.add(amount), createdAt, status);
    }
}
```

**Value Object:**
```java
// domain/model/MonetaryAmount.java
public record MonetaryAmount(BigDecimal value, Currency currency) {
    // ✅ Validações no construtor
    public MonetaryAmount {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor não pode ser negativo");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Moeda é obrigatória");
        }
    }

    public boolean isLessThan(MonetaryAmount other) {
        validateSameCurrency(other);
        return this.value.compareTo(other. value) < 0;
    }

    public MonetaryAmount subtract(MonetaryAmount other) {
        validateSameCurrency(other);
        return new MonetaryAmount(this.value.subtract(other. value), currency);
    }
}
```

---

### 3. **Use Cases (Camada 2) - Orquestração de Regras**

**Características:**
- ✅ **Contém lógica de aplicação** (não de negócio)
- ✅ **Orquestra Entities + Repositories**
- ✅ **Independente de frameworks** (pode ter `@Service` mas sem lógica Spring-specific)

#### Exemplo:

```java
// application/usecase/ProcessTransactionUseCase. java
@Service
@AllArgsConstructor
public class ProcessTransactionUseCase {

    // ✅ Depende de interfaces (não de implementações)
    private final TransactionStrategyFactory strategyFactory;
    private final AccountRepository accountRepository;  // ← INTERFACE
    private final TransactionRepository transactionRepository;

    @Transactional
    @Retryable(includes = OptimisticLockException.class)
    public TransactionResult execute(Transaction transaction) {
        // 1️⃣ Busca conta (usa gateway)
        var accountOpt = accountRepository.findById(transaction.accountId());
        if (accountOpt.isEmpty()) {
            var failedTransaction = transactionRepository.save(
                transaction.markFailed()
            );
            return new TransactionResult(failedTransaction, null);
        }

        var account = accountOpt.get();

        // 2️⃣ Idempotência (verifica se já processou)
        var transactionOpt = transactionRepository. findById(transaction.id());
        if (transactionOpt. isPresent()) {
            return new TransactionResult(transactionOpt.get(), account);
        }

        // 3️⃣ Aplica regra de negócio (DEBIT ou CREDIT)
        try {
            var accountUpdated = strategyFactory
                .get(transaction.operation())  // ← Strategy Pattern
                .execute(account, transaction.amount());

            var accountSaved = accountRepository.update(accountUpdated);
            var transactionSaved = transactionRepository.save(
                transaction.markSucceeded()
            );

            return new TransactionResult(transactionSaved, accountSaved);
        } catch (InsufficientBalanceException e) {
            // 4️⃣ Trata falha de negócio
            var failedTransaction = transactionRepository.save(
                transaction.markFailed()
            );
            return new TransactionResult(failedTransaction, account);
        }
    }
}
```

**Por que não é Hexagonal puro?**
- ❌ Hexagonal focaria em "Ports" (interfaces de entrada E saída)
- ✅ Clean Architecture foca em **Use Cases** como orquestradores principais

---

### 4. **Interface Adapters (Camada 3) - Conversão de Dados**

**Responsabilidade:**
- Converter DTOs externos ↔ Entities do domínio
- Implementar interfaces (gateways) definidas no domain

#### Adapter de Persistência:

```java
// infra/persistence/transaction/TransactionPersistenceAdapter.java
@Component
@RequiredArgsConstructor
public class TransactionPersistenceAdapter implements TransactionRepository {

    private final TransactionJpaRepository jpaRepository;  // ← Spring Data JPA

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity entity = toEntity(transaction);  // ← Conversão
        TransactionEntity saved = jpaRepository.save(entity);
        return toDomain(saved);  // ← Conversão de volta
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(this::toDomain);
    }

    // ✅ Métodos de conversão isolados
    private TransactionEntity toEntity(Transaction transaction) {
        TransactionEntity entity = new TransactionEntity();
        entity.setId(transaction.id());
        entity.setAccountId(transaction.accountId());
        entity.setAmount(transaction.amount().value());
        entity.setCurrency(transaction.amount().currency().getCurrencyCode());
        entity.setStatus(transaction.status());
        return entity;
    }

    private Transaction toDomain(TransactionEntity entity) {
        return new Transaction(
            entity.getId(),
            entity.getAccountId(),
            new MonetaryAmount(
                entity.getAmount(),
                Currency.getInstance(entity.getCurrency())
            ),
            entity.getOperation(),
            entity. getStatus(),
            entity.getCreatedAt()
        );
    }
}
```

#### Adapter de API (Web):

```java
// infra/api/TransactionController. java
@RestController
@RequestMapping("/transactions")
@AllArgsConstructor
public class TransactionController {

    private final ProcessTransactionUseCase processTransactionUseCase;

    @PostMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> authorize(
        @PathVariable UUID transactionId,
        @RequestBody @Valid TransactionRequest request
    ) {
        // ✅ Converte DTO → Domain
        var amount = new MonetaryAmount(
            request.amount().value(),
            Currency.getInstance(request.amount().currency())
        );
        
        var transaction = new Transaction(
            transactionId,
            request.accountId(),
            amount,
            TransactionType.fromString(request.operation()),
            null,
            null
        );

        // ✅ Chama Use Case (camada 2)
        var result = processTransactionUseCase. execute(transaction);

        // ✅ Converte Domain → DTO
        TransactionResponse response = TransactionResponse.toResponse(result);
        
        return ResponseEntity.ok(response);
    }
}
```

---

### 5. **Frameworks & Drivers (Camada 4) - Detalhes Técnicos**

**O que fica aqui:**
- Spring Boot
- JPA/Hibernate
- PostgreSQL JDBC Driver
- AWS SQS SDK
- Swagger/OpenAPI

**Exemplo:**
```java
// infra/config/SqsConfig.java
@Configuration
@Profile("listener")
public class SqsConfig {

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder()
            .region(Region.of("sa-east-1"))
            .build();
    }

    @Bean
    public SqsTemplate sqsTemplate(SqsAsyncClient client) {
        return SqsTemplate.newTemplate(client);
    }
}
```

---

## Dependency Inversion Principle (DIP)

**Problema sem Clean Architecture:**
```java
// ❌ Use Case depende de implementação concreta
public class ProcessTransactionUseCase {
    private final TransactionJpaRepository repository;  // 💥 Acoplamento! 
}
```

**Solução com Clean Architecture:**
```java
// ✅ Use Case depende de abstração (interface no domain)
public class ProcessTransactionUseCase {
    private final TransactionRepository repository;  // ← INTERFACE
}

// Implementação fica no adapter
@Component
public class TransactionPersistenceAdapter implements TransactionRepository {
    // Usa JPA aqui
}
```

**Diagrama de Dependências:**
```
┌────────────────────────────────────────┐
│  TransactionController (infra/api)    │
└──────────────┬─────────────────────────┘
               │ depends on
               ↓
┌────────────────────────────────────────┐
│  ProcessTransactionUseCase (app)      │
└──────────────┬─────────────────────────┘
               │ depends on
               ↓
┌────────────────────────────────────────┐
│  TransactionRepository (domain)       │  ← INTERFACE
└────────────────────────────────────────┘
               ↑ implements
               │
┌────────────────────────────────────────┐
│  TransactionPersistenceAdapter (infra)│
└────────────────────────────────────────┘
```

---

## Benefícios da Clean Architecture

### 1. **Testabilidade**

```java
// ✅ Teste UNITÁRIO sem Spring/JPA
@Test
void deveReprovarTransacaoComSaldoInsuficiente() {
    // Arrange - Usa mocks para repositories
    var accountRepo = mock(AccountRepository.class);
    var transactionRepo = mock(TransactionRepository.class);
    
    var account = new Account(
        UUID.randomUUID(),
        "João Silva",
        new MonetaryAmount(new BigDecimal("50.00"), BRL),
        OffsetDateTime.now(),
        "ENABLED"
    );
    
    when(accountRepo.findById(any())).thenReturn(Optional.of(account));
    
    // Act
    var useCase = new ProcessTransactionUseCase(accountRepo, transactionRepo, ... );
    var transaction = new Transaction(
        UUID.randomUUID(),
        account.id(),
        new MonetaryAmount(new BigDecimal("100.00"), BRL),
        TransactionType.DEBIT,
        null,
        null
    );
    
    var result = useCase.execute(transaction);
    
    // Assert
    assertEquals(Status.FAILED, result.transaction().status());
    verify(transactionRepo).save(argThat(t -> t.status() == Status.FAILED));
}
```

**Vantagens:**
- ⚡ Teste roda em **< 50ms** (sem Spring Context)
- ✅ Não precisa de banco de dados
- ✅ Foca na lógica de negócio

---

### 2. **Independência de Framework**

**Cenário:** Migrar de Spring Boot para Quarkus

```diff
# Código que NÃO muda: 
+ domain/*       (100% reutilizado)
+ application/*  (100% reutilizado)

# Código que muda:
- infra/api/*         (Trocar @RestController por @Path)
- infra/config/*      (Configuração diferente)
- infra/persistence/* (JPA continua funcionando)
```

---

### 3. **Evolução Sem Quebrar Regras de Negócio**

**Exemplo:** Trocar PostgreSQL por MongoDB

```java
// ✅ Criar novo adapter (domain NÃO muda)
@Component
public class TransactionMongoAdapter implements TransactionRepository {
    
    private final MongoTemplate mongoTemplate;

    @Override
    public Transaction save(Transaction transaction) {
        TransactionDocument doc = toDocument(transaction);
        mongoTemplate.save(doc);
        return transaction;
    }
    
    // ... conversões
}
```

**No Spring:**
```java
@Configuration
@Profile("mongodb")  // ← Escolhe adapter via profile
public class MongoConfig {
    @Bean
    public TransactionRepository transactionRepository() {
        return new TransactionMongoAdapter(... );
    }
}
```

---

## Consequências

### Positivas ✅

| Benefício                 | Descrição                                      |
|---------------------------|------------------------------------------------|
| **Testabilidade**         | Domain testável sem infraestrutura             |
| **Manutenibilidade**      | Regras de negócio isoladas em 1 lugar         |
| **Flexibilidade**         | Trocar frameworks sem reescrever lógica        |
| **Onboarding**            | Novos devs entendem fluxo via Use Cases       |
| **Conformidade**          | Auditores validam `domain/` sem olhar infra   |

### Negativas ⚠️

| Desafio                   | Impacto                                        |
|---------------------------|------------------------------------------------|
| **Boilerplate**           | Mappers (DTO ↔ Domain ↔ Entity)                |
| **Curva de aprendizado**  | Devs juniores precisam entender camadas       |
| **Over-engineering**      | Para CRUDs simples pode ser excessivo          |

### Mitigações

1. **Reduzir Boilerplate:**
    - Usar `record` do Java 17 (menos código)
    - Libraries como MapStruct para mappers

2. **Documentação:**
    - Criar guia de onboarding explicando camadas
    - Diagramas C4 Model

3. **Avaliar Necessidade:**
    - Usar Clean Architecture em **módulos críticos** (transações, pagamentos)
    - CRUD simples pode ser apenas JPA direto

---

## Referências

- [Robert C. Martin - Clean Architecture Book](https://www.amazon.com/Clean-Architecture-Craftsmans-Software-Structure/dp/0134494164)
- [The Clean Architecture (Blog Post)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Clean Architecture in Spring Boot](https://medium.com/@aakankshadev99/clean-architecture-with-spring-boot-9e3c3c1f0bdb)

---

**Data:** 2025-12-20  
**Autores:** @msvidal  
**Revisores:** Equipe de Arquitetura