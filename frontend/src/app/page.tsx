'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { isAuthenticated } from '@/store/auth';

export default function RootPage() {
  const router = useRouter();
  useEffect(() => {
    router.replace(isAuthenticated() ? '/chat' : '/login');
  }, [router]);

  return (
    <div style={{ display:'flex', alignItems:'center', justifyContent:'center', height:'100dvh', background:'#0a0a12' }}>
      <div className="spinner" />
    </div>
  );
}
