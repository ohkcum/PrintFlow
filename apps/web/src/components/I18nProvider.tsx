'use client';

import { I18nextProvider } from 'react-i18next';
import { useEffect, useState } from 'react';
import i18n from '@/lib/i18n';

function I18nInitializer({ children }: { children: React.ReactNode }) {
  const [initialized, setInitialized] = useState(false);

  useEffect(() => {
    if (!i18n.isInitialized) {
      i18n.init().then(() => setInitialized(true));
    } else {
      setInitialized(true);
    }
  }, []);

  if (!initialized) {
    return null;
  }

  return <I18nextProvider i18n={i18n}>{children}</I18nextProvider>;
}

export function I18nProvider({ children }: { children: React.ReactNode }) {
  return <I18nInitializer>{children}</I18nInitializer>;
}
