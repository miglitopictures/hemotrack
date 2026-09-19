import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { motion } from "motion/react";
import { useState } from "react";
import { ArrowRight, Lock, Mail } from "lucide-react";
import { toast } from "sonner";
import { Logo } from "@/components/app-shell";
import { fadeUp } from "@/components/ui-kit";

export const Route = createFileRoute("/login")({
  head: () => ({
    meta: [
      { title: "Entrar — HemoTrack" },
      { name: "description", content: "Acesse o HemoTrack com e-mail e senha da sua instituição." },
      { property: "og:title", content: "Entrar — HemoTrack" },
      {
        property: "og:description",
        content: "Acesso de hospitais e hemocentros à rede HemoTrack.",
      },
    ],
  }),
  component: LoginPage,
});

function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [erro, setErro] = useState<string | null>(null);

  function entrar(e: React.FormEvent<HTMLFormElement>) {
    // Botão de submit de verdade: agora o Enter no campo funciona e o leitor
    // de tela anuncia um botão, não um link.
    e.preventDefault();
    if (!email.trim() || !senha.trim()) {
      setErro("Informe e-mail e senha para continuar.");
      return;
    }
    setErro(null);
    // TODO(back): POST /auth/login → guardar sessão e redirecionar pelo perfil.
    toast.success("Bem-vindo de volta!", { description: "Sessão simulada do protótipo." });
    void navigate({ to: "/hospital" });
  }

  return (
    <div className="relative grid min-h-screen place-items-center px-4 py-12">
      <div className="surface-grid pointer-events-none absolute inset-0 opacity-60" />
      <motion.div {...fadeUp} className="relative w-full max-w-md">
        <div className="flex justify-center">
          <Logo />
        </div>
        <div className="mt-8 rounded-3xl border border-border bg-card p-7 shadow-lift">
          <h1 className="text-2xl font-semibold">Entrar</h1>
          <p className="mt-1.5 text-sm text-muted-foreground">
            Use as credenciais da sua instituição.
          </p>

          <form className="mt-6 space-y-4" onSubmit={entrar} noValidate>
            <label className="block">
              <span className="text-sm font-medium">E-mail</span>
              <div className="mt-1.5 flex items-center gap-2 rounded-xl border border-input bg-background px-3 py-2.5 transition-colors focus-within:border-primary">
                <Mail className="size-4 text-muted-foreground" aria-hidden="true" />
                <input
                  type="email"
                  name="email"
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="nome@instituicao.org.br"
                  className="w-full bg-transparent text-sm outline-none"
                />
              </div>
            </label>

            <label className="block">
              <span className="text-sm font-medium">Senha</span>
              <div className="mt-1.5 flex items-center gap-2 rounded-xl border border-input bg-background px-3 py-2.5 transition-colors focus-within:border-primary">
                <Lock className="size-4 text-muted-foreground" aria-hidden="true" />
                <input
                  type="password"
                  name="senha"
                  autoComplete="current-password"
                  value={senha}
                  onChange={(e) => setSenha(e.target.value)}
                  placeholder="••••••••"
                  className="w-full bg-transparent text-sm outline-none"
                />
              </div>
            </label>

            {erro ? (
              <p role="alert" className="text-sm font-medium text-destructive">
                {erro}
              </p>
            ) : null}

            <motion.button
              type="submit"
              whileHover={{ y: -2 }}
              whileTap={{ scale: 0.97 }}
              className="flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground shadow-card"
            >
              Entrar <ArrowRight className="size-4" aria-hidden="true" />
            </motion.button>
          </form>

          <div className="mt-5 flex items-center justify-between text-sm">
            <button
              type="button"
              onClick={() =>
                toast.info("Recuperação de senha", {
                  description: "Fluxo ainda não implementado no protótipo.",
                })
              }
              className="font-medium text-primary hover:underline"
            >
              Esqueci minha senha
            </button>
            <Link to="/cadastro" className="text-muted-foreground hover:text-foreground">
              Criar conta
            </Link>
          </div>
        </div>

        <div className="mt-4 rounded-2xl border border-dashed border-border p-4 text-center text-xs text-muted-foreground">
          Protótipo: entre direto no{" "}
          <Link to="/hospital" className="font-semibold text-primary">
            painel do hospital
          </Link>{" "}
          ou no{" "}
          <Link to="/hemocentro" className="font-semibold text-primary">
            painel do hemocentro
          </Link>
          .
        </div>
      </motion.div>
    </div>
  );
}
