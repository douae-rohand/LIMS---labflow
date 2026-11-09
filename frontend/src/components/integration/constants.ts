// Les documents exigés et les types de laboratoire sont paramétrés par le
// Super Administrateur : voir hooks/useParametresPublics.ts.

export const REGIONS_MAROC = [
  "Tanger-Tétouan-Al Hoceïma",
  "L'Oriental",
  "Fès-Meknès",
  "Rabat-Salé-Kénitra",
  "Béni Mellal-Khénifra",
  "Casablanca-Settat",
  "Marrakech-Safi",
  "Drâa-Tafilalet",
  "Souss-Massa",
  "Guelmim-Oued Noun",
  "Laâyoune-Sakia El Hamra",
  "Dakhla-Oued Ed-Dahab",
] as const;

export const FORMAT_DOCUMENTS_ACCEPTES = "application/pdf,image/jpeg,image/png,image/webp";
export const TAILLE_MAX_DOCUMENT = 10 * 1024 * 1024;
