'use client';

import { useCallback } from 'react';
import { useTranslation as usei18nextTranslation } from 'react-i18next';

export function useTranslation(ns: string = 'common') {
  return usei18nextTranslation(ns);
}

export function useLanguage() {
  const { i18n } = usei18nextTranslation();

  const currentLanguage = i18n.language;
  const changeLanguage = useCallback(
    (lang: string) => i18n.changeLanguage(lang),
    [i18n]
  );

  return { currentLanguage, changeLanguage };
}
