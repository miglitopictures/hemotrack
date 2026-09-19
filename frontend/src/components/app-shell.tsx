import { Link, useNavigate, useRouterState } from "@tanstack/react-router";
import { AnimatePresence, motion } from "motion/react";
import { useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import {
  BarChart3,
  Boxes,
  ClipboardList,
  Droplet,
  Inbox,
  LayoutDashboard,
  LogOut,
  Menu,
  Moon,
  PlusCircle,
  Search,
  Sun,
  Truck,
  X,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { buscar, type ResultadoBusca } from "@/lib/data";

type Papel = "hospital" | "hemocentro";
type NavItem = { label: string; to: string; icon: typeof Droplet };

const hospitalNav: NavItem[] = [
  { label: "Painel", to: "/hospital", icon: LayoutDashboard },
  { label: "Nova solicitação", to: "/hospital/nova-solicitacao", icon: PlusCircle },
  { label: "Minhas solicitações", to: "/hospital/solicitacoes", icon: ClipboardList },
  { label: "Transportes", to: "/hospital/transportes", icon: Truck },
];

const hemocentroNav: NavItem[] = [
  { label: "Painel", to: "/hemocentro", icon: LayoutDashboard },
  { label: "Solicitações recebidas", to: "/hemocentro/solicitacoes", icon: Inbox },
  { label: "Estoque", to: "/hemocentro/estoque", icon: Boxes },
  { label: "Distribuição", to: "/hemocentro/distribuicoes", icon: Truck },
  { label: "Indicadores", to: "/hemocentro/indicadores", icon: BarChart3 },
];

export function Logo({ className }: { className?: string }) {
  return (
    <Link to="/" className={cn("flex items-center gap-2.5", className)}>
      <motion.span
        whileHover={{ rotate: -10, scale: 1.08 }}
        transition={{ type: "spring", stiffness: 400, damping: 15 }}
        className="grid size-9 place-items-center rounded-xl bg-primary text-primary-foreground shadow-card"
      >
        <Droplet className="size-5" />
      </motion.span>
      <span className="font-display text-lg font-semibold tracking-tight">
        Hemo<span className="text-primary">Track</span>
      </span>
    </Link>
  );
}

export function ThemeToggle() {
  // O tema já foi aplicado por um script bloqueante no <head> (ver __root.tsx),
  // então aqui só espelhamos o estado que está no DOM — sem piscar.
  const [dark, setDark] = useState(false);
  const [pronto, setPronto] = useState(false);

  useEffect(() => {
    setDark(document.documentElement.classList.contains("dark"));
    setPronto(true);
  }, []);

  function alternar() {
    const novo = !dark;
    setDark(novo);
    document.documentElement.classList.toggle("dark", novo);
    try {
      localStorage.setItem("hemotrack-theme", novo ? "dark" : "light");
    } catch {
      // modo privativo / storage bloqueado: só não persiste
    }
  }

  return (
    <motion.button
      type="button"
      whileTap={{ scale: 0.92 }}
      onClick={alternar}
      aria-label={dark ? "Ativar tema claro" : "Ativar tema escuro"}
      className={cn(
        "relative grid size-9 place-items-center rounded-xl border border-border text-muted-foreground transition-colors hover:text-primary",
        !pronto && "invisible",
      )}
    >
      <AnimatePresence mode="wait" initial={false}>
        <motion.span
          key={dark ? "moon" : "sun"}
          initial={{ opacity: 0, rotate: -90, scale: 0.6 }}
          animate={{ opacity: 1, rotate: 0, scale: 1 }}
          exit={{ opacity: 0, rotate: 90, scale: 0.6 }}
          transition={{ duration: 0.25 }}
          className="grid place-items-center"
        >
          {dark ? <Moon className="size-4" /> : <Sun className="size-4" />}
        </motion.span>
      </AnimatePresence>
    </motion.button>
  );
}

// ---------------------------------------------------------------------------
// Busca global — o campo do header agora funciona de verdade
// ---------------------------------------------------------------------------

function BuscaGlobal({ papel }: { papel: Papel }) {
  const navigate = useNavigate();
  const [termo, setTermo] = useState("");
  const [aberto, setAberto] = useState(false);
  const [indice, setIndice] = useState(0);
  const containerRef = useRef<HTMLDivElement>(null);

  const resultados = useMemo(() => {
    const todos = buscar(termo);
    // Hospital não navega no inventário do hemocentro.
    return papel === "hospital" ? todos.filter((r) => r.tipo !== "Bolsa") : todos;
  }, [termo, papel]);

  useEffect(() => {
    setIndice(0);
  }, [termo]);

  useEffect(() => {
    function aoClicarFora(e: MouseEvent) {
      if (!containerRef.current?.contains(e.target as Node)) setAberto(false);
    }
    document.addEventListener("mousedown", aoClicarFora);
    return () => document.removeEventListener("mousedown", aoClicarFora);
  }, []);

  function irPara(r: ResultadoBusca) {
    setAberto(false);
    setTermo("");

    if (r.tipo === "Solicitação") {
      void navigate(
        papel === "hospital"
          ? { to: "/hospital/solicitacoes/$id", params: { id: r.id } }
          : { to: "/hemocentro/solicitacoes/$id", params: { id: r.id } },
      );
      return;
    }
    if (r.tipo === "Transporte") {
      void navigate(
        papel === "hospital"
          ? { to: "/hospital/transportes/$id", params: { id: r.id } }
          : { to: "/hemocentro/distribuicoes/$id", params: { id: r.id } },
      );
      return;
    }
    void navigate({ to: "/hemocentro/estoque", search: { q: r.id } });
  }

  return (
    <div ref={containerRef} className="relative min-w-0 flex-1 sm:max-w-sm">
      <div className="flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm text-muted-foreground focus-within:border-primary/50">
        <Search className="size-4 shrink-0" aria-hidden="true" />
        <input
          type="search"
          value={termo}
          onChange={(e) => {
            setTermo(e.target.value);
            setAberto(true);
          }}
          onFocus={() => setAberto(true)}
          onKeyDown={(e) => {
            if (e.key === "Escape") {
              setAberto(false);
              return;
            }
            if (resultados.length === 0) return;
            if (e.key === "ArrowDown") {
              e.preventDefault();
              setIndice((i) => (i + 1) % resultados.length);
            } else if (e.key === "ArrowUp") {
              e.preventDefault();
              setIndice((i) => (i - 1 + resultados.length) % resultados.length);
            } else if (e.key === "Enter") {
              e.preventDefault();
              const alvo = resultados[indice];
              if (alvo) irPara(alvo);
            }
          }}
          aria-label="Buscar solicitação, bolsa ou transporte"
          aria-expanded={aberto && termo.trim().length >= 2}
          aria-controls="resultados-busca"
          placeholder="Buscar solicitação, bolsa ou transporte"
          className="w-full bg-transparent text-sm text-foreground outline-none placeholder:text-muted-foreground"
        />
      </div>

      <AnimatePresence>
        {aberto && termo.trim().length >= 2 ? (
          <motion.div
            initial={{ opacity: 0, y: -6 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -6 }}
            transition={{ duration: 0.15 }}
            id="resultados-busca"
            role="listbox"
            className="absolute left-0 right-0 top-full z-50 mt-2 overflow-hidden rounded-2xl border border-border bg-card shadow-lift"
          >
            {resultados.length === 0 ? (
              <p className="px-4 py-4 text-sm text-muted-foreground">
                Nada encontrado para “{termo}”.
              </p>
            ) : (
              <ul className="max-h-80 overflow-y-auto">
                {resultados.map((r, i) => (
                  <li key={`${r.tipo}-${r.id}`}>
                    <button
                      type="button"
                      role="option"
                      aria-selected={i === indice}
                      onMouseEnter={() => setIndice(i)}
                      onClick={() => irPara(r)}
                      className={cn(
                        "flex w-full items-center gap-3 px-4 py-2.5 text-left transition-colors",
                        i === indice ? "bg-primary-soft" : "hover:bg-secondary",
                      )}
                    >
                      <span className="shrink-0 rounded-md border border-border px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
                        {r.tipo}
                      </span>
                      <span className="min-w-0 flex-1">
                        <span className="block truncate text-sm font-semibold">{r.titulo}</span>
                        <span className="block truncate text-xs text-muted-foreground">
                          {r.detalhe}
                        </span>
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </motion.div>
        ) : null}
      </AnimatePresence>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Shell
// ---------------------------------------------------------------------------

export function AppShell({ role, children }: { role: Papel; children: ReactNode }) {
  const [open, setOpen] = useState(false);
  const pathname = useRouterState({ select: (s) => s.location.pathname });
  const nav = role === "hospital" ? hospitalNav : hemocentroNav;
  const perfil =
    role === "hospital"
      ? { nome: "Hospital Santa Clara", tag: "Hospital · Recife/PE", iniciais: "HS" }
      : { nome: "Hemocentro Regional", tag: "Hemocentro · Recife/PE", iniciais: "HR" };

  const navList = (
    <nav aria-label="Navegação principal" className="flex flex-col gap-1">
      {nav.map((item) => {
        const active = pathname === item.to || pathname.startsWith(item.to + "/");
        return (
          <Link
            key={item.to}
            to={item.to}
            onClick={() => setOpen(false)}
            aria-current={active ? "page" : undefined}
            className="relative rounded-xl px-3 py-2.5 text-sm font-medium transition-colors"
          >
            {active ? (
              <motion.span
                layoutId={`nav-${role}`}
                className="absolute inset-0 rounded-xl bg-primary-soft"
                transition={{ type: "spring", stiffness: 400, damping: 32 }}
              />
            ) : null}
            <span
              className={cn(
                "relative flex items-center gap-3",
                active ? "text-primary" : "text-muted-foreground hover:text-foreground",
              )}
            >
              <item.icon className="size-4.5" aria-hidden="true" />
              {item.label}
            </span>
          </Link>
        );
      })}
    </nav>
  );

  const sidebarBody = (
    <div className="flex h-full flex-col gap-6 p-4">
      <Logo className="px-2 pt-1" />
      <div className="rounded-2xl border border-border bg-secondary/60 p-3">
        <p className="text-sm font-semibold leading-tight">{perfil.nome}</p>
        <p className="mt-0.5 text-xs text-muted-foreground">{perfil.tag}</p>
      </div>
      {navList}
      <div className="mt-auto flex flex-col gap-2">
        <Link
          to={role === "hospital" ? "/hemocentro" : "/hospital"}
          className="rounded-xl border border-dashed border-border px-3 py-2.5 text-xs font-medium text-muted-foreground transition-colors hover:border-primary/40 hover:text-primary"
        >
          Ver visão do {role === "hospital" ? "hemocentro" : "hospital"}
        </Link>
        <Link
          to="/login"
          className="flex items-center gap-2 rounded-xl px-3 py-2.5 text-sm font-medium text-muted-foreground transition-colors hover:text-primary"
        >
          <LogOut className="size-4" aria-hidden="true" /> Sair
        </Link>
      </div>
    </div>
  );

  return (
    <div className="flex min-h-screen w-full bg-background">
      <a
        href="#conteudo"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-[60] focus:rounded-xl focus:bg-primary focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-primary-foreground"
      >
        Pular para o conteúdo
      </a>

      <aside className="sticky top-0 hidden h-screen w-70 shrink-0 border-r border-sidebar-border bg-sidebar lg:block">
        {sidebarBody}
      </aside>

      <AnimatePresence>
        {open ? (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setOpen(false)}
              className="fixed inset-0 z-40 bg-foreground/30 backdrop-blur-sm lg:hidden"
            />
            <motion.aside
              initial={{ x: "-100%" }}
              animate={{ x: 0 }}
              exit={{ x: "-100%" }}
              transition={{ type: "spring", stiffness: 320, damping: 32 }}
              className="fixed inset-y-0 left-0 z-50 w-72 border-r border-sidebar-border bg-sidebar lg:hidden"
            >
              <button
                type="button"
                onClick={() => setOpen(false)}
                aria-label="Fechar menu"
                className="absolute right-3 top-4 grid size-9 place-items-center rounded-xl text-muted-foreground hover:bg-secondary"
              >
                <X className="size-4" />
              </button>
              {sidebarBody}
            </motion.aside>
          </>
        ) : null}
      </AnimatePresence>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex items-center gap-3 border-b border-border bg-background/80 px-4 py-3 backdrop-blur-md sm:px-6">
          <button
            type="button"
            onClick={() => setOpen(true)}
            aria-label="Abrir menu"
            className="grid size-9 place-items-center rounded-xl border border-border text-muted-foreground lg:hidden"
          >
            <Menu className="size-4" />
          </button>
          <BuscaGlobal papel={role} />
          <div className="ml-auto flex items-center gap-2">
            <ThemeToggle />
            <div className="hidden items-center gap-2 rounded-xl border border-border bg-card px-3 py-1.5 sm:flex">
              <span className="grid size-7 place-items-center rounded-lg bg-primary-soft text-xs font-bold text-primary">
                {perfil.iniciais}
              </span>
              <span className="text-xs font-medium">{perfil.nome}</span>
            </div>
          </div>
        </header>
        <main
          id="conteudo"
          className="relative mx-auto w-full max-w-7xl flex-1 px-4 py-6 sm:px-6 sm:py-8"
        >
          <div className="surface-grid pointer-events-none absolute inset-x-0 top-0 h-96 opacity-40 [mask-image:linear-gradient(to_bottom,black,transparent)]" />
          <div className="relative space-y-6">{children}</div>
        </main>
      </div>
    </div>
  );
}
