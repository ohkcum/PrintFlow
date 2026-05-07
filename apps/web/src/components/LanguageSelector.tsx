'use client';

import { useCallback } from 'react';
import { supportedLanguages, changeLanguage as i18nChangeLanguage } from '@/lib/i18n';
import { useLanguage } from './i18n';

export function LanguageSelector() {
  const { currentLanguage, changeLanguage } = useLanguage();

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLSelectElement>) => {
      changeLanguage(e.target.value);
    },
    [changeLanguage]
  );

  return (
    <select
      value={currentLanguage}
      onChange={handleChange}
      style={{
        padding: '0.375rem 0.625rem',
        background: 'var(--color-input)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-md)',
        color: 'var(--color-foreground)',
        fontSize: '0.8rem',
        cursor: 'pointer',
        outline: 'none',
      }}
      aria-label="Select language"
    >
      {supportedLanguages.map((lang) => (
        <option key={lang.code} value={lang.code}>
          {lang.name}
        </option>
      ))}
    </select>
  );
}
