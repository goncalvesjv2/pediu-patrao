# Pediu Patrão

## Sobre o projeto

O PediuPatrão é uma aplicação web para gerenciamento de uma pizzaria, desenvolvida como parte da atividade prática da disciplina de Engenharia de Software.

O projeto tem como objetivo organizar o cadastro de clientes, o gerenciamento de pedidos, o controle de acesso por perfil de usuário e o registro de auditoria das operações realizadas no sistema.

A evolução da aplicação contempla regras de negócio para o ciclo de vida dos pedidos, aplicação de descontos, cancelamentos e rastreabilidade das alterações realizadas.

## Tecnologias utilizadas

- Java
- Spring Boot
- Spring Security
- MongoDB
- Thymeleaf
- Maven

## Funcionalidades implementadas

### 1. Gerenciamento de produtos

- Cadastro e consulta de produtos.
- Alteração dos dados dos produtos.
- Controle de produtos ativos e inativos.

### 2. Gerenciamento de clientes

- Cadastro e consulta de clientes.
- Alteração dos dados cadastrais.
- Identificação dos campos alterados durante a atualização dos dados do cliente.

### 3. Gerenciamento de pedidos

- Criação de pedidos associados a clientes e produtos.
- Cálculo do valor total do pedido com base nos itens.
- Registro do responsável pela criação e do horário de entrada do pedido.
- Controle do fluxo de status: recebido, em preparo, pronto, saiu para entrega e finalizado.
- Registro do responsável e do horário de saída para entrega.
- Validação das transições permitidas entre os status.
- Cancelamento de pedidos mediante justificativa, conforme as regras de negócio.
- Aplicação de descontos de até 20%, conforme as permissões do usuário.

### 4. Autenticação e controle de acesso

- Autenticação de usuários com Spring Security.
- Controle de acesso às funcionalidades conforme o perfil do usuário.
- Restrição das operações de cancelamento e aplicação de descontos ao perfil de gerente.
- Restrição da consulta aos registros de auditoria aos perfis de administrador e gerente.

### 5. Auditoria 

- Registro da criação de pedidos.
- Registro das alterações de status dos pedidos.
- Registro de aplicação de descontos e cancelamento de pedidos.
- Registro da criação e das alterações dos dados dos clientes.
- Registro de valores anteriores e novos dos campos alterados, quando aplicável.
- Registro da justificativa de cancelamento.

### 6. Testes 

- Testes das regras de negócio relacionadas aos pedidos.
- Testes de autorização para consulta dos registros de auditoria.
- Validação das regras de transição de status, cancelamento e aplicação de descontos.

## Configuração do banco de dados

A aplicação utiliza MongoDB para persistência dos dados.

Para executar o projeto localmente, configure o arquivo `src/main/resources/application.properties` com os dados de conexão do seu ambiente.

Exemplo de configuração:

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/
spring.data.mongodb.database=pizzaria
```

Verifique se o MongoDB está em execução antes de iniciar a aplicação.

## Como executar o projeto

1. Clone o repositório:

   ```bash
   git clone https://github.com/goncalvesjv2/pediu-patrao.git
   ```

2. Entre na pasta do projeto:

   ```bash
   cd pediu-patrao
   ```

3. Configure a conexão com o MongoDB no arquivo `src/main/resources/application.properties`.


4. Execute a aplicação pelo IntelliJ IDEA, utilizando a classe principal `PizzariaPedidosApplication`, ou pelo terminal, caso o Maven Wrapper esteja disponível.


   **Windows:**

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   **Linux/macOS:**

   ```bash
   ./mvnw spring-boot:run
   ```

5. Após a inicialização, acesse a aplicação pelo navegador:

   http://localhost:8080
