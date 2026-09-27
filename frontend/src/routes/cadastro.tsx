import { useQueryClient } from "@tanstack/react-query";

import { cadastrarInstituicao } from "@/lib/api-auth";
import { ErroDeApiHttp } from "@/lib/httpClient";
import { autenticarEGuardarSessao, destinoDoUsuario } from "@/lib/sessao-login";

import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { motion } from "motion/react";
import { useState } from "react";
import { Building2, Check, Droplet, Hospital } from "lucide-react";
import { toast } from "sonner";
import { Logo } from "@/components/app-shell";
import { Field, fadeUp } from "@/components/ui-kit";
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
  municipio: string;
  responsavel: string;
  email: string;
  senha: string;
};

const vazio: Campos = {
  nome: "",
  cnpj: "",
  telefone: "",
  endereco: "",
  municipio: "",
  responsavel: "",
  email: "",
  senha: "",
};

/**
 * O backend identifica o campo pelo caminho dentro do JSON
 * ("instituicao.razaoSocial"); aqui os campos são planos. Este mapa liga
 * os dois para que o erro apareça no input certo.
 */
const CAMPO_DO_BACK: Record<string, keyof Campos> = {
  "instituicao.razaoSocial": "nome",
  "instituicao.cnpj": "cnpj",
  "instituicao.endereco": "endereco",
  "instituicao.municipio": "municipio",
  "instituicao.telefone": "telefone",
  "administrador.nome": "responsavel",
  "administrador.email": "email",
  "administrador.senha": "senha",
};

function CadastroPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [tipo, setTipo] = useState<"hospital" | "hemocentro">("hospital");
  const [campos, setCampos] = useState<Campos>(vazio);
  const [erro, setErro] = useState<string | null>(null);
  const [errosPorCampo, setErrosPorCampo] = useState<Partial<Record<keyof Campos, string>>>({});
  const [enviando, setEnviando] = useState(false);

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

  async function concluir(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();

    const obrigatorios = [
      "nome",
      "cnpj",
      "endereco",
      "municipio",
      "responsavel",
      "email",
      "senha",
    ] as const;

    const faltando = obrigatorios.filter((chave) => !campos[chave].trim());

    if (faltando.length > 0) {
      setErro("Preencha todos os campos obrigatórios para concluir o cadastro.");
      return;
    }

    setErro(null);
    setErrosPorCampo({});
    setEnviando(true);

    try {
      await cadastrarInstituicao({
        instituicao: {
          razaoSocial: campos.nome.trim(),
          cnpj: campos.cnpj.trim(),
          tipo: tipo === "hemocentro" ? "HEMOCENTRO" : "HOSPITAL",
          endereco: campos.endereco.trim(),
          municipio: campos.municipio.trim(),
          telefone: campos.telefone.trim() || null,
        },
        administrador: {
          nome: campos.responsavel.trim(),
          email: campos.email.trim(),
          senha: campos.senha,
        },
      });

      // Entra direto com as credenciais que acabaram de ser criadas.
      const usuario = await autenticarEGuardarSessao(
        queryClient,
        campos.email.trim(),
        campos.senha,
      );

      toast.success("Instituição cadastrada", {
        description: "Seu cadastro entrou em análise.",
      });

      // TODO(passo 8): com a instituição PENDENTE_APROVACAO, o destino
      // correto é a tela de espera — hoje cai no painel.
      await navigate({ to: destinoDoUsuario(usuario) });
    } catch (falha) {
      if (falha instanceof ErroDeApiHttp) {
        setErro(falha.message);

        const novosErros: Partial<Record<keyof Campos, string>> = {};

        for (const erroDeCampo of falha.erros) {
          const campo = CAMPO_DO_BACK[erroDeCampo.campo];

          if (campo !== undefined) {
            novosErros[campo] = erroDeCampo.mensagem;
          }
        }

        setErrosPorCampo(novosErros);
      } else {
        setErro("Não foi possível concluir o cadastro. Verifique se o servidor está no ar.");
      }
    } finally {
      setEnviando(false);
    }
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

          <div
            role="radiogroup"
            aria-label="Tipo de instituição"
            className="mt-6 grid gap-3 sm:grid-cols-2"
          >
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
              erro={errosPorCampo.nome ?? null}
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
              erro={errosPorCampo.cnpj ?? null}
              placeholder="00.000.000/0001-00"
            />
            <Field
              label="Telefone"
              name="telefone"
              autoComplete="tel"
              value={campos.telefone}
              onChange={definir("telefone")}
              erro={errosPorCampo.telefone ?? null}
              placeholder="(81) 3000-0000"
            />
            <Field
              label="Endereço"
              name="endereco"
              autoComplete="street-address"
              required
              value={campos.endereco}
              onChange={definir("endereco")}
              erro={errosPorCampo.endereco ?? null}
              placeholder="Av. Domingos Ferreira, 1240"
            />
            <Field
              label="Município"
              name="municipio"
              autoComplete="address-level2"
              required
              value={campos.municipio}
              onChange={definir("municipio")}
              erro={errosPorCampo.municipio ?? null}
              placeholder="Recife"
            />

            <div className="mt-2 border-t border-border pt-4 sm:col-span-2">
              <h2 className="text-sm font-semibold">Responsável pela conta</h2>
              <p className="mt-0.5 text-xs text-muted-foreground">
                Esta pessoa será a administradora da instituição no HemoTrack.
              </p>
            </div>

            <Field
              label="Nome do responsável"
              name="responsavel"
              autoComplete="name"
              required
              value={campos.responsavel}
              onChange={definir("responsavel")}
              erro={errosPorCampo.responsavel ?? null}
              placeholder="Maria Souza"
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
              erro={errosPorCampo.email ?? null}
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
              erro={errosPorCampo.senha ?? null}
              placeholder="Mínimo de 6 caracteres"
            />

            {erro ? (
              <p role="alert" className="text-sm font-medium text-destructive sm:col-span-2">
                {erro}
              </p>
            ) : null}

            <div className="sm:col-span-2">
              <motion.button
                type="submit"
                disabled={enviando}
                whileHover={{ y: enviando ? 0 : -2 }}
                whileTap={{ scale: enviando ? 1 : 0.97 }}
                className="mt-2 flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground shadow-card disabled:opacity-60"
              >
                <Building2 className="size-4" aria-hidden="true" />
                {enviando ? "Cadastrando…" : "Concluir cadastro"}
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
