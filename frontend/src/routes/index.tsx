import { createFileRoute, Link } from "@tanstack/react-router";
import { motion } from "motion/react";
import {
  ArrowRight,
  Boxes,
  ClipboardList,
  Droplet,
  Hospital,
  ShieldCheck,
  Thermometer,
  Truck,
} from "lucide-react";
import heroImg from "@/assets/hero-hemolink.jpg";
import { Logo, ThemeToggle } from "@/components/app-shell";
import { Stagger, StaggerItem, fadeUp } from "@/components/ui-kit";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "HemoTrack — Gestão e distribuição de hemocomponentes" },
      {
        name: "description",
        content:
          "Plataforma que conecta hospitais e hemocentros para solicitar, liberar e rastrear hemocomponentes em tempo real.",
      },
      { property: "og:title", content: "HemoTrack — Gestão e distribuição de hemocomponentes" },
      {
        property: "og:description",
        content:
          "Solicitações, estoque de bolsas, distribuição e monitoramento de transporte em uma única plataforma.",
      },
    ],
  }),
  component: Landing,
});

const recursos = [
  {
    icon: ClipboardList,
    titulo: "Solicitações rastreáveis",
    texto: "Hospitais registram pedidos com componente, tipo sanguíneo e prioridade clínica.",
  },
  {
    icon: Boxes,
    titulo: "Estoque por bolsa",
    texto: "Hemocentros controlam validade, armazenamento e compatibilidade de cada bolsa.",
  },
  {
    icon: Truck,
    titulo: "Distribuição integrada",
    texto: "Rota, responsável e etapas de separação acompanhadas do aceite até a entrega.",
  },
  {
    icon: Thermometer,
    titulo: "Cadeia de frio monitorada",
    texto: "Temperatura e localização simuladas em tempo real, com alertas automáticos.",
  },
];

const etapas = [
  { n: "01", t: "Hospital solicita", d: "Componente, tipo sanguíneo, quantidade e prioridade." },
  { n: "02", t: "Hemocentro analisa", d: "Aceita ou recusa com motivo e seleciona as bolsas." },
  { n: "03", t: "Transporte", d: "Rota e cadeia de frio monitoradas ponta a ponta." },
  { n: "04", t: "Entrega confirmada", d: "Registro da entrega e histórico completo da bolsa." },
];

function Landing() {
  return (
    <div className="min-h-screen bg-background">
      <header className="sticky top-0 z-40 border-b border-border bg-background/80 backdrop-blur-md">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-4 sm:px-6">
          <Logo />
          <div className="flex items-center gap-2">
            <ThemeToggle />
            <Link
              to="/login"
              className="rounded-xl px-4 py-2 text-sm font-semibold text-foreground transition-colors hover:text-primary"
            >
              Entrar
            </Link>
            <Link to="/cadastro">
              <motion.span
                whileHover={{ y: -2 }}
                whileTap={{ scale: 0.96 }}
                className="inline-flex items-center gap-1.5 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground shadow-card"
              >
                Cadastrar <ArrowRight className="size-4" />
              </motion.span>
            </Link>
          </div>
        </div>
      </header>

      <section className="relative overflow-hidden">
        <div className="surface-grid pointer-events-none absolute inset-0 opacity-70" />
        <div className="relative mx-auto grid max-w-6xl gap-12 px-4 py-16 sm:px-6 lg:grid-cols-[1.05fr_0.95fr] lg:items-center lg:py-24">
          <motion.div {...fadeUp}>
            <span className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary-soft px-3 py-1.5 text-xs font-semibold text-primary">
              <Droplet className="size-3.5" /> Rede de hemocomponentes
            </span>
            <h1 className="mt-6 text-4xl font-semibold leading-[1.05] sm:text-6xl">
              Cada bolsa no <span className="text-gradient-primary">lugar certo</span>, na hora
              certa.
            </h1>
            <p className="mt-5 max-w-lg text-base leading-relaxed text-muted-foreground">
              O HemoTrack conecta hospitais e hemocentros em um fluxo único: solicitação, análise,
              separação de bolsas, transporte refrigerado e entrega — tudo visível em tempo real.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to="/hospital">
                <motion.span
                  whileHover={{ y: -3 }}
                  whileTap={{ scale: 0.96 }}
                  className="inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-3 text-sm font-semibold text-primary-foreground shadow-lift"
                >
                  <Hospital className="size-4" /> Entrar como hospital
                </motion.span>
              </Link>
              <Link to="/hemocentro">
                <motion.span
                  whileHover={{ y: -3 }}
                  whileTap={{ scale: 0.96 }}
                  className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-5 py-3 text-sm font-semibold shadow-card"
                >
                  <Droplet className="size-4 text-primary" /> Entrar como hemocentro
                </motion.span>
              </Link>
            </div>
            <dl className="mt-12 grid max-w-md grid-cols-3 gap-6">
              {[
                ["128", "instituições"],
                ["4.2k", "bolsas/mês"],
                ["18min", "tempo médio"],
              ].map(([v, l]) => (
                <div key={l}>
                  <dt className="font-display text-2xl font-semibold text-primary">{v}</dt>
                  <dd className="text-xs text-muted-foreground">{l}</dd>
                </div>
              ))}
            </dl>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, scale: 0.96, y: 24 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            transition={{ duration: 0.7, ease: [0.16, 1, 0.3, 1] }}
            className="relative"
          >
            <img
              src={heroImg}
              alt="Profissional de saúde acondicionando bolsas de hemocomponentes em caixa térmica"
              width={1280}
              height={960}
              className="w-full rounded-3xl border border-border object-cover shadow-lift"
            />
            <motion.div
              animate={{ y: [0, -10, 0] }}
              transition={{ duration: 5, repeat: Infinity, ease: "easeInOut" }}
              className="absolute -bottom-6 left-4 rounded-2xl border border-border bg-card p-4 shadow-lift sm:left-8"
            >
              <div className="flex items-center gap-3">
                <span className="grid size-9 place-items-center rounded-xl bg-success-soft text-success">
                  <Thermometer className="size-4" />
                </span>
                <div>
                  <p className="text-sm font-semibold">4,2 °C estável</p>
                  <p className="text-xs text-muted-foreground">TRP-501 · 12 min para entrega</p>
                </div>
              </div>
            </motion.div>
          </motion.div>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
        <h2 className="max-w-xl text-2xl font-semibold sm:text-3xl">
          Um sistema para os dois lados da rede
        </h2>
        <Stagger className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {recursos.map((r) => (
            <StaggerItem key={r.titulo}>
              <motion.article
                whileHover={{ y: -6 }}
                transition={{ type: "spring", stiffness: 300, damping: 20 }}
                className="h-full rounded-2xl border border-border bg-card p-5 shadow-card"
              >
                <span className="grid size-10 place-items-center rounded-xl bg-primary-soft text-primary">
                  <r.icon className="size-5" />
                </span>
                <h3 className="mt-4 text-base font-semibold">{r.titulo}</h3>
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{r.texto}</p>
              </motion.article>
            </StaggerItem>
          ))}
        </Stagger>
      </section>

      <section className="border-y border-border bg-secondary/50">
        <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
          <h2 className="text-2xl font-semibold sm:text-3xl">Como funciona</h2>
          <Stagger className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {etapas.map((e) => (
              <StaggerItem key={e.n}>
                <div className="border-t-2 border-primary/30 pt-4">
                  <span className="font-display text-sm font-bold text-primary">{e.n}</span>
                  <h3 className="mt-2 text-base font-semibold">{e.t}</h3>
                  <p className="mt-1.5 text-sm text-muted-foreground">{e.d}</p>
                </div>
              </StaggerItem>
            ))}
          </Stagger>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
        <motion.div
          {...fadeUp}
          className="flex flex-col items-start justify-between gap-6 rounded-3xl border border-border bg-card p-8 shadow-card sm:flex-row sm:items-center sm:p-10"
        >
          <div>
            <h2 className="text-2xl font-semibold">Pronto para conectar sua instituição?</h2>
            <p className="mt-2 max-w-md text-sm text-muted-foreground">
              Cadastre seu hospital ou hemocentro e comece a operar com rastreabilidade completa.
            </p>
          </div>
          <Link to="/cadastro">
            <motion.span
              whileHover={{ y: -3 }}
              whileTap={{ scale: 0.96 }}
              className="inline-flex items-center gap-2 rounded-xl bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground shadow-lift"
            >
              Criar conta <ArrowRight className="size-4" />
            </motion.span>
          </Link>
        </motion.div>
      </section>

      <footer className="border-t border-border">
        <div className="mx-auto flex max-w-6xl flex-col gap-3 px-4 py-8 text-xs text-muted-foreground sm:flex-row sm:items-center sm:justify-between sm:px-6">
          <Logo />
          <p className="flex items-center gap-1.5">
            <ShieldCheck className="size-3.5" /> Protótipo visual · dados fictícios
          </p>
        </div>
      </footer>
    </div>
  );
}
