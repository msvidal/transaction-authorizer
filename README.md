# 💳 Transaction Authorizer

Sistema de autorização de transações financeiras com suporte a débito e crédito, construído com Spring Boot 4 e arquitetura limpa.

## 🚀 Funcionalidades

- ✅ Processamento de transações de **débito** e **crédito**
- ✅ **Idempotência** baseada em UUID de transação
- ✅ **Controle de concorrência** com Optimistic Locking
- ✅ Validação de saldo antes de débitos
- ✅ Consumo de eventos SQS para criação de contas
- ✅ API REST documentada com **Swagger/OpenAPI**
- ✅ Tratamento de DLQ (Dead Letter Queue)

## 🏗️ Arquitetura

O projeto segue os princípios de **Clean Architecture** e **Domain-Driven Design**:

```
📦 transaction-authorizer
├── 🎯 domain/           # Modelos de domínio e regras de negócio
├── 🔧 application/      # Casos de uso (Use Cases)
├── 🌐 infra/            # Infraestrutura (API, DB, Mensageria)
└── ✅ test/             # Testes unitários e de integração
```

### Padrões de Design

- **Strategy Pattern** → Operações de transação (Débito/Crédito)
- **Repository Pattern** → Abstração de persistência
- **Adapter Pattern** → Conversão Domain ↔ Infrastructure
- **Use Case Pattern** → Lógica de aplicação isolada

## 🛠️ Stack Tecnológica

| Tecnologia | Versão | Descrição |
|------------|--------|-----------|
| Java | 17 | Linguagem de programação |
| Spring Boot | 4.0.1 | Framework principal |
| Spring Data JPA | 4.0.1 | Persistência |
| PostgreSQL | 15 | Banco de dados |
| AWS SQS | - | Mensageria |
| Docker | - | Containerização |
| LocalStack | - | Emulação AWS local |

## 📋 Pré-requisitos

- **Java 17+**
- **Docker** e **Docker Compose**
- **Maven 3.9+** (opcional, wrapper incluído)

## 🚀 Quick Start

### 1️⃣ Clone o repositório

```bash
git clone https://github.com/msvidal/transaction-authorizer.git
cd transaction-authorizer
```

### 2️⃣ Inicie os serviços com Docker

```bash
chmod +x build-and-run.sh
./build-and-run.sh
```

Ou manualmente:

```bash
docker-compose up -d --build
```

### 3️⃣ Aguarde a inicialização (~30s)

```bash
docker-compose logs -f api
```

### 4️⃣ Acesse a aplicação

| Serviço | URL | Descrição |
|---------|-----|-----------|
| 🌐 API | http://localhost:8080 | Aplicação principal |
| 📚 Swagger UI | http://localhost:8080/swagger-ui.html | Documentação interativa |
| 📊 Actuator | http://localhost:8080/actuator | Endpoints de monitoramento |

## 📖 Uso da API

### Criar uma conta (via SQS)

```bash
aws --endpoint-url=http://localhost:4566 sqs send-message \
  --queue-url http://localhost:4566/000000000000/account-events \
  --message-body '{
    "account":  {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "owner": "123e4567-e89b-12d3-a456-426614174000",
      "balance":  1000.00,
      "createdAt": 1735000000
    }
  }'
```

### Processar transação de DÉBITO

```bash
curl -X POST http://localhost:8080/transactions/7c9e6679-7425-40de-944b-e07fc1f90ae7 \
  -H "Content-Type: application/json" \
  -d '{
    "account_id": "550e8400-e29b-41d4-a716-446655440000",
    "amount": {
      "value": 50.00,
      "currency":  "BRL"
    },
    "operation": "DEBIT"
  }'
```

**Resposta:**

```json
{
  "transaction": {
    "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "operation": "DEBIT",
    "amount":  {
      "value": 50.00,
      "currency":  "BRL"
    },
    "status": "SUCCEEDED",
    "created_at": "2025-12-20T21:30:00-03:00"
  },
  "account": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "balance":  {
      "value": 950.00,
      "currency":  "BRL"
    }
  }
}
```

### Processar transação de CRÉDITO

```bash
curl -X POST http://localhost:8080/transactions/8d8f7780-8536-51ef-b95c-f18gd2g01bf8 \
  -H "Content-Type: application/json" \
  -d '{
    "account_id": "550e8400-e29b-41d4-a716-446655440000",
    "amount": {
      "value": 200.00,
      "currency":  "BRL"
    },
    "operation": "CREDIT"
  }'
```

### Idempotência

Enviar a mesma transação (mesmo UUID) múltiplas vezes retorna o mesmo resultado:

```bash
# Primeira chamada - processa
curl -X POST http://localhost:8080/transactions/SAME-UUID-HERE ... 

# Segunda chamada - retorna resultado anterior (não processa novamente)
curl -X POST http://localhost:8080/transactions/SAME-UUID-HERE ... 
```

## 🏗️ Desenvolvimento Local

### Executar sem Docker

```bash
# 1. Inicie PostgreSQL e LocalStack
docker-compose up -d postgres localstack

# 2. Configure variáveis de ambiente
export SPRING_PROFILES_ACTIVE=api,local
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/transactiondb
export SPRING_DATASOURCE_USERNAME=user
export SPRING_DATASOURCE_PASSWORD=password
export AWS_SQS_ENDPOINT=http://localhost:4566

# 3. Execute a aplicação
./mvnw spring-boot:run
```

### Executar Testes

```bash
# Todos os testes
./mvnw test

# Testes específicos
./mvnw test -Dtest=ProcessTransactionUseCaseTest

# Com coverage
./mvnw test jacoco:report
```

## 🗂️ Estrutura do Projeto

```
src/
├── main/
│   ├── java/. ../transactionauthorizer/
│   │   ├── TransactionAuthorizerApplication.java
│   │   ├── domain/
│   │   │   ├── exception/
│   │   │   ├── model/         # Account, Transaction, MonetaryAmount
│   │   │   ├── repository/    # Interfaces
│   │   │   └── service/       # Strategy Pattern
│   │   ├── application/
│   │   │   └── usecase/       # ProcessTransaction, CreateAccount
│   │   └── infra/
│   │       ├── api/           # Controllers, DTOs
│   │       ├── persistence/   # JPA Entities, Adapters
│   │       ├── messaging/     # SQS Listener
│   │       └── config/        # Configurações
│   └── resources/
│       ├── application.yml
│       ├── application-api.yml
│       ├── application-listener.yml
│       └── application-local.yml
└── test/
    └── java/.../transactionauthorizer/
        ├── application/
        ├── domain/
        └── infra/
```

## 🔧 Configuração

### Variáveis de Ambiente

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `SPRING_PROFILES_ACTIVE` | Profiles ativos | `api` |
| `SPRING_DATASOURCE_URL` | URL do PostgreSQL | - |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco | `user` |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco | `password` |
| `AWS_SQS_ENDPOINT` | Endpoint SQS (LocalStack) | - |
| `AWS_SQS_QUEUE_NAME` | Nome da fila SQS | `account-events` |
| `AWS_SQS_DLQ_NAME` | Nome da DLQ | `account-events-dlq` |
| `AWS_REGION` | Região AWS | `us-east-1` |

### Profiles Spring

- **`api`** - Expõe API REST
- **`listener`** - Consome mensagens SQS
- **`local`** - Ambiente de desenvolvimento (api + listener)

## 🐳 Docker Compose

O projeto inclui os seguintes serviços:

```yaml
services:
  - postgres      # Banco de dados
  - localstack    # Emulação AWS (SQS)
  - api           # API REST
  - listener      # Consumidor SQS
```

## 🧪 Testes

### Cobertura de Testes

- ✅ Testes unitários (Use Cases)
- ✅ Testes de repository adapters
- ✅ Testes de mensageria (SQS)
- ✅ Testes de validação

### Executar com Coverage

```bash
./mvnw clean test jacoco:report
open target/site/jacoco/index. html
```

## 🔐 Segurança

⚠️ **IMPORTANTE**: Este projeto é uma **demonstração** e não deve ser usado em produção sem:

- Implementar autenticação/autorização (Spring Security, OAuth2)
- Adicionar rate limiting
- Implementar auditoria completa

## 📈 Performance

### Otimizações Implementadas

- ✅ Optimistic Locking (evita locks pessimistas)
- ✅ Retry automático em conflitos
- ✅ Idempotência (evita processamento duplicado)
- ✅ Índices de banco de dados
- ✅ Connection pooling (HikariCP)
- ✅ Docker multi-stage build

## 👤 Autor

**msvidal**
- GitHub: [@msvidal](https://github.com/msvidal)
