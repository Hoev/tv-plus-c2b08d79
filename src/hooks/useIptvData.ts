import { useEffect, useMemo, useState } from 'react';
import { onValue, ref } from 'firebase/database';
import { db } from '@/lib/firebase';
import type { Category, SideMenu } from '@/types/admin';

export interface IptvDataState {
  categories: Record<string, Category>;
  sideMenus: Record<string, SideMenu>;
  loading: boolean;
  error: string | null;
}

export const useIptvData = (): IptvDataState => {
  const [categories, setCategories] = useState<Record<string, Category>>({});
  const [sideMenus, setSideMenus] = useState<Record<string, SideMenu>>({});
  const [error, setError] = useState<string | null>(null);
  const [ready, setReady] = useState({ categories: false, sideMenus: false });

  useEffect(() => {
    setError(null);

    const unsubCategories = onValue(
      ref(db, 'categories'),
      (snapshot) => {
        setCategories(snapshot.val() || {});
        setReady((r) => ({ ...r, categories: true }));
      },
      (err) => {
        setError(err?.message || 'Firebase error');
        setReady((r) => ({ ...r, categories: true }));
      }
    );

    const unsubSideMenus = onValue(
      ref(db, 'sideMenus'),
      (snapshot) => {
        setSideMenus(snapshot.val() || {});
        setReady((r) => ({ ...r, sideMenus: true }));
      },
      (err) => {
        setError(err?.message || 'Firebase error');
        setReady((r) => ({ ...r, sideMenus: true }));
      }
    );

    return () => {
      unsubCategories();
      unsubSideMenus();
    };
  }, []);

  const loading = useMemo(() => !(ready.categories && ready.sideMenus), [ready]);

  return { categories, sideMenus, loading, error };
};
