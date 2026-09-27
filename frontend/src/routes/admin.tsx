import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, redirect } from "@tanstack/react-router";
import { Building2, Check } from "lucide-react";
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
import { ErroDeApiHttp } from "@/lib/httpClient";
import { temSessaoValida } from "@/lib/sessao";
import { destinoDoUsuario } from "@/lib/sessao-login";

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
          <Logo />
        </div>

        <div className="mt-8 rounded-3xl border border-border bg-card p-7 shadow-lift">
          <h1 className="text-2xl font-semibold">Cadastros em análise</h1>
          <p className="mt-1.5 text-sm text-muted-foreground">
            Instituições aguardando aprovação para operar no HemoTrack.
          </p>

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
                      {instituicao.municipio} · CNPJ {instituicao.cnpj}
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
        </div>
      </motion.div>
    </div>
  );
}