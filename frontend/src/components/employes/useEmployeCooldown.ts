import { useCallback, useEffect, useRef, useState } from "react";

export function useEmployeCooldown(employeId: number | undefined) {
  const [cooldownRenvoi, setCooldownRenvoi] = useState(0);
  const cooldownTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    if (cooldownTimerRef.current !== null) {
      clearInterval(cooldownTimerRef.current);
      cooldownTimerRef.current = null;
    }
    setCooldownRenvoi(0);
  }, [employeId]);

  useEffect(() => {
    return () => {
      if (cooldownTimerRef.current !== null) {
        clearInterval(cooldownTimerRef.current);
      }
    };
  }, []);

  const lancerCooldown = useCallback((secondes: number) => {
    if (cooldownTimerRef.current !== null) {
      clearInterval(cooldownTimerRef.current);
    }
    setCooldownRenvoi(secondes);
    cooldownTimerRef.current = setInterval(() => {
      setCooldownRenvoi((prev) => {
        if (prev <= 1) {
          if (cooldownTimerRef.current !== null) {
            clearInterval(cooldownTimerRef.current);
            cooldownTimerRef.current = null;
          }
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  }, []);

  return { cooldownRenvoi, lancerCooldown };
}
