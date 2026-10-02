import * as CheckboxPrimitive from "@radix-ui/react-checkbox";
import { Check } from "lucide-react";
import type {
  InputHTMLAttributes,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
  ReactNode,
} from "react";
import { cn } from "@/lib/utils";

const field =
  "w-full rounded-2xl border border-input bg-card px-4 py-3 text-sm text-ink-900 placeholder:text-muted-foreground transition-colors duration-200 focus:border-brand-600";

export function Label({
  children,
  htmlFor,
}: {
  children: ReactNode;
  htmlFor?: string;
}) {
  return (
    <label htmlFor={htmlFor} className="text-sm font-semibold text-ink-900">
      {children}
    </label>
  );
}

export function Input({
  className,
  ...props
}: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={cn(field, className)} {...props} />;
}

export function Textarea({
  className,
  ...props
}: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className={cn(field, "min-h-28", className)} {...props} />;
}

export function Select({
  className,
  ...props
}: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select className={cn(field, className)} {...props} />;
}

export function Checkbox({
  label,
  id,
}: {
  label: string;
  id: string;
}) {
  return (
    <div className="flex items-center gap-3">
      <CheckboxPrimitive.Root
        id={id}
        className="flex size-5 items-center justify-center rounded-md border border-input bg-card data-[state=checked]:bg-brand-900"
      >
        <CheckboxPrimitive.Indicator>
          <Check className="size-3.5 text-primary-foreground" aria-hidden="true" />
        </CheckboxPrimitive.Indicator>
      </CheckboxPrimitive.Root>
      <Label htmlFor={id}>{label}</Label>
    </div>
  );
}
