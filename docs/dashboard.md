# Dashboard Administrativo — Pediu Patrão

## 1. Apresentação

O Dashboard Administrativo foi desenvolvido para o sistema Pediu Patrão, uma aplicação de gerenciamento de pedidos de pizzaria criada na disciplina de Engenharia de Software.

O objetivo do painel é permitir que administradores e gerentes consultem informações sobre vendas, pedidos e produtos, ajudando a acompanhar os resultados do estabelecimento.

O dashboard utiliza dados armazenados no MongoDB, disponibilizados por uma API REST em Spring Boot e apresentados na interface com Thymeleaf, JavaScript, Bootstrap e Chart.js.

- **Perfis autorizados:** ADMIN e GERENTE

## 2. Tecnologias utilizadas

- Java 21
- Spring Boot
- Spring Security
- MongoDB
- Thymeleaf

## 3. Requisitos funcionais

### RF01 — Indicadores gerais de vendas

O dashboard apresenta os seguintes indicadores:

- Total de pedidos.
- Pedidos finalizados.
- Pedidos cancelados.
- Faturamento.
- Ticket médio.

O faturamento considera apenas os pedidos finalizados.

O ticket médio é calculado dividindo o faturamento pela quantidade de pedidos finalizados.

### RF02 — Análise de vendas

O dashboard apresenta dois gráficos:

- **Gráfico de barras:** faturamento diário.
- **Gráfico de linhas:** quantidade de pedidos finalizados por dia.

Os gráficos permitem visualizar o desempenho das vendas ao longo de um período.

### RF03 — Situação dos pedidos

O dashboard apresenta um gráfico de rosca com a distribuição dos pedidos por status.

Os status considerados são:

- RECEBIDO
- EM_PREPARO
- PRONTO
- SAIU_PARA_ENTREGA
- FINALIZADO
- CANCELADO

O gráfico permite acompanhar a situação atual dos pedidos.

### RF04 — Produtos mais vendidos

O dashboard apresenta uma tabela com o ranking dos cinco produtos mais vendidos.

A tabela apresenta:

- Posição no ranking.
- Nome do produto.
- Quantidade vendida.

O ranking considera apenas os produtos de pedidos finalizados.

### RF05 — Filtros de consulta

O dashboard permite consultar os dados por período.

O usuário pode informar:

- Data inicial.
- Data final.

As datas são utilizadas para consultar os indicadores, os gráficos e o ranking de produtos.

A aplicação valida se a data inicial é anterior ou igual à data final.

### RF06 — Integração com a API

O dashboard utiliza JavaScript para realizar requisições HTTP aos endpoints da API.

Os dados são obtidos do back-end Spring Boot e utilizados para atualizar os elementos da interface.

### RF07 — Atualização e tratamento de dados

O dashboard permite atualizar os dados sem precisar recarregar a página inteira.

Também apresenta mensagens para:

- Carregamento das informações.
- Falhas de conexão.
- Períodos sem pedidos.
- Filtros inválidos.

Após uma nova consulta bem-sucedida, as mensagens de erro anteriores são removidas.

## 4. Requisitos não funcionais

### RNF01 — Segurança

O acesso aos endpoints do dashboard é restrito aos usuários autenticados com os perfis `ADMIN` e `GERENTE`.

A autorização é realizada utilizando Spring Security.

### RNF02 — Usabilidade

A interface apresenta informações organizadas em indicadores, gráficos e tabela.

As mensagens de erro e os filtros utilizam textos claros para facilitar a utilização.

### RNF03 — Responsividade

A interface utiliza Bootstrap para adaptar os elementos a diferentes tamanhos de tela.

### RNF04 — Manutenibilidade

O código está organizado em camadas:

- Controller
- Service
- Repository
- DTO
- Interface

Essa organização facilita a manutenção e a implementação de novas funcionalidades.

### RNF05 — Integridade dos dados

Os endpoints do dashboard realizam apenas consultas.

As operações não modificam os pedidos armazenados no banco de dados.

### RNF06 — Tratamento de falhas

O sistema apresenta mensagens quando ocorrem falhas de conexão, filtros inválidos ou ausência de resultados.

### RNF07 — Testabilidade

As regras de negócio e os endpoints possuem testes automatizados com JUnit 5, Mockito e MockMvc.

## 5. Regras de negócio

As principais regras utilizadas no dashboard são:

1. Apenas pedidos com status `FINALIZADO` entram no cálculo do faturamento.

2. O ticket médio é calculado dividindo o faturamento pela quantidade de pedidos finalizados.

3. Quando não existem pedidos finalizados, o faturamento e o ticket médio são iguais a zero.

4. A análise de vendas considera a data de entrada dos pedidos.

5. Os gráficos de vendas apresentam os dias sem vendas com valores iguais a zero.

6. O ranking de produtos considera apenas itens de pedidos finalizados.

7. Os produtos são ordenados pela quantidade vendida, do maior para o menor.

8. O ranking apresenta até cinco produtos.

9. A data inicial não pode ser posterior à data final.

10. As datas inicial e final devem ser informadas juntas.

11. Quando nenhum período é informado, a análise diária de vendas considera os últimos sete dias.

12. Os demais indicadores, quando consultados sem datas, consideram todo o histórico disponível.

## 6. Documentação dos endpoints

A API do dashboard disponibiliza quatro endpoints para consulta.

**URL base:**

```http
http://localhost:8080/api/dashboard
```

**Método HTTP:** `GET`

**Autorização:** `ADMIN` ou `GERENTE`

### 6.1. Parâmetros de consulta

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| inicio | LocalDate | Não* | Data inicial do período |
| fim | LocalDate | Não* | Data final do período |

**Observação:** os parâmetros são opcionais, mas devem ser informados juntos quando utilizados.

Formato das datas:

```text
AAAA-MM-DD
```

Exemplo:

```text
2026-11-01
```

### 6.2. RF01 — Indicadores gerais

**Endpoint:**

```http
GET /api/dashboard
```

**Exemplo de requisição:**

```http
GET /api/dashboard?inicio=2026-11-01&fim=2026-11-10
```

**Exemplo de resposta — 200 OK:**

```json
{
  "totalPedidos": 3,
  "pedidosFinalizados": 3,
  "pedidosCancelados": 0,
  "faturamento": 196,
  "ticketMedio": 65.33
}
```

**Descrição dos campos:**

| Campo | Descrição |
|---|---|
| totalPedidos | Quantidade total de pedidos |
| pedidosFinalizados | Quantidade de pedidos finalizados |
| pedidosCancelados | Quantidade de pedidos cancelados |
| faturamento | Valor total dos pedidos finalizados |
| ticketMedio | Valor médio dos pedidos finalizados |

### 6.3. RF02 — Análise de vendas

**Endpoint:**

```http
GET /api/dashboard/vendas
```

**Exemplo de requisição:**

```http
GET /api/dashboard/vendas?inicio=2026-11-08&fim=2026-11-10
```

**Exemplo de resposta — 200 OK:**

```json
[
  {
    "data": "2026-11-01",
    "pedidosFinalizados": 0,
    "faturamento": 0
  },
  {
    "data": "2026-11-02",
    "pedidosFinalizados": 0,
    "faturamento": 0
  },
  {
    "data": "2026-11-03",
    "pedidosFinalizados": 0,
    "faturamento": 0
  },
  {
    "data": "2026-11-04",
    "pedidosFinalizados": 0,
    "faturamento": 0
  },
  {
    "data": "2026-11-05",
    "pedidosFinalizados": 0,
    "faturamento": 0
  },
  {
    "data": "2026-11-06",
    "pedidosFinalizados": 0,
    "faturamento": 0
  },
  {
    "data": "2026-11-07",
    "pedidosFinalizados": 0,
    "faturamento": 0
  },
  {
    "data": "2026-11-08",
    "pedidosFinalizados": 0,
    "faturamento": 0
  },
  {
    "data": "2026-11-09",
    "pedidosFinalizados": 0,
    "faturamento": 0
  },
  {
    "data": "2026-11-10",
    "pedidosFinalizados": 3,
    "faturamento": 196
  }
]
```

**Descrição dos campos:**

| Campo | Descrição |
|---|---|
| data | Data da consulta |
| pedidosFinalizados | Quantidade de pedidos finalizados no dia |
| faturamento | Faturamento do dia |

Esses dados são utilizados nos gráficos de barras e linhas.

### 6.4. RF03 — Situação dos pedidos

**Endpoint:**

```http
GET /api/dashboard/status
```

**Exemplo de requisição:**

```http
GET /api/dashboard/status?inicio=2026-11-01&fim=2026-11-10
```

**Exemplo de resposta — 200 OK:**

```json
[
  {
    "status": "RECEBIDO",
    "quantidade": 0
  },
  {
    "status": "EM_PREPARO",
    "quantidade": 1
  },
  {
    "status": "PRONTO",
    "quantidade": 0
  },
  {
    "status": "SAIU_PARA_ENTREGA",
    "quantidade": 0
  },
  {
    "status": "FINALIZADO",
    "quantidade": 5
  },
  {
    "status": "CANCELADO",
    "quantidade": 2
  }
]
```

**Descrição dos campos:**

| Campo | Descrição |
|---|---|
| status | Situação atual do pedido |
| quantidade | Quantidade de pedidos naquele status |

Os dados são apresentados em um gráfico de rosca.

### 6.5. RF04 — Produtos mais vendidos

**Endpoint:**

```http
GET /api/dashboard/produtos
```

**Exemplo de requisição:**

```http
GET /api/dashboard/produtos?inicio=2026-11-01&fim=2026-11-10
```

**Exemplo de resposta — 200 OK:**

```json
[
  {
    "status": "RECEBIDO",
    "quantidade": 0
  },
  {
    "status": "EM_PREPARO",
    "quantidade": 0
  },
  {
    "status": "PRONTO",
    "quantidade": 0
  },
  {
    "status": "SAIU_PARA_ENTREGA",
    "quantidade": 0
  },
  {
    "status": "FINALIZADO",
    "quantidade": 3
  },
  {
    "status": "CANCELADO",
    "quantidade": 0
  }
]
```

**Descrição dos campos:**

| Campo | Descrição |
|---|---|
| produtoId | Identificador do produto |
| nomeProduto | Nome do produto |
| quantidadeVendida | Quantidade total de unidades vendidas |

A API retorna até cinco produtos, ordenados pela quantidade vendida.

Quando não existem vendas no período, retorna uma lista vazia:

```json
[]
```

### 6.6. Possíveis erros da API

| Código HTTP | Situação |
|---|---|
| 200 OK | Consulta realizada com sucesso |
| 400 Bad Request | Datas inválidas ou parâmetros incompletos |
| 403 Forbidden | Usuário autenticado sem permissão |
| 500 Internal Server Error | Falha inesperada no servidor |

O acesso sem autenticação pode resultar em redirecionamento para a página de login, conforme a configuração do Spring Security.

**Exemplo 1 — Data inicial posterior à data final**

Requisição:

```http
GET /api/dashboard?inicio=2026-11-10&fim=2026-11-01
```

Resposta:

```json
{
  "erro": "A data inicial não pode ser posterior à data final."
}
```

**Exemplo 2 — Apenas uma data informada**

Requisição:

```http
GET /api/dashboard?inicio=2026-11-01
```

Resposta:

```json
{
  "erro": "Informe a data inicial e a data final."
}
```

## 7. Integração com a interface

A interface do Dashboard Administrativo utiliza o arquivo `dashboard.js` para realizar as consultas aos endpoints.

O JavaScript utiliza a função `fetch()` para solicitar os dados da API.

### Funcionamento

1. O usuário acessa o Dashboard Administrativo.

2. O JavaScript consulta os endpoints da API.

3. A API busca os pedidos armazenados no MongoDB.

4. A Service realiza os cálculos e agrupamentos necessários.

5. A Controller retorna os resultados em JSON.

6. O JavaScript atualiza os indicadores, gráficos e tabela.

### Atualização dos dados

O dashboard possui dois botões:

- **Filtrar:** consulta as informações de acordo com o período informado.
- **Atualizar:** realiza uma nova consulta utilizando os filtros atuais.

### Tratamento de erros

A interface apresenta mensagens quando:

- O servidor está indisponível.
- As datas informadas são inválidas.
- Não existem pedidos no período.
- Não existem vendas finalizadas.

As mensagens anteriores são atualizadas ou removidas após uma nova consulta, conforme o resultado obtido.

## 8. Testes realizados

Foram implementados testes automatizados para verificar as regras de negócio e os endpoints do Dashboard Administrativo.

### 8.1. DashboardServiceTest

Foram realizados testes relacionados a:

- Análise das vendas por dia.
- Agrupamento de pedidos.
- Cálculo do faturamento.
- Situação dos pedidos.
- Produtos mais vendidos.
- Filtros por período.
- Validação das datas.
- Consultas sem resultados.

**Resultado:**

```text
Tests run: 52
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

### 8.2. DashboardControllerTest

Foram realizados testes relacionados a:

- Consulta dos endpoints.
- Respostas HTTP.
- Parâmetros de datas.
- Validação de filtros.
- Respostas JSON.
- Permissões de acesso.
- Restrições por perfil de usuário.

**Resultado:**

```text
Tests run: 47
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

### 8.3. Testes gerais do projeto

Em uma execução anterior, a suíte completa apresentou:

```text
Tests run: 413
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

### 8.4. Como executar os testes

**Windows (PowerShell):**

```powershell
.\mvnw.cmd test
```

**Somente os testes da Service:**

```powershell
.\mvnw.cmd test -Dtest=DashboardServiceTest
```

**Somente os testes da Controller:**

```powershell
.\mvnw.cmd test -Dtest=DashboardControllerTest
```

### 8.5. Testes manuais

Também foram realizados testes pela interface e pelo navegador.

| Cenário | Resultado |
|---|---|
| Consultar um período válido | Dados retornados com HTTP 200 |
| Consultar datas invertidas | Mensagem de validação apresentada |
| Informar apenas uma data | Mensagem solicitando as duas datas |
| Consultar período sem pedidos | Indicadores zerados e mensagem informativa |
| Consultar período com vendas | Indicadores e gráficos atualizados |
| Falha de conexão | Mensagem de erro apresentada |
| Nova consulta após recuperação | Mensagem de erro removida |

As requisições válidas devem apresentar o status HTTP 200.

## 9. Como acessar o Dashboard Administrativo

1. Verifique se o MongoDB está em execução.

2. Configure a conexão com o banco de dados no arquivo:

   `src/main/resources/application.properties`

3. Execute a aplicação.

**Windows:**

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux/macOS:**

```bash
./mvnw spring-boot:run
```

4. Acesse:
   [http://localhost:8080](http://localhost:8080)

5. Faça login com um usuário de perfil `ADMIN` ou `GERENTE`.

6. Acesse o Dashboard Administrativo pelo menu da aplicação.

## 10. Versionamento

A implementação do Dashboard Administrativo foi desenvolvida na branch:

```text
feature/dashboard
```