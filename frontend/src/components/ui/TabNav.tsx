'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import styles from './TabNav.module.css';

const TABS = [
  { href: '/chat',      label: 'Chat',      icon: '💬' },
  { href: '/tasks',     label: 'Tasks',     icon: '✅' },
  { href: '/plan',      label: 'Plan',      icon: '📅' },
  { href: '/approvals', label: 'Approvals', icon: '🔔' },
  { href: '/memories',  label: 'Memory',    icon: '🧠' },
  { href: '/settings',  label: 'Settings',  icon: '⚙️' },
];

export default function TabNav() {
  const path = usePathname();

  return (
    <nav className={styles.nav} aria-label="Main navigation">
      {TABS.map((tab) => {
        const active = path.startsWith(tab.href);
        return (
          <Link
            key={tab.href}
            href={tab.href}
            className={`${styles.tab} ${active ? styles.active : ''}`}
          >
            <span className={styles.icon}>{tab.icon}</span>
            <span className={styles.label}>{tab.label}</span>
            {active && <span className={styles.dot} />}
          </Link>
        );
      })}
    </nav>
  );
}
