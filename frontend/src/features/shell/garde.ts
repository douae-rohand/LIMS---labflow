import { redirect } from "@tanstack/react-router";
import { peut } from "@/api/session";

/** Garde de route : renvoie vers l'accueil du laboratoire si la permission manque. */
export function exigerPermission(code: string): void {
  if (!peut(code)) {
    throw redirect({ to: "/app" });
  }
}
