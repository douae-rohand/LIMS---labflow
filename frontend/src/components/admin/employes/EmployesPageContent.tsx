import { useNavigate } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useEffect, useRef, useState } from "react";
import { listerEmployes } from "@/api/employes";
import type { EmployesSearch } from "@/routes/app/employes/index";
import { EmployesFiltres } from "@/components/employes/EmployesFiltres";
import { EmployesTable } from "@/components/employes/EmployesTable";
import { EmployesPagination } from "@/components/employes/EmployesPagination";
import { EmployePanneau } from "@/components/employes/EmployePanneau";
import type { ColTri } from "@/components/employes/EmployesTable";

const PAGE_SIZE = 10;
const DEBOUNCE_MS = 400;
const Q_MIN_LEN = 2;

interface EmployesPageContentProps {
  search: EmployesSearch;
}

export function EmployesPageContent({ search }: EmployesPageContentProps) {
  const navigate = useNavigate();
  const [inputValue, setInputValue] = useState(search.q ?? "");
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isDebouncing = useRef(false);

  useEffect(() => {
    if (debounceTimer.current !== null) clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(() => {
      debounceTimer.current = null;
      const trimmed = inputValue.trim();
      const qUrl = trimmed.length >= Q_MIN_LEN ? trimmed : undefined;
      if (qUrl !== search.q) {
        isDebouncing.current = true;
        void navigate({ to: "/app/employes", search: (prev: EmployesSearch) => ({ ...prev, q: qUrl, page: 1 }), replace: true });
      }
    }, DEBOUNCE_MS);
    return () => { if (debounceTimer.current !== null) clearTimeout(debounceTimer.current); };
  }, [inputValue, search.q, navigate]);

  useEffect(() => {
    if (isDebouncing.current) { isDebouncing.current = false; return; }
    setInputValue(search.q ?? "");
  }, [search.q]);

  const page0 = search.page - 1;
  const query = useQuery({
    queryKey: ["employes", { q: search.q, statut: search.statut, role: search.role, page: page0, tri: search.tri, direction: search.direction }],
    queryFn: () => listerEmployes({ q: search.q, statut: search.statut, role: search.role, page: page0, size: PAGE_SIZE, tri: search.tri, direction: search.direction }),
    placeholderData: (prev) => prev,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  });

  useEffect(() => {
    if (query.data !== undefined && !query.isFetching && query.data.totalPages > 0 && search.page > query.data.totalPages) {
      void navigate({ to: "/app/employes", search: (prev: EmployesSearch) => ({ ...prev, page: query.data!.totalPages }), replace: true });
    }
  }, [query.data, query.isFetching, search.page, navigate]);

  const filtresActifs = (search.q !== undefined && search.q.length > 0) || search.statut !== undefined || search.role !== undefined;

  const handleStatut = (value: string | undefined) => void navigate({ to: "/app/employes", search: (prev: EmployesSearch) => ({ ...prev, statut: value, page: 1 }), replace: true });
  const handleRole = (value: string | undefined) => void navigate({ to: "/app/employes", search: (prev: EmployesSearch) => ({ ...prev, role: value, page: 1 }), replace: true });
  const handleReinitialiser = () => { setInputValue(""); if (debounceTimer.current !== null) { clearTimeout(debounceTimer.current); debounceTimer.current = null; } void navigate({ to: "/app/employes", search: (prev: EmployesSearch) => ({ ...prev, q: undefined, statut: undefined, role: undefined, page: 1 }), replace: true }); };
  const handleInputClear = () => { setInputValue(""); if (debounceTimer.current !== null) { clearTimeout(debounceTimer.current); debounceTimer.current = null; } if (search.q !== undefined) void navigate({ to: "/app/employes", search: (prev: EmployesSearch) => ({ ...prev, q: undefined, page: 1 }), replace: true }); };
  const handleTri = (col: ColTri) => { const nouvelleDirection: "asc" | "desc" = search.tri === col && search.direction === "asc" ? "desc" : "asc"; void navigate({ to: "/app/employes", search: (prev: EmployesSearch) => ({ ...prev, tri: col, direction: nouvelleDirection, page: 1 }), replace: true }); };
  const handlePrecedent = () => { if (search.page > 1) void navigate({ to: "/app/employes", search: (prev: EmployesSearch) => ({ ...prev, page: search.page - 1 }), replace: false }); };
  const handleSuivant = () => { if (query.data && search.page < query.data.totalPages) void navigate({ to: "/app/employes", search: (prev: EmployesSearch) => ({ ...prev, page: search.page + 1 }), replace: false }); };
  const handleSelectEmploye = (id: number) => void navigate({ to: "/app/employes", search: (prev: EmployesSearch) => ({ ...prev, employe: id }), replace: false });
  const handleFermerPanneau = () => void navigate({ to: "/app/employes", search: (prev: EmployesSearch) => ({ ...prev, employe: undefined }), replace: false });

  return (
    <div className="space-y-3">
      <EmployesFiltres inputValue={inputValue} statut={search.statut} role={search.role} filtresActifs={filtresActifs} onInputChange={setInputValue} onInputClear={handleInputClear} onStatutChange={handleStatut} onRoleChange={handleRole} onReinitialiser={handleReinitialiser} />
      <EmployesTable employes={query.data?.content} isLoading={query.isLoading} isError={query.isError} error={query.error} tri={search.tri} direction={search.direction} filtresActifs={filtresActifs} employeSelectionneId={search.employe} onSelectEmploye={handleSelectEmploye} onTriChange={handleTri} onReinitialiser={handleReinitialiser} onReessayer={() => void query.refetch()} />
      {query.data !== undefined && query.data.totalElements > 0 && <EmployesPagination data={query.data} onPrecedent={handlePrecedent} onSuivant={handleSuivant} />}
      <EmployePanneau employeId={search.employe} onClose={handleFermerPanneau} />
    </div>
  );
}
