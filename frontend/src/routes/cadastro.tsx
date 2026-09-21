import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { motion } from "motion/react";
import { useState } from "react";
import { Building2, Check, Droplet, Hospital } from "lucide-react";
import { toast } from "sonner";
import { Logo } from "@/components/app-shell";
import { fadeUp } from "@/components/ui-kit";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/cadastro")({
  head: () => ({
    meta: [
      { title: "Cadastrar instituição — HemoTrack" },
      {
        name: "description",
        content: "Cadastre um hospital ou hemocentro na rede HemoTrack em poucos passos.",
      },
      { property: "og:title", content: "Cadastrar instituição — HemoTrack" },
      {
        property: "og:description",
        content: "Hospital ou hemocentro: dados institucionais, endereço e contato.",
      },
    ],
  }),
  component: CadastroPage,
});

type Campos = {
  nome: string;
  cnpj: string;
  telefone: string;
  endereco: string;
  email: string;
  senha: string;
};

const vazio: Campos = { nome: "", cnpj: "", telefone: "", endereco: "", email: "", senha: "" };

function Field({
  label,
  placeholder,
  type = "text",
  className,
  value,
  onChange,
  name,
  autoComplete,
  required = false,
}: {
  label: string;
  placeholder: string;
  type?: string;
  className?: string;
  value: string;
  onChange: (v: string) => void;
  name: string;
  autoComplete?: string;
  required?: boolean;
}) {
  return (
    <label className={cn("block", className)}>
      <span className="text-sm font-medium">
        {label}
        {required ? <span className="text-primary"> *</span> : null}
      </span>
      <input
        type={type}
        name={name}
        autoComplete={autoComplete}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="mt-1.5 w-full rounded-xl border border-input bg-background px-3 py-2.5 text-sm outline-none transition-colors focus:border-primary"
      />
    </label>
  );
}

function CadastroPage() {
  const navigate = useNavigate();
  const [tipo, setTipo] = useState<"hospital" | "hemocentro">("hospital");
  const [campos, setCampos] = useState<Campos>(vazio);
  const [erro, setErro] = useState<string | null>(null);

  const definir = (chave: keyof Campos) => (valor: string) =>
    setCampos((c) => ({ ...c, [chave]: valor }));

  const opcoes = [
    {
      key: "hospital" as const,
      icon: Hospital,
      titulo: "Hospital",
      texto: "Solicita e recebe hemocomponentes",
    },
    {
      key: "hemocentro" as const,
      icon: Droplet,
      titulo: "Hemocentro",
      texto: "Gerencia estoque e distribui bolsas",
    },
  ];

  function concluir(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const faltando = (["nome", "cnpj", "email", "senha"] as const).filter((k) => !campos[k].trim());
    if (faltando.length > 0) {
      setErro("Preencha nome, CNPJ, e-mail e senha para concluir o cadastro.");
      return;
    }
    setErro(null);
    // TODO(back): POST /instituicoes
    toast.success("Instituição cadastrada", { description: "Cadastro simulado do protótipo." });
    void navigate({ to: tipo === "hospital" ? "/hospital" : "/hemocentro" });
  }

  return (
    <div className="relative min-h-screen px-4 py-12">
      <div className="surface-grid pointer-events-none absolute inset-0 opacity-60" />
      <motion.div {...fadeUp} className="relative mx-auto w-full max-w-2xl">
        <div className="flex justify-center">
          <Logo />
        </div>

        <div className="mt-8 rounded-3xl border border-border bg-card p-7 shadow-lift">
          <h1 className="text-2xl font-semibold">Cadastrar instituição</h1>
          <p className="mt-1.5 text-sm text-muted-foreground">
            Escolha o perfil e complete os dados institucionais.
          </p>

          <div role="radiogroup" aria-label="Tipo de instituição" className="mt-6 grid gap-3 sm:grid-cols-2">
            {opcoes.map((o) => {
              const ativo = tipo === o.key;
              return (
                <motion.button
                  key={o.key}
                  type="button"
                  role="radio"
                  aria-checked={ativo}
                  onClick={() => setTipo(o.key)}
                  whileHover={{ y: -3 }}
                  whileTap={{ scale: 0.98 }}
                  className={cn(
                    "relative flex items-start gap-3 rounded-2xl border p-4 text-left transition-colors",
                    ativo ? "border-primary bg-primary-soft" : "border-border bg-background",
                  )}
                >
                  <span
                    className={cn(
                      "grid size-10 shrink-0 place-items-center rounded-xl",
                      ativo
                        ? "bg-primary text-primary-foreground"
                        : "bg-secondary text-muted-foreground",
                    )}
                    aria-hidden="true"
                  >
                    <o.icon className="size-5" />
                  </span>
                  <span>
                    <span className="block text-sm font-semibold">{o.titulo}</span>
                    <span className="block text-xs text-muted-foreground">{o.texto}</span>
                  </span>
                  {ativo ? (
                    <motion.span
                      initial={{ scale: 0 }}
                      animate={{ scale: 1 }}
                      aria-hidden="true"
                      className="absolute right-3 top-3 grid size-5 place-items-center rounded-full bg-primary text-primary-foreground"
                    >
                      <Check className="size-3" />
                    </motion.span>
                  ) : null}
                </motion.button>
              );
            })}
          </div>

          <form className="mt-7 grid gap-4 sm:grid-cols-2" onSubmit={concluir} noValidate>
            <Field
              label="Nome da instituição"
              name="nome"
              autoComplete="organization"
              required
              value={campos.nome}
              onChange={definir("nome")}
              placeholder={
                tipo === "hospital" ? "Hospital Santa Clara" : "Hemocentro Regional Recife"
              }
              className="sm:col-span-2"
            />
            <Field
              label="CNPJ"
              name="cnpj"
              required
              value={campos.cnpj}
              onChange={definir("cnpj")}
              placeholder="00.000.000/0001-00"
            />
            <Field
              label="Telefone"
              name="telefone"
              autoComplete="tel"
              value={campos.telefone}
              onChange={definir("telefone")}
              placeholder="(81) 3000-0000"
            />
            <Field
              label="Endereço"
              name="endereco"
              autoComplete="street-address"
              value={campos.endereco}
              onChange={definir("endereco")}
              placeholder="Av. Domingos Ferreira, 1240 — Recife/PE"
              className="sm:col-span-2"
            />
            <Field
              label="E-mail"
              name="email"
              type="email"
              autoComplete="email"
              required
              value={campos.email}
              onChange={definir("email")}
              placeholder="contato@instituicao.org.br"
            />
            <Field
              label="Senha"
              name="senha"
              type="password"
              autoComplete="new-password"
              required
              value={campos.senha}
              onChange={definir("senha")}
              placeholder="••••••••"
            />

            {erro ? (
              <p role="alert" className="text-sm font-medium text-destructive sm:col-span-2">
                {erro}
              </p>
            ) : null}

            <div className="sm:col-span-2">
              <motion.button
                type="submit"
                whileHover={{ y: -2 }}
                whileTap={{ scale: 0.97 }}
                className="mt-2 flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground shadow-card"
              >
                <Building2 className="size-4" aria-hidden="true" /> Concluir cadastro
              </motion.button>
            </div>
          </form>

          <p className="mt-5 text-center text-sm text-muted-foreground">
            Já tem conta?{" "}
            <Link to="/login" className="font-semibold text-primary hover:underline">
              Entrar
            </Link>
          </p>
        </div>
      </motion.div>
    </div>
  );
}
