import { useQueryClient } from "@tanstack/react-query";
import { createFileRoute, redirect, useNavigate } from "@tanstack/react-router";
import { Clock, LogOut, RefreshCw } from "lucide-react";
import { motion } from "motion/react";
import { useEffect } from "react";

import { Logo } from "@/components/app-shell";
import { fadeUp } from "@/components/ui-kit";
import { temSessaoValida } from "@/lib/sessao";
import { destinoDoUsuario, encerrarSessao } from "@/lib/sessao-login";
import { useUsuarioLogado } from "@/lib/useUsuarioLogado";

export const Route = createFileRoute("/aguardando-aprovacao")({
  beforeLoad: () => {
    if (!temSessaoValida()) {
      throw redirect({ to: "/login" });
    }
  },
  head: () => ({
    meta: [
      { title: "Cadastro em análise — HemoTrack" },
      {
        name: "description",
        content: "Seu cadastro está em análise pela operação do HemoTrack.",
      },
    ],
  }),
  component: AguardandoAprovacaoPage,
});

function AguardandoAprovacaoPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { data: usuario, refetch, isFetching } = useUsuarioLogado();

  const instituicao = usuario?.instituicao ?? null;
  const aprovada = instituicao?.status === "APROVADA";

  // Assim que a aprovação sair, sai desta tela sozinho.
  useEffect(() => {
    if (usuario !== undefined && aprovada) {
      void navigate({ to: destinoDoUsuario(usuario) });
    }
  }, [usuario, aprovada, navigate]);

  function sair() {
    encerrarSessao(queryClient);
    void navigate({ to: "/login" });
  }

  return (
    <div className="relative grid min-h-screen place-items-center px-4 py-12">
      <div className="surface-grid pointer-events-none absolute inset-0 opacity-60" />

      <motion.div {...fadeUp} className="relative w-full max-w-md">
        <div className="flex justify-center">
          <Logo />
        </div>

        <div className="mt-8 rounded-3xl border border-border bg-card p-7 text-center shadow-lift">
          <span
            className="mx-auto grid size-12 place-items-center rounded-2xl bg-primary-soft text-primary"
            aria-hidden="true"
          >
            <Clock className="size-6" />
          </span>

          <h1 className="mt-5 text-2xl font-semibold">Cadastro em análise</h1>

          <p className="mt-2 text-sm text-muted-foreground">
            {instituicao === null
              ? "Estamos carregando os dados da sua instituição."
              : `O cadastro de ${instituicao.razaoSocial} está sendo revisado pela operação do HemoTrack. Assim que for aprovado, o painel abre automaticamente.`}
          </p>

          {usuario ? (
            <div className="mt-6 rounded-2xl border border-border bg-secondary/60 p-3 text-left">
              <p className="text-xs text-muted-foreground">Conectado como</p>
              <p className="mt-0.5 text-sm font-semibold">{usuario.nome}</p>
              <p className="text-xs text-muted-foreground">{usuario.email}</p>
            </div>
          ) : null}

          <div className="mt-6 flex flex-col gap-2">
            <motion.button
              type="button"
              onClick={() => void refetch()}
              disabled={isFetching}
              whileHover={{ y: isFetching ? 0 : -2 }}
              whileTap={{ scale: isFetching ? 1 : 0.97 }}
              className="flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground shadow-card disabled:opacity-60"
            >
              <RefreshCw className="size-4" aria-hidden="true" />
              {isFetching ? "Verificando…" : "Verificar novamente"}
            </motion.button>

            <button
              type="button"
              onClick={sair}
              className="flex w-full items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium text-muted-foreground transition-colors hover:text-primary"
            >
              <LogOut className="size-4" aria-hidden="true" /> Sair
            </button>
          </div>
        </div>

        <p className="mt-4 text-center text-xs text-muted-foreground">
          A aprovação é feita pela equipe do HemoTrack e costuma levar até um dia útil.
        </p>
      </motion.div>
    </div>
  );
}