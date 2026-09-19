import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  Outlet,
  Link,
  createRootRouteWithContext,
  useRouter,
  HeadContent,
} from "@tanstack/react-router";
import { MotionConfig } from "motion/react";
import { useEffect } from "react";

import { Toaster } from "../components/ui/sonner";

/**
 * Reporte simples de erro não tratado.
 *
 * No projeto original (TanStack Start) isso chamava um módulo
 * `lib/lovable-error-reporting`, que era injetado automaticamente pela
 * plataforma da Lovable e não existe fora dela. Aqui é só um console.error —
 * troque por Sentry/LogRocket/etc. quando tiver telemetria de verdade.
 */
function reportarErro(error: unknown, contexto?: Record<string, unknown>) {
  console.error("[HemoTrack] erro não tratado:", error, contexto);
}

function NotFoundComponent() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <div className="max-w-md text-center">
        <h1 className="font-display text-7xl font-bold text-foreground">404</h1>
        <h2 className="mt-4 text-xl font-semibold text-foreground">Página não encontrada</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          O endereço acessado não existe ou o registro foi removido. Confira o código da
          solicitação, bolsa ou transporte.
        </p>
        <div className="mt-6 flex flex-wrap justify-center gap-2">
          <Link
            to="/hospital"
            className="inline-flex items-center justify-center rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90"
          >
            Painel do hospital
          </Link>
          <Link
            to="/hemocentro"
            className="inline-flex items-center justify-center rounded-xl border border-border px-4 py-2 text-sm font-semibold transition-colors hover:border-primary/40 hover:text-primary"
          >
            Painel do hemocentro
          </Link>
        </div>
      </div>
    </div>
  );
}

function ErrorComponent({ error, reset }: { error: Error; reset: () => void }) {
  console.error(error);
  const router = useRouter();
  useEffect(() => {
    reportarErro(error, { boundary: "tanstack_root_error_component" });
  }, [error]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <div className="max-w-md text-center">
        <h1 className="text-xl font-semibold tracking-tight text-foreground">
          Esta página não carregou
        </h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Algo falhou do nosso lado. Você pode tentar de novo ou voltar ao início.
        </p>
        <div className="mt-6 flex flex-wrap justify-center gap-2">
          <button
            onClick={() => {
              router.invalidate();
              reset();
            }}
            className="inline-flex items-center justify-center rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90"
          >
            Tentar de novo
          </button>
          <a
            href="/"
            className="inline-flex items-center justify-center rounded-xl border border-input bg-background px-4 py-2 text-sm font-semibold text-foreground transition-colors hover:bg-accent"
          >
            Ir para o início
          </a>
        </div>
      </div>
    </div>
  );
}

export const Route = createRootRouteWithContext<{ queryClient: QueryClient }>()({
  // O `head()` + <HeadContent /> funcionam normalmente em modo client-only
  // (não é exclusivo do TanStack Start) e atualizam <title>/meta a cada
  // troca de rota. As tags que não mudam entre rotas (viewport, favicon,
  // fontes, tema) já ficam fixas em index.html.
  head: () => ({
    meta: [{ title: "HemoTrack — Gestão e distribuição de hemocomponentes" }],
  }),
  component: RootComponent,
  notFoundComponent: NotFoundComponent,
  errorComponent: ErrorComponent,
});

function RootComponent() {
  const { queryClient } = Route.useRouteContext();

  return (
    <QueryClientProvider client={queryClient}>
      <HeadContent />
      {/* reducedMotion="user" desliga as animações de transform para quem
          pediu movimento reduzido no sistema operacional. */}
      <MotionConfig reducedMotion="user">
        {/* Obrigatório: as rotas filhas renderizam aqui. Remover o
            <Outlet /> quebra todas as rotas filhas. */}
        <Outlet />
        <Toaster position="top-right" richColors closeButton />
      </MotionConfig>
    </QueryClientProvider>
  );
}
