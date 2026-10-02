import { cn } from "@/lib/utils";

/** Photo avec voile mint/sarcelle, coins arrondis et reflet glossy. */
export function Photo({
  src,
  alt,
  className,
  imgClassName,
  eager,
}: {
  src: string;
  alt: string;
  className?: string | undefined;
  imgClassName?: string;
  eager?: boolean;
}) {
  return (
    <div
      className={cn(
        "photo-tint glossy-top rounded-3xl border border-white/60 bg-mint-200 shadow-glossy",
        className,
      )}
    >
      <img
        src={src}
        alt={alt}
        loading={eager ? "eager" : "lazy"}
        decoding="async"
        className={cn("size-full object-cover", imgClassName)}
      />
    </div>
  );
}
