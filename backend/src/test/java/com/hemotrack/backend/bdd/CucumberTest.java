package com.hemotrack.backend.bdd;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

// Ponto de entrada do BDD: o Surefire acha esta classe pelo sufixo "Test"
// e ela executa todos os .feature do pacote "features" (src/test/resources/features).
// failIfNoTests = false: enquanto todo cenário estiver @wip, a suite fica vazia sem quebrar o build.
@Suite(failIfNoTests = false)
@IncludeEngines("cucumber")
@SelectPackages("features")
class CucumberTest {
}