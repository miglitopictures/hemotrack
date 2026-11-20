package com.hemotrack.backend.model.sangue;

/**
 * HU08: faixa de temperatura aceita no transporte de cada hemocomponente.
 * Referencia: ANVISA (RDC 34/2014), via POP de transporte de hemocomponentes da Ebserh.
 *
 * @param minC limite inferior em graus Celsius; null = sem limite (congelados)
 * @param maxC limite superior em graus Celsius
 */
public record FaixaTemperatura(Double minC, Double maxC) {

    public static FaixaTemperatura de(TipoHemocomponente tipo) {
        return switch (tipo) {
            case HEMACIAS -> new FaixaTemperatura(1.0, 10.0);
            case PLAQUETAS -> new FaixaTemperatura(20.0, 24.0);
            // congelados: -18 C ou menos
            case PLASMA, CRIOPRECIPITADO -> new FaixaTemperatura(null, -18.0);
        };
    }

    public boolean contem(double temperaturaC) {
        boolean acimaDoMinimo = minC == null || temperaturaC >= minC;
        boolean abaixoDoMaximo = maxC == null || temperaturaC <= maxC;
        return acimaDoMinimo && abaixoDoMaximo;
    }
}
