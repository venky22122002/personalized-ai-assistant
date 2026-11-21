'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { isAuthenticated } from '@/store/auth';
import TabNav from '@/components/ui/TabNav';

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();

  useEffect(() => {
    if (!isAuthenticated()) router.replace('/login');
  }, [router]);

  return (
    <div className="page-shell">
      <div className="page-content">{children}</div>
      <TabNav />
    </div>
  );
}
