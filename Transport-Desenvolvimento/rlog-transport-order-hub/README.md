# RLOG-TRANSPORT-ORDER-SERVICE
Projeto que tem por objetivo expor uma api para redirecionar os payloads recebidos aos endpoints cadastrados no sistema.

# TECNOLOGIAS/LIBS

* *Java 11*
* *Spring 2.1.4.RELEASE*
* *Lombok*
* *Mongodb*
* *Influx*

# CONFIGURAÇÕES

Os arquivos de configurações estão localizados no diretório src/resources.

* application-devlocal.yml - Para configurações locais de desenvolvimento.

# BANCO DE DADOS

* As colections estão localizadas no schema ```rlog-transport-order-hub```. Para acessar o ambiente de desenvolvimento, o host utilizado é o ```mongodb-dev-oms.eastus.cloudapp.azure.com``` na porta ```27017```.

# ESTRUTURA

A estrutura de pacotes da aplicação está organizada da seguinte maneira:


| Pacote | Descrição |
| ------ | ------ |
| infrastructure | onde estão localizadas todas as integrações com recursos externos como banco de dados e api's. Os seus sub-pacotes são: api, mongo e service.

# DESENVOLVIMENTO/COLABORAÇÃO

Ao clonar o repositório https://lojasrenner.visualstudio.com/Omnichannel/_git/rlog-transport-order-hub, deve-se criar uma nova branch para a feature a ser desenvolvida, exemplo:

` feat/teste_123 `

Após desenvolvimento, deve-se então abrir um Pull Request para a branch de desenvolvimento (dev) e executar o pipeline referênte ao commit gerado no PR.

# Build, Run and Test

### 1. Build

* `$ ./gradlew build --info --stacktrace -Dsonar.java.coveragePlugin=jacoco -Dspring.profiles.active=devlocal`
* Pode ser realizado o build dentro do Intellij, utilizando a aba do gradle -> Tasks -> build -> *executar o 'build'*

### 2. Run

* Após executar o build, para rodar o projeto basta executar o bootRun que se encontra em: Tasks -> application -> *bootRun*
* A configuração dentro do bootRun, deverá preferencialmente utilizar *devlocal*, ou seja, no Evironment Variables deverá estar assim para rodar local: *spring.profiles.active=devlocal*

### 3. Test

* O Projeto possui testes unitários e pode ser consultado através do [Sonar](http://oms-toolbox.eastus.cloudapp.azure.com:9000/sonar/dashboard?branch=dev&id=rlog-transport-order-hub).



