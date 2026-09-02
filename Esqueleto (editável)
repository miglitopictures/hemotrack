
Ideias de Pacotes principais 
//├── doador/
//│   ├── Doador
//│   ├── Triagem
//│   └── Aptidao
//│
//├── coleta/
//│   ├── Doacao
//│   ├── Bolsa
//│   └── Identificacao
//│
//├── laboratorio/
//│   ├── TesteLaboratorial
//│   ├── ResultadoTeste
//│   └── Validacao
//│
//├── estoque/
//│   ├── Armazenamento
//│   ├── ComponenteSanguineo
//│   ├── Reserva
//│   └── Validade
//│
//└── transfusao/
//    ├── Solicitacao
//    ├── Compatibilidade
//    ├── Liberacao
//    ├── Transporte
//    └── Transfusao
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

Eu faria a bolsa possuir um status, e o sistema só permitir determinadas transições:

//COLETADA
//↓
//IDENTIFICADA
//↓
//EM_TESTE
//↓
//APROVADA / REPROVADA
//↓
//FRACIONADA
//↓
//ARMAZENADA ATÉ X VALIDADE SE A VALIDADE TIVER VENCENDO ELA ENTRA EM PRIORIDADE DE USO
//↓
//DISPONÍVEL
//↓
//RESERVADA
//↓
//LIBERADA
//↓
//EM_TRANSPORTE
//↓
//RECEBIDA
//↓
//TRANSFUNDIDA
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
 
alguns caminhos alternativos:

//EM_TESTE → REPROVADA → DESCARTADA
//
//DISPONÍVEL → VENCIDA → DESCARTADA
//
//TRANSPORTE → DEVOLVIDA → AVALIAÇÃO → DISPONÍVEL ou DESCARTADA

//1. Doador
//→ Cadastro → Triagem → Aptidão
//
//2. Coleta
//→ Doação → Bolsa → Identificação → Rastreabilidade
//
//3. Laboratório
//→ Testes → Resultados → Aprovação/Reprovação
//
//4. Estoque
//→ Armazenamento → Validade → Disponibilidade → Reserva
//
//5. Transfusão
//→ Solicitação ao HEMO CENTRO → Compatibilidade → Liberação → Transporte → Recebimento(HOSPITAL) → Transfusão
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

| Etapa                          | O que acontece                         | Principais validações                                                    |
| ------------------------------ | -------------------------------------- | ------------------------------------------------------------------------ |
| **1. Cadastro do doador**      | Doador é registrado no sistema         | Dados obrigatórios, CPF/documento, idade, cadastro válido                |
| **2. Triagem**                 | Avaliação antes da doação              | Questionário, condições de saúde, peso, pressão, critérios para doação   |
| **3. Aprovação para doar**     | Doador é considerado apto              | **Apto ou inapto**; se inapto, encerra o processo                        |
| **4. Doação**                  | Sangue é coletado                      | Doador aprovado, material correto, procedimento iniciado                 |
| **5. Coleta da bolsa**         | Bolsa recebe o sangue                  | Volume adequado, integridade da bolsa, identificação única               |
| **6. Identificação**           | Bolsa recebe código/rastreabilidade    | Código único, vínculo com doador, data/hora e tipo sanguíneo             |
| **7. Armazenamento inicial**   | Bolsa aguarda processamento            | Temperatura e condições adequadas                                        |
| **8. Testes laboratoriais**    | Amostras são analisadas                | Testes obrigatórios realizados                                           |
| **9. Validação dos testes**    | Resultados são avaliados               | **Aprovada / Reprovada**                                                 |
| **10. Fracionamento**          | Sangue pode ser separado               | Bolsa aprovada e apta ao processamento                                   |
| **11. Separação**              | Gera componentes                       | Hemácias, plasma, plaquetas etc.                                         |
| **12. Armazenamento**          | Componentes são armazenados            | Temperatura, validade e condições específicas                            |
| **13. Disponibilização**       | Componente entra no estoque disponível | Testes aprovados, validade vigente e estoque disponível                  |
| **14. Solicitação hospitalar** | Hospital solicita sangue               | Solicitação válida, componente necessário e quantidade                   |
| **15. Compatibilidade**        | Verificação para o receptor            | Tipo sanguíneo, compatibilidade e demais critérios aplicáveis            |
| **16. Liberação**              | Componente é separado do estoque       | Bolsa válida, compatível e dentro da validade                            |
| **17. Transporte**             | Bolsa vai para o hospital              | Condições adequadas de transporte e rastreabilidade                      |
| **18. Recebimento**            | Hospital confirma chegada              | Integridade, identificação e condições da bolsa                          |
| **19. Transfusão**             | Sangue é administrado ao paciente      | Conferência do paciente, bolsa e procedimento                            |
| **20. Finalização**            | Sistema encerra o ciclo                | Bolsa marcada como **transfundida**                                      |
| **21. Descarte**               | Caso não possa ser utilizada           | Motivo registrado: validade, contaminação, resultado alterado, dano etc. |
