# Sistema de Gestão de Estudantes | Java & MongoDB

Esse sistema é composto por um conjunto de tabelas que representam a gestão de estudantes, contendo tabelas como: estudantes, cursos, matriculas e notas.

Esse sistema deverá ser executado no ambiente proposto: Linux (Ubunto).

O sistema exige que as tabelas existam, então basta compilar o projeto e executar o script Java de configuração do banco de dados "MongoDatabaseSetup.java" para criação das tabelas e preenchimento de dados de exemplos, siga os passos a seguir.

-> Video de demonstração: https://youtu.be/1Hq6aYKLh-M?si=z_N9gXrsLoeSdiwV

## Executando o Script de Configuração do Banco de dados

### Execução manual

obs: Antes de tudo, inicie o banco de dados MongoDB e MySQL no terminal do Ubunto (certifique-se de estar na pasta "database_services" para executar o comando):
```bash
docker compose up -d mongo -d mysql
```

1. Faça um clone do projeto em alguma pasta:
```bash
git clone https://github.com/LaysonBatista/crud-gestao-estudantes-mongoDB
```

2. Na pasta do projeto, compile:
```bash
javac -d bin -cp "lib/*" src/*.java src/conexion/*.java src/controller/*.java src/model/*.java src/reports/*.java src/utils/*.java
```

3. Logo após, execute o script de configuração do MongoDB:
```bash
java -cp "bin:lib/*:src" MongoDatabaseSetup
```

4. Execute o Menu Principal do Sistema:
```bash
java -cp "bin:lib/*:src" Main
```


## Pré-requisitos para execução do sistema

### 1. Softwares Necessários

#### Java Development Kit (JDK)
- **Versão**: JDK 8 ou superior
- **Verificação**: `java -version` e `javac -version`

#### Docker 
- **Para**: Executar o MongoDB via Docker compose
- **Verficação**: `docker --version` e `docker-compose --version`



## Organização
- [diagrams](diagrams): Nesse diretório está o [diagrama relacional](diagrams/DIAGRAMA_RELACIONAL_ESTUDANTES.PNG) (lógico) do sistema.
    * O sistema possui quatro entidades: ESTUDANTES, CURSOS, MATRICULAS E NOTAS.
- [lib](lib): Nesse diretório está o [mysql-connector-java-8.0.30.jar](mysql-connector-java-8.0.30.jar) e [mongodb-driver-core-5.6.1.jar](mongodb-driver-core-5.6.1.jar) para realizar a conexão com o banco de dados.
- [sql](sql): Nesse diretório estão os scripts para criação das tabelas e inserção de dados fictícios para testes do sistema
    * [create_tables_estudantes.sql](sql/create_tables_estudantes.sql): script responsável pela criação das tabelas, relacionamentos e criação de permissão no esquema LabDatabase.
    * [inserting_samples_records.sql](sql/inserting_samples_records.sql): script responsável pela inserção dos registros fictícios para testes do sistema.
    * [creat_collections_estudantes.js](creat_collections_estudantes.js): script responsável pela criação de colaeções para o MongoDB.
- [src](src): Nesse diretório estão os scripts do sistema
    * [conexion](src/conexion): Nesse repositório encontra-se o [módulo de conexão com o banco de dados MySQL](src/conexion/Conexao.java) e o [módulo de conexão com o banco de dados MySQL](src/conexion/MongoConnection.java).
    * [controller](src/controller/): Nesse diretório encontram-sem as classes controladoras, responsáveis por realizar inserção, alteração e exclusão dos registros das tabelas.
    * [model](src/model/): Nesse diretório encontram-ser as classes das entidades descritas no [diagrama relacional](diagrams/DIAGRAMA_RELACIONAL_ESTUDANTES.png).
    * [reports](src/reports/) Nesse diretório encontra-se a [classe](src/reports/Relatorios.java) responsável por gerar todos os relatórios do sistema.
    * [utils](src/utils/): Nesse diretório encontram-se scripts de automatização da [tela de informações iniciais](src/utils/splash_screen.java).


# crud-gestao-estudantes-mongoDB

## Integrantes

- Carlos Vinícius
- Layson Batista
- Lucas da Silva de Melo
- Sabrina Rosa
- Soffia Martins

## Tema

- Sistema de Gestão de Estudantes
