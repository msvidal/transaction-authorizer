# 🚀 Load Testing - Transaction Authorizer

Documentação e resultados dos testes de carga do sistema de autorização de transações.

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Configuração](#configuração)
- [Executando os Testes](#executando-os-testes)
- [Resultados](#resultados)
- [Scripts Disponíveis](#scripts-disponíveis)

## 🎯 Visão Geral

Este diretório contém:
- Scripts de testes de carga (k6)
- Scripts de validação de saldos
- Resultados históricos dos testes
- Documentação de benchmarks

## ⚙️ Configuração

### Pré-requisitos

```bash
# Instalar k6
brew install k6  # macOS
# ou
sudo apt-get install k6  # Linux
# ou
choco install k6  # Windows

# Instalar Python (para scripts de validação)
python3 --version  # >= 3.8
pip3 install requests
```

### Preparação do Ambiente

```bash
# 1. Iniciar a aplicação localmente
./mvnw spring-boot:run

# 2. Verificar se está respondendo
curl http://localhost:8080/actuator/health
```

## 🏃 Executando os Testes

### Teste Básico (50 VUs, 2 minutos)

```bash
cd docs/load-testing/scripts
k6 run load-test.js
```

### Teste com Mais Carga (100 VUs)

```bash
k6 run --vus 100 --duration 5m load-test.js
```

### Validar Saldos Após o Teste

```bash
python3 validate_balances.py
```

## 📊 Resultados

### Último Teste

| Data | VUs | Requisições | Taxa Sucesso | P95 Latência | Throughput |
|------|-----|-------------|--------------|--------------|------------|
| 2025-12-20 | 50 | 2.684 | 100% | 15.42ms | 22.23 req/s |

📄 [Ver relatório completo](results/2025-12-20-baseline.md)

### Benchmarks Atuais

- **Throughput**: ~22 req/s (50 VUs)
- **Latência P95**: 15.42ms
- **Capacidade**: 50+ usuários concorrentes
- **Disponibilidade**: 100%

## 📁 Scripts Disponíveis

### `load-test.js`
Teste de carga padrão com rampa de usuários (0 → 50 → 0 VUs).

**Uso:**
```bash
k6 run load-test.js
```

### `validate_balances.py`
Valida a integridade dos saldos após os testes.

**Uso:**
```bash
python3 validate_balances.py
```

## 📈 Métricas Importantes

### Thresholds Configurados

- `http_req_duration`: p(95) < 500ms
- `http_req_failed`: rate < 1%

### SLAs (Service Level Agreements)

- **Disponibilidade**: 99.9%
- **Latência P95**: < 100ms
- **Throughput Mínimo**: 10 req/s

## 🔧 Troubleshooting

### Erro de Conexão

```bash
# Verificar se a aplicação está rodando
curl http://localhost:8080/actuator/health
```

### Performance Degradada

1.  Verificar uso de CPU/memória
2. Analisar logs da aplicação
3. Verificar conexões com banco de dados
4. Revisar índices no banco

## 📚 Referências

- [k6 Documentation](https://k6.io/docs/)
- [Load Testing Best Practices](https://k6.io/docs/testing-guides/test-types/)
- [Testing HTTP APIs](https://k6.io/docs/examples/http-authentication/)

## 👥 Contribuindo

Para adicionar novos testes:

1. Criar script em `scripts/`
2. Executar e documentar resultados em `results/YYYY-MM-DD-description.md`
3. Atualizar este README com o novo benchmark

---

**Última Atualização**: 2025-12-20  
**Mantido por**: msvidal