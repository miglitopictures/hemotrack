/**
 * Gestão de membros de uma instituição.
 *
 * Só o ADMIN_INSTITUICAO chega aqui — quem garante isso são as guardas das
 * rotas /hospital/membros e /hemocentro/membros, e, de verdade, o backend.
 *
 * O painel é o mesmo para hospital e hemocentro: gerir pessoas não depende
 * do tipo da instituição, só do papel de quem está logado.
 */

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Check, ShieldCheck, UserPlus, X } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { EstadoErro, EstadoVazio, Field, Panel, Skeleton } from "@/components/ui-kit";
import type { UsuarioDoBack } from "@/lib/api-auth";
import {
  type OperadorRequest,
  alterarAtivoMembro,
  cadastrarOperador,
  chaveMembros,
  listarMembros,
} from "@/lib/api-membros";
import { ErroDeApiHttp } from "@/lib/httpClient";
import { cn } from "@/lib/utils";

type Campos = OperadorRequest;

const vazio: Campos = { nome: "", email: "", senha: "" };

export function MembrosPainel({ instituicaoId }: { instituicaoId: number }) {
  const queryClient = useQueryClient();

  const [campos, setCampos] = useState<Campos>(vazio);
  const [errosPorCampo, setErrosPorCampo] = useState<Partial<Record<keyof Campos, string>>>({});

  // Id do operador cuja desativação está esperando confirmação na linha.
  const [confirmando, setConfirmando] = useState<number | null>(null);

  const membros = useQuery({
    queryKey: chaveMembros(instituicaoId),
    queryFn: () => listarMembros(instituicaoId),
  });

  const criacao = useMutation({
    mutationFn: (dados: Campos) => cadastrarOperador(instituicaoId, dados),

    onSuccess: (operador) => {
      toast.success(`${operador.nome} já pode entrar no sistema`);
      setCampos(vazio);
      setErrosPorCampo({});

      void queryClient.invalidateQueries({ queryKey: chaveMembros(instituicaoId) });
    },

    onError: (falha) => {
      if (!(falha instanceof ErroDeApiHttp)) {
        toast.error("Não foi possível cadastrar o operador.");
        return;
      }

      // E-mail já cadastrado: o erro pertence a um campo, não a um toast que
      // some e deixa o formulário do jeito que estava.
      if (falha.status === 409) {
        setErrosPorCampo({ email: falha.message });
        return;
      }

      // Aqui os campos do back são planos ("nome", "email", "senha"). O
      // cadastro.tsx precisa de um mapa de tradução porque lá o corpo é
      // aninhado ("administrador.email"); neste não tem o que traduzir.
      if (falha.erros.length > 0) {
        const novos: Partial<Record<keyof Campos, string>> = {};

        for (const erroDeCampo of falha.erros) {
          if (erroDeCampo.campo in vazio) {
            novos[erroDeCampo.campo as keyof Campos] = erroDeCampo.mensagem;
          }
        }

        setErrosPorCampo(novos);
        return;
      }

      toast.error(falha.message);
    },
  });

  const alteracao = useMutation({
    mutationFn: (alvo: { usuarioId: number; ativo: boolean }) =>
      alterarAtivoMembro(instituicaoId, alvo.usuarioId, alvo.ativo),

    onSuccess: (membro) => {
      toast.success(
        membro.ativo ? `${membro.nome} voltou a ter acesso` : `${membro.nome} não entra mais`,
      );
      setConfirmando(null);

      void queryClient.invalidateQueries({ queryKey: chaveMembros(instituicaoId) });
    },

    onError: (falha) => {
      toast.error(
        falha instanceof ErroDeApiHttp ? falha.message : "Não foi possível alterar o acesso.",
      );
      setConfirmando(null);
    },
  });

  function definir(chave: keyof Campos) {
    return (valor: string) => setCampos((atual) => ({ ...atual, [chave]: valor }));
  }

  function enviar(evento: React.FormEvent) {
    evento.preventDefault();
    setErrosPorCampo({});

    criacao.mutate({
      nome: campos.nome.trim(),
      email: campos.email.trim(),
      senha: campos.senha,
    });
  }

  function acoesDoOperador(membro: UsuarioDoBack) {
    if (!membro.ativo) {
      return (
        <div className="flex items-center gap-2">
          <span className="rounded-lg bg-secondary px-2.5 py-1 text-xs font-semibold text-muted-foreground">
            Sem acesso
          </span>
          <button
            type="button"
            disabled={alteracao.isPending}
            onClick={() => alteracao.mutate({ usuarioId: membro.id, ativo: true })}
            className="rounded-xl border border-border px-3 py-1.5 text-xs font-semibold transition-colors hover:border-primary/40 hover:text-primary disabled:opacity-60"
          >
            Reativar
          </button>
        </div>
      );
    }

    // Reativar é inofensivo e vai direto. Tirar o acesso de alguém no meio do
    // plantão, não: confirma na própria linha, onde o nome está à vista.
    if (confirmando === membro.id) {
      return (
        <div className="flex items-center gap-2">
          <span className="text-xs text-muted-foreground">Tirar o acesso?</span>
          <button
            type="button"
            disabled={alteracao.isPending}
            onClick={() => alteracao.mutate({ usuarioId: membro.id, ativo: false })}
            className="inline-flex items-center gap-1.5 rounded-xl bg-destructive px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-60"
          >
            <Check className="size-3.5" /> Confirmar
          </button>
          <button
            type="button"
            onClick={() => setConfirmando(null)}
            aria-label="Cancelar"
            className="rounded-xl border border-border p-1.5 transition-colors hover:text-primary"
          >
            <X className="size-3.5" />
          </button>
        </div>
      );
    }

    return (
      <button
        type="button"
        onClick={() => setConfirmando(membro.id)}
        className="rounded-xl border border-border px-3 py-1.5 text-xs font-semibold transition-colors hover:border-destructive/40 hover:text-destructive"
      >
        Desativar
      </button>
    );
  }

  function conteudoDaLista() {
    if (membros.isPending) {
      return (
        <div className="grid gap-2 px-5 py-5">
          <Skeleton className="h-14" />
          <Skeleton className="h-14" />
        </div>
      );
    }

    if (membros.isError) {
      return (
        <div className="px-5 py-5">
          <EstadoErro erro={membros.error as Error} aoTentarDeNovo={() => void membros.refetch()} />
        </div>
      );
    }

    // A lista nunca vem vazia de verdade: o administrador sempre está nela.
    // O estado vazio é sobre não haver OPERADOR, que é o que interessa aqui.
    const operadores = membros.data.filter((membro) => membro.papel === "OPERADOR");

    if (operadores.length === 0) {
      return (
        <EstadoVazio
          titulo="Nenhum operador ainda"
          descricao="Cadastre quem vai usar o sistema no dia a dia. Você continua sendo o único administrador."
        />
      );
    }

    return (
      <ul className="divide-y divide-border">
        {membros.data.map((membro) => (
          <li key={membro.id} className="flex flex-wrap items-center gap-3 px-5 py-4">
            <div className="min-w-0 flex-1">
              <p
                className={cn(
                  "truncate text-sm font-semibold",
                  !membro.ativo && "text-muted-foreground",
                )}
              >
                {membro.nome}
              </p>
              <p className="truncate text-xs text-muted-foreground">{membro.email}</p>
            </div>

            {membro.papel === "ADMIN_INSTITUICAO" ? (
              // Sem botão: o backend responde 409 para desativar administrador,
              // e oferecer uma ação que sempre falha é pior que não oferecer.
              <span className="inline-flex items-center gap-1.5 rounded-lg bg-primary-soft px-2.5 py-1 text-xs font-semibold text-primary">
                <ShieldCheck className="size-3.5" /> Administrador
              </span>
            ) : (
              acoesDoOperador(membro)
            )}
          </li>
        ))}
      </ul>
    );
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
      <Panel title="Membros" description="Quem tem acesso ao painel desta instituição.">
        {conteudoDaLista()}
      </Panel>

      <Panel title="Novo operador" description="Ele entra com o e-mail e a senha definidos aqui.">
        <form className="grid gap-4 px-5 py-5" onSubmit={enviar} noValidate>
          <Field
            label="Nome"
            name="nome"
            placeholder="Maria Souza"
            // O admin está cadastrando OUTRA pessoa: o preenchimento
            // automático do navegador aqui só serviria para criar um
            // operador com o e-mail de quem está logado.
            autoComplete="off"
            required
            value={campos.nome}
            onChange={definir("nome")}
            erro={errosPorCampo.nome ?? null}
          />
          <Field
            label="E-mail"
            name="email"
            type="email"
            placeholder="maria@hemope.gov.br"
            autoComplete="off"
            required
            value={campos.email}
            onChange={definir("email")}
            erro={errosPorCampo.email ?? null}
          />
          <Field
            label="Senha inicial"
            name="senha"
            type="password"
            placeholder="mínimo 6 caracteres"
            autoComplete="new-password"
            required
            value={campos.senha}
            onChange={definir("senha")}
            erro={errosPorCampo.senha ?? null}
          />

          <p className="text-xs text-muted-foreground">
            Combine esta senha com a pessoa: por enquanto ela não consegue trocá-la sozinha.
          </p>

          <button
            type="submit"
            disabled={criacao.isPending}
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground transition-opacity disabled:opacity-60"
          >
            <UserPlus className="size-4" />
            {criacao.isPending ? "Cadastrando…" : "Cadastrar operador"}
          </button>
        </form>
      </Panel>
    </div>
  );
}
