import { FileText, Replace, Trash2, Upload } from "lucide-react";
import { useMemo, useRef, useState } from "react";
import { Badge, Button } from "@/components/lab";
import {
  FORMAT_DOCUMENTS_ACCEPTES,
  LIBELLES_DOCUMENTS,
  TAILLE_MAX_DOCUMENT,
  TYPES_DOCUMENTS,
  type TypeDocumentIntegration,
} from "./constants";
import { cn } from "@/lib/utils";

type DocumentUploaderProps = {
  files: Partial<Record<TypeDocumentIntegration, File>>;
  onChange: (type: TypeDocumentIntegration, file: File | null) => void;
  errors?: Partial<Record<TypeDocumentIntegration, string>>;
};

function formatTaille(bytes: number): string {
  if (bytes < 1024) return `${bytes} o`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} Ko`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
}

function DocumentSlot({
  type,
  file,
  error,
  preview,
  onChange,
}: {
  type: TypeDocumentIntegration;
  file?: File | undefined;
  error?: string | undefined;
  preview?: string | undefined;
  onChange: (file: File | null) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [tailleError, setTailleError] = useState<string | null>(null);
  const meta = LIBELLES_DOCUMENTS[type];
  const isImage = file?.type.startsWith("image/");
  const message = tailleError || error;

  const appliquerFichier = (next: File | null) => {
    if (next && next.size > TAILLE_MAX_DOCUMENT) {
      setTailleError("Fichier trop volumineux (10 Mo max).");
      return;
    }
    setTailleError(null);
    onChange(next);
  };

  const ouvrirSelecteur = () => {
    const input = inputRef.current;
    if (!input) return;
    input.value = "";
    input.click();
  };

  return (
    <div
      className={cn(
        "rounded-3xl border bg-card p-4",
        message ? "border-red-300" : "border-brand-900/10",
      )}
    >
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="font-semibold text-ink-900">{meta.libelle}</p>
          <p className="mt-1 text-sm text-muted-foreground">{meta.raison}</p>
        </div>
        <Badge variant="mint" size="sm">
          Obligatoire
        </Badge>
      </div>

      <input
        ref={inputRef}
        type="file"
        className="sr-only"
        accept={FORMAT_DOCUMENTS_ACCEPTES}
        onChange={(event) => {
          appliquerFichier(event.target.files?.[0] ?? null);
          event.target.value = "";
        }}
      />

      {file ? (
        <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center">
          {isImage && preview ? (
            <img src={preview} alt={file.name} className="h-20 w-20 rounded-2xl object-cover" />
          ) : (
            <div className="flex size-20 items-center justify-center rounded-2xl bg-surface-card">
              <FileText className="size-7 text-brand-900" />
            </div>
          )}
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-semibold text-ink-900">{file.name}</p>
            <p className="text-xs text-muted-foreground">
              {file.type || "fichier"} · {formatTaille(file.size)}
            </p>
          </div>
          <div className="flex gap-2">
            <Button
              type="button"
              variant="secondary"
              size="sm"
              icon={<Replace className="size-4" />}
              onClick={ouvrirSelecteur}
            >
              Remplacer
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              icon={<Trash2 className="size-4" />}
              onClick={() => {
                setTailleError(null);
                onChange(null);
              }}
            >
              Retirer
            </Button>
          </div>
        </div>
      ) : (
        <button
          type="button"
          className="mt-4 flex w-full cursor-pointer flex-col items-center justify-center rounded-2xl border border-dashed border-brand-900/20 bg-surface-card px-4 py-6 text-center"
          onClick={ouvrirSelecteur}
        >
          <Upload className="size-6 text-brand-900" />
          <span className="mt-2 text-sm font-semibold text-ink-900">Déposer ou choisir un fichier</span>
          <span className="mt-1 text-xs text-muted-foreground">PDF, JPG, PNG, WEBP — 10 Mo max</span>
        </button>
      )}
      {message ? <p className="mt-2 text-sm text-red-600">{message}</p> : null}
    </div>
  );
}

export function DocumentUploader({ files, onChange, errors }: DocumentUploaderProps) {
  const previews = useMemo(() => {
    const urls: Partial<Record<TypeDocumentIntegration, string>> = {};
    TYPES_DOCUMENTS.forEach((type) => {
      const file = files[type];
      if (file) urls[type] = URL.createObjectURL(file);
    });
    return urls;
  }, [files]);

  return (
    <div className="flex flex-col gap-4">
      <p className="text-sm text-muted-foreground">
        6 documents obligatoires · PDF, JPG, PNG ou WEBP · 10 Mo maximum par fichier
      </p>
      {TYPES_DOCUMENTS.map((type) => (
        <DocumentSlot
          key={type}
          type={type}
          file={files[type]}
          error={errors?.[type]}
          preview={previews[type]}
          onChange={(file) => onChange(type, file)}
        />
      ))}
    </div>
  );
}
