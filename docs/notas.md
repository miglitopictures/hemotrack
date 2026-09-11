# Coisas para estudar
Spring Data JPA
H2
Thymeleaf


# Termos

XML - Extensible Markup Language - é um tipo de linguagem de marcação da organização de padrões W3C, usada para compartilhamento fácil de informações através da internet e configuracoes (no nosso caso). 


# API REST

Coleta ----> Hemocentro ----> Hospital
                L processamento
--------------------------------------------------------------
#  Dominio

## Sistema

### {User}
Id, Nome, Area, Instituicao ... (Pesquisar)

tipos de usuario:
- Usuario Coletor - esta no ponto de coleta e pode aumentar e diminuir o numero de bolsas no estoque.
- Usuario Hemocentro - usuario master.
- Usuario Hospital - faz as {Requisicoes de Trasfucao} (RT).

### {Requisicao de Trasfusao}
Id, data, paciente, hemocomponente, prioridade

## Produtos

### {Bolsa}
 Id, data de coleta, validade, boolEmTransito ... (Pesquisar)

### {Hemocomponente}
 Id, infos, data de coleta, validade, boolEmTransito ... (Pesquisar)

## Instituicoes - pontos no grafo

### {Ponto de Coleta}
Id, EstoqueBolsas, numBolsas, infos ... (Pesquisar)

### {Hemocentro}
 |
Id, EstoqueBolsas, EstoqueHemocomponentes, Requisicoes, infos ... (Pesquisar)

### {Hospital}
 | Id, EstoqueHemocomponentes, infos ... (Pesquisar)



-----------------------------------------------------------------
## Detalhamento API

User | Id, Nome, Area, Instituicao ... (Pesquisar)

get     /users
get     /users[id]
delete  /users[id]
post    /users       + json(cliente infos)
put     /users[id]   + json(cliente infos novas)
patch   /users[id]   + json(cliente especifica para atualizar)

Bolsa | Id, infos ... (Pesquisar)

get     /hemocomponete
get     /hemocomponete[id]
delete  /hemocomponete[id]
post    /hemocomponete       + json(cliente infos)
put     /hemocomponete[id]   + json(cliente infos novas)
patch   /hemocomponete[id]   + json(cliente especifica para atualizar)


Hemocomponente | Tipo, Qntd(ml), Area, Instituicao ... (Pesquisar)

get     /hemocomponete
get     /hemocomponete[id]
delete  /hemocomponete[id]
post    /hemocomponete       + json(cliente infos)
put     /hemocomponete[id]   + json(cliente infos novas)
patch   /hemocomponete[id]   + json(cliente especifica para atualizar)
