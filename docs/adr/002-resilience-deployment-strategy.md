# ADR 003: Estratégias de Resiliência, Deploy em Cloud e Pipeline CI/CD

## Status
✅ Proposto

## Contexto
O sistema de autorização de transações financeiras precisa:
- **Alta disponibilidade** e tolerância a falhas em serviços externos (PostgreSQL, SQS)
- **Estratégia de deployment** que minimize impacto de bugs em produção
- **Infraestrutura cloud-native** escalável e resiliente
- **Pipeline automatizado** com validações de qualidade e segurança

## Decisão

Implementar uma arquitetura resiliente baseada em:
1. **Padrões de resiliência** (Retry, Backoff, Circuit Breaker, Bulkhead, Rate Limiting)
2. **Infraestrutura AWS** com Kubernetes (EKS) e componentes gerenciados
3. **Pipeline CI/CD** com GitHub Actions e estratégia Canary Deployment
4. **Observabilidade** completa (métricas, logs, tracing)

---

## 1. Padrões de Resiliência

### 1.1 Retry com Exponential Backoff

#### ✅ Implementado

**Motivação:**
- Evita que todos os clientes retentem ao mesmo tempo após falha
- Reduz pressão no banco de dados durante recuperação
- Melhora taxa de sucesso em cenários de alta concorrência

---

### 1.2 Timeout e Deadline Propagation

#### 🟡 Parcialmente implementado

**Motivação:**
- Evita requisições "penduradas" consumindo recursos
- Melhora experiência do usuário (fail-fast)
- Facilita identificação de queries lentas

---

## 2. Arquitetura de Deployment em Cloud Pública (AWS)

### 2.1 Diagrama de Infraestrutura

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Internet / Clients                          │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   API Gateway (REST API)                            │
│   [Authentication, Authorization, Request/Response Transformation]  │
│            [Usage Plans, API Keys, Throttling]                      │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│        Application Load Balancer (ALB) - Multi-AZ                   │
│         [Health Checks, SSL Termination, Target Groups]             │
└───────────────┬────────────────────────────────────────────────────-┘
                │
      ┌─────────┴─────────┐
      │                   │
      ▼                   ▼
┌─────────────┐     ┌─────────────┐
│   AZ-1a     │     │   AZ-1b     │
│             │     │             │
│  ┌───────┐  │     │  ┌───────┐  │
│  │ EKS   │  │     │  │ EKS   │  │
│  │ Pod 1 │  │     │  │ Pod 5 │  │
│  └───┬───┘  │     │  └───┬───┘  │
│      │      │     │      │      │
│  ┌───┴───┐  │     │  ┌───┴───┐  │
│  │ EKS   │  │     │  │ EKS   │  │
│  │ Pod 2 │  │     │  │ Pod 6 │  │
│  └───────┘  │     │  └───────┘  │
│             │     │             │
│  ┌───────┐  │     │  ┌───────┐  │
│  │ EKS   │  │     │  │ EKS   │  │
│  │ Pod 3 │  │     │  │ Pod 7 │  │
│  └───────┘  │     │  └───────┘  │
│             │     │             │
│  ┌───────┐  │     │  ┌───────┐  │
│  │ EKS   │  │     │  │ EKS   │  │
│  │ Pod 4 │  │     │  │ Pod 8 │  │
│  └───────┘  │     │  └───────┘  │
└──────┬──────┘     └──────┬──────┘
       │                   │
       └───────────┬───────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Amazon EKS Cluster                               │
│              [Auto Scaling:  4-20 pods based on CPU/Memory]         │
│       Pod Spec:  1 vCPU, 2GB RAM, Java 17 Container                 │
│       Node Group: m5.xlarge (4 vCPU, 16GB RAM) - 2-6 nodes          │
└────────────────────────────┬────────────────────────────────────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
┌─────────────────┐  ┌──────────────┐  ┌─────────────────┐
│   RDS Aurora    │  │  Amazon SQS  │  │   Secrets       │
│   PostgreSQL    │  │              │  │   Manager       │
│   Multi-AZ      │  │ ┌──────────┐ │  │                 │
│                 │  │ │  Queue:  │ │  │  ┌──────────┐   │
│ ┌────────────┐  │  │ │  conta-  │ │  │  │ DB Creds │   │
│ │  Primary   │  │  │ │ bancaria │ │  │  └──────────┘   │
│ │ (AZ-1a)    │  │  │ │ -criada  │ │  │                 │
│ └─────┬──────┘  │  │ └──────────┘ │  │  ┌──────────┐   │
│       │         │  │              │  │  │ API Keys │   │
│       ▼         │  │ ┌──────────┐ │  │  └──────────┘   │
│ ┌────────────┐  │  │ │   DLQ    │ │  │                 │
│ │  Replica   │  │  │ │          │ │  └─────────────────┘
│ │ (AZ-1b)    │  │  │ │          │ │
│ └────────────┘  │  │ └──────────┘ │
│                 │  │              │
│ Auto Backup     │  │ FIFO Queue   │
│ Point-in-Time   │  │ Dead Letter  │
│ Recovery        │  │ Queue        │
└─────────────────┘  └──────────────┘
```

## 3. Pipeline CI/CD com GitHub Actions e Canary Deployment

### 3.1 Estrutura do Pipeline

### 3.2 Implementação com GitHub Actions

### 3.3 Configuração Flagger para Canary Deployment

### 3.4 Alarmes CloudWatch para Rollback Automático

---

## 4. Observabilidade e Monitoramento

### 4.1 Métricas Custom (Prometheus)

### 4.2 Stack de Observabilidade (Prometheus + Grafana)

### 4.3 Distributed Tracing (AWS X-Ray)

## 5. Segurança

### 5.1 Autenticação e Autorização

### 5.2 Secrets Management com Kubernetes + AWS Secrets Manager

---

## Consequências

### Positivas ✅
- **Resiliência:** Sistema tolera falhas de AZ, banco, mensageria
- **Deploys seguros:** Canary deployment protege clientes de bugs
- **Observabilidade:** Métricas e traces permitem debug rápido
- **Escalabilidade:** Auto-scaling horizontal em todos os componentes
- **Flexibilidade:** Kubernetes permite migração entre clouds

### Negativas ⚠️
- **Complexidade:** Kubernetes adiciona overhead operacional
- **Custo:** ~$1100/mês para infraestrutura completa
- **Latência:** Circuit breaker/retry adicionam overhead
- **Curva de aprendizado:** Equipe precisa conhecer Kubernetes + AWS

### Mitigações
- **Infraestrutura como Código:** Terraform para reprodutibilidade
- **Runbooks:** Documentação de incidentes e troubleshooting
- **FinOps:** Revisão mensal de custos e otimizações

---

## Referências

- [AWS Well-Architected Framework](https://aws.amazon.com/architecture/well-architected/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Flagger Progressive Delivery](https://flagger.app/)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Google SRE Book - Handling Overload](https://sre.google/sre-book/handling-overload/)

---

**Data:** 2025-12-21  
**Autor:** msvidal  
**Revisores:** [Pendente]  
**Status:** Proposto ✅