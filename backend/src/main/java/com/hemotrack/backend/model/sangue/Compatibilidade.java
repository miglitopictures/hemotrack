package com.hemotrack.backend.model.sangue;

import com.hemotrack.backend.model.shared.FatorRh;
import com.hemotrack.backend.model.shared.TipoABO;

/**
 * Regras didaticas de compatibilidade ABO/Rh (HU06)
 */
public final class Compatibilidade {

    private Compatibilidade() {
    }

    public static boolean podeReceber(TipoHemocomponente tipo,
                                      TipoABO aboDoador, FatorRh rhDoador,
                                      TipoABO aboReceptor, FatorRh rhReceptor) {
        switch (tipo) {
            case HEMACIAS:
                return aboHemacias(aboDoador, aboReceptor) && rhCompativel(rhDoador, rhReceptor);
            case PLAQUETAS:
                return aboPlasma(aboDoador, aboReceptor) && rhCompativel(rhDoador, rhReceptor);
            case PLASMA:
            case CRIOPRECIPITADO:
                return aboPlasma(aboDoador, aboReceptor);
            default:
                return false;
        }
    }

    // hemacias: tipos iguais sao compativeis, O doa para todos e AB recebe de todos.
    private static boolean aboHemacias(TipoABO doador, TipoABO receptor) {
        return doador == receptor || doador == TipoABO.O || receptor == TipoABO.AB;
    }

    // plasma: o inverso das hemacias. AB doa para todos e O recebe de todos.
    private static boolean aboPlasma(TipoABO doador, TipoABO receptor) {
        return doador == receptor || doador == TipoABO.AB || receptor == TipoABO.O;
    }

    // Rh negativo doa para todos; Rh positivo so para Rh positivo.
    private static boolean rhCompativel(FatorRh doador, FatorRh receptor) {
        return doador == FatorRh.NEGATIVO || receptor == FatorRh.POSITIVO;
    }
}