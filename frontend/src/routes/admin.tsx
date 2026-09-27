import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, redirect, useNavigate } from "@tanstack/react-router";
import { Building2, Check, Inbox, LogOut, RefreshCw } from "lucide-react";
import { motion } from "motion/react";
import { toast } from "sonner";

import { Logo } from "@/components/app-shell";
import { fadeUp } from "@/components/ui-kit";
import {
  CHAVE_USUARIO_LOGADO,
  aprovarInstituicao,
  buscarUsuarioLogado,
  chaveInstituicoes,
  listarInstituicoes,
} from "@/lib/api-auth";
import { formatarCnpj } from "@/lib/formato";
import { ErroDeApiHttp } from "@/lib/httpClient";
import { temSessaoValida } from "@/lib/sessao";
import { destinoDoUsuario, encerrarSessao } from "@/lib/sessao-login";

export const Route = createFileRoute("/admin")({
  beforeLoad: async ({ context }) => {
    if (!temSessaoValida()) {
      throw redirect({ to: "/login" });
    }

    // O papel está no token, mas ler de /auth/me mantém uma fonte só de
    // verdade para as guardas — e o dado já está em cache depois do login.
    const usuario = await context.queryClient.ensureQueryData({
      queryKey: CHAVE_USUARIO_LOGADO,
      queryFn: buscarUsuarioLogado,
    });

    if (usuario.papel !== "ADMIN_SISTEMA") {
      // Quem não é da operação volta para o próprio lugar, não para o login.
      throw redirect({ to: destinoDoUsuario(usuario) });
    }
  },
  head: () => ({
    meta: [
      { title: "Administração — HemoTrack" },
      { name: "description", content: "Aprovação de cadastros de instituições." },
    ],
  }),
  component: AdminPage,
});

function AdminPage() {
  const queryClient = useQueryClient();

  const navigate = useNavigate();

  function sair() {
    encerrarSessao(queryClient);
    void navigate({ to: "/login" });
  }

  const pendentes = useQuery({
    queryKey: chaveInstituicoes("PENDENTE_APROVACAO"),
    queryFn: () => listarInstituicoes("PENDENTE_APROVACAO"),
  });

  const aprovacao = useMutation({
    mutationFn: aprovarInstituicao,

    onSuccess: (instituicao) => {
      toast.success(`${instituicao.razaoSocial} aprovada`);

      // A instituição saiu de "pendente": manda o React Query buscar a lista
      // de novo em vez de removê-la na mão. Uma fonte de verdade só.
      void queryClient.invalidateQueries({
        queryKey: chaveInstituicoes("PENDENTE_APROVACAO"),
      });
    },

    onError: (falha) => {
      toast.error(
        falha instanceof ErroDeApiHttp ? falha.message : "Não foi possível aprovar o cadastro.",
      );
    },
  });

  const lista = pendentes.data ?? [];

  return (
    <div className="relative min-h-screen px-4 py-12">
      <div className="surface-grid pointer-events-none absolute inset-0 opacity-60" />

      <motion.div {...fadeUp} className="relative mx-auto w-full max-w-3xl">
        <div className="flex justify-center">
            <div className="flex items-center justify-between gap-4">
                <Logo />
                <button
                    type="button"
                    onClick={sair}
                    className="flex items-center gap-2 rounded-xl px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:text-primary"
                >
                    <LogOut className="size-4" aria-hidden="true" /> Sair
                </button>
            </div>
        </div>

        <div className="mt-8 rounded-3xl border border-border bg-card p-7 shadow-lift">
                    <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h1 className="text-2xl font-semibold">Cadastros em análise</h1>
              <p className="mt-1.5 text-sm text-muted-foreground">
                Instituições aguardando aprovação para operar no HemoTrack.
              </p>
            </div>

            <button
              type="button"
              onClick={() => void pendentes.refetch()}
              disabled={pendentes.isFetching}
              className="flex items-center gap-2 rounded-xl border border-border px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:text-primary disabled:opacity-60"
            >
              <RefreshCw className="size-4" aria-hidden="true" />
              {pendentes.isFetching ? "Atualizando…" : "Atualizar"}
            </button>
          </div>

          {pendentes.isPending ? (
            <p className="mt-8 text-center text-sm text-muted-foreground">Carregando cadastros…</p>
          ) : pendentes.isError ? (
            <div className="mt-8 text-center">
              <p className="text-sm font-medium text-destructive">
                {pendentes.error instanceof ErroDeApiHttp
                  ? pendentes.error.message
                  : "Não foi possível carregar os cadastros."}
              </p>
              <button
                type="button"
                onClick={() => void pendentes.refetch()}
                className="mt-3 text-sm font-semibold text-primary hover:underline"
              >
                Tentar de novo
              </button>
            </div>
          ) : lista.length === 0 ? (
            <div className="mt-8 text-center">
              <span
                className="mx-auto grid size-12 place-items-center rounded-2xl bg-secondary text-muted-foreground"
                aria-hidden="true"
              >
                <Inbox className="size-6" />
              </span>
              <p className="mt-4 text-sm font-medium">Nenhum cadastro em análise</p>
              <p className="mt-1 text-sm text-muted-foreground">
                Novas instituições aparecem aqui assim que se cadastram.
              </p>
            </div>
          ) : (
            <ul className="mt-6 space-y-3">
              {lista.map((instituicao) => {
                const aprovandoEsta =
                  aprovacao.isPending && aprovacao.variables === instituicao.id;

                return (
                  <li
                    key={instituicao.id}
                    className="flex flex-wrap items-center gap-4 rounded-2xl border border-border bg-background p-4"
                  >
                    <span
                      className="grid size-10 shrink-0 place-items-center rounded-xl bg-secondary text-muted-foreground"
                      aria-hidden="true"
                    >
                      <Building2 className="size-5" />
                    </span>

                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-semibold">{instituicao.razaoSocial}</p>
                      <p className="text-xs text-muted-foreground">
                        {instituicao.tipo === "HEMOCENTRO" ? "Hemocentro" : "Hospital"} ·{" "}
                        {instituicao.municipio} · CNPJ {formatarCnpj(instituicao.cnpj)}
                      </p>
                      <p className="text-xs text-muted-foreground">{instituicao.endereco}</p>
                    </div>

                    <motion.button
                      type="button"
                      onClick={() => aprovacao.mutate(instituicao.id)}
                      disabled={aprovacao.isPending}
                      whileHover={{ y: aprovacao.isPending ? 0 : -2 }}
                      whileTap={{ scale: aprovacao.isPending ? 1 : 0.97 }}
                      className="flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground shadow-card disabled:opacity-60"
                    >
                      <Check className="size-4" aria-hidden="true" />
                      {aprovandoEsta ? "Aprovando…" : "Aprovar"}
                    </motion.button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </motion.div>
    </div>
  );
}