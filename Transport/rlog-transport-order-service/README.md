# Broker Fulfilment e Logística

Servi�o para cria��o de ordem de remessa para envio de ordem para parceiros

## Requirements

1. Java 11.x.x
2. Lombok plugin - 1.x.x

## Steps to Setup

**1. Add Temporary IP**
* Para rodar o projeto é necessário liberar seu IP, para isso o pipeline [Add Temporary IP](https://lojasrenner.visualstudio.com/OMS-2018/_build?definitionId=478) deve ser rodado.

**2. Configure MongoDb**
* Necessário configurar a porta 27017 para acessar o banco de dados Mongo.
* Solicitar a senha ao time.
```bash
ssh bfladmin@oms-tester-machine.eastus.cloudapp.azure.com -L 27017:10.100.15.120:27017
```

**3. Clone the application**
```bash
git clone https://lojasrenner.visualstudio.com/OMS-2018/_git/bfl-broker-fulfillment-service
```
* As credenciais de acesso ao repositório devem ser geradas no próprio repositório.

**4. Run the app**

```bash
gradlew bootRun -Dspring.profiles.active=local
```

## Running tests

O projeto contempla testes unitários que podem ser rodados utilizando o comando `gradlew test`.

## Code Quality and Code Security

O projeto tem suas métricas analisadas e registradas pelo [SonarQube](http://oms-toolbox.eastus.cloudapp.azure.com:9000/sonar/dashboard?id=bfl-broker-fulfillment-service). 