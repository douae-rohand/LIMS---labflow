import { useQuery } from "@tanstack/react-query";
import { getSession } from "@/api/session";
import * as audit from "@/api/audit";
import type { ListerAuditParams } from "@/api/audit";

export function useAudit(params: ListerAuditParams) {
  const enabled = getSession()?.user.permissions.includes("AUDIT_CONSULTER") === true;

  const liste = useQuery({
    queryKey: ["audit", params],
    queryFn: () => audit.listerAudit({ ...params, size: params.size ?? 10 }),
    enabled,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
    placeholderData: (prev) => prev,
  });

  return { liste };
}
