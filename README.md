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

## Configuração do banco de dados

A aplicação utiliza MongoDB.

Para desenvolvimento local, configure o arquivo `src/main/resources/application.properties` com os dados de conexão do seu ambiente.

Exemplo de configuração:

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/
spring.data.mongodb.database=pizzaria
```

Verifique se MongoDB esteja em execução antes de iniciar a aplicação.

## Como executar o projeto

1. Clone o repositório:
    ```bash
    git clone https://github.com/goncalvesjv2/pediu-patrao
    ```

2. Entre na pasta do projeto:
    ```bash
    cd PediuPatrao
    ```

3. Configure a conexão com o MongoDB no arquivo application.properties.

4. Execute a aplicação pelo IntelliJ IDEA, utilizando a classe principal PizzariaPedidosApplication, ou pelo terminal, caso o Maven Wrapper esteja disponível:

   ```bash
   ./mvnw spring-boot:run
   ```

   No Windows, utilizando o Prompt de Comando:

   ```bash
   mvnw.cmd spring-boot:run
   ```

5. Após a inicialização, acesse pelo Front-End:

   `http://localhost:8080`
