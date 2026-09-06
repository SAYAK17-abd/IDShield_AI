import React, { useState } from 'react';
import { Shield, ShieldAlert, Search, User, ChevronDown, CheckCircle, ExternalLink, Activity } from 'lucide-react';

export const DEMO_USERS = [
  {
    role: 'ROLE_ADMIN',
    name: 'System Administrator',
    email: 'admin@idshield.com',
    pass: 'Admin@123456!',
    title: 'Admin',
    desc: 'Full System Oversight & Audit Trail',
    avatarBg: 'bg-purple-600',
    badgeColor: 'bg-purple-500/10 text-purple-400 border-purple-500/30',
    icon: ShieldAlert,
  },
  {
    role: 'ROLE_INVESTIGATOR',
    name: 'Officer Rajesh Sen',
    email: 'investigator@idshield.com',
    pass: 'Investigator@123456!',
    title: 'Officer / Investigator',
    desc: 'Case Review & Status Decider',
    avatarBg: 'bg-blue-600',
    badgeColor: 'bg-blue-500/10 text-blue-400 border-blue-500/30',
    icon: Search,
  },
  {
    role: 'ROLE_USER',
    name: 'Citizen Sayak Dutta',
    email: 'user@idshield.com',
    pass: 'User@123456!',
    title: 'Citizen User',
    desc: 'Document Upload & Personal Screening',
    avatarBg: 'bg-emerald-600',
    badgeColor: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
    icon: User,
  },
];

export default function Navbar({ currentUser, onUserSwitch, gatewayStatus, onOpenAuditModal }) {
  const [showMenu, setShowMenu] = useState(false);

  return (
    <header className="sticky top-0 z-40 glass-nav px-4 sm:px-8 py-3.5 flex items-center justify-between border-b border-slate-800/80">
      {/* Brand & Gateway Health */}
      <div className="flex items-center space-x-3">
        <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-blue-600 flex items-center justify-center shadow-lg shadow-indigo-500/20 text-white font-bold text-xl">
          🛡️
        </div>
        <div>
          <div className="flex items-center space-x-2">
            <h1 className="font-bold text-lg tracking-tight text-white flex items-center gap-1.5">
              IDShield AI
              <span className="hidden sm:inline-block text-[10px] uppercase font-semibold bg-indigo-500/20 text-indigo-400 border border-indigo-500/30 px-2 py-0.5 rounded-full">
                SIH26188 Gateway
              </span>
            </h1>
          </div>
          <div className="flex items-center space-x-2 text-xs">
            <span
              className={`inline-block w-2 h-2 rounded-full ${
                gatewayStatus === 'connected'
                  ? 'bg-emerald-400 animate-pulse'
                  : gatewayStatus === 'connecting'
                  ? 'bg-amber-400 animate-ping'
                  : 'bg-rose-500'
              }`}
            ></span>
            <span className="text-slate-400 hidden sm:inline">Port 8080:</span>
            <span
              className={
                gatewayStatus === 'connected' ? 'text-emerald-400 font-medium' : 'text-slate-400'
              }
            >
              {gatewayStatus === 'connected'
                ? 'Gateway Online'
                : gatewayStatus === 'connecting'
                ? 'Connecting...'
                : 'Gateway Offline'}
            </span>
          </div>
        </div>
      </div>

      {/* User Switcher Dropdown */}
      <div className="relative">
        <button
          onClick={() => setShowMenu(!showMenu)}
          className="flex items-center space-x-2.5 bg-slate-900/90 hover:bg-slate-800 border border-slate-700/80 rounded-full py-1.5 pl-2 pr-3.5 text-xs transition shadow-sm active:scale-95"
        >
          <div
            className={`w-7 h-7 rounded-full flex items-center justify-center text-white font-semibold text-xs shadow ${currentUser.avatarBg}`}
          >
            {currentUser.title.charAt(0)}
          </div>
          <div className="text-left hidden md:block">
            <div className="font-semibold text-slate-200 leading-tight flex items-center gap-1">
              {currentUser.name}
              <span className={`text-[9px] px-1.5 py-0.2 rounded border font-mono ${currentUser.badgeColor}`}>
                {currentUser.title}
              </span>
            </div>
            <div className="text-[11px] text-slate-400">{currentUser.email}</div>
          </div>
          <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
        </button>

        {showMenu && (
          <div className="absolute right-0 mt-2 w-80 rounded-2xl glass-card bg-slate-900/95 border border-slate-700/80 shadow-2xl p-2 z-50 animate-in fade-in zoom-in-95">
            <div className="px-3 py-2 border-b border-slate-800 text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
              Switch Security Role
            </div>
            <div className="space-y-1 mt-1">
              {DEMO_USERS.map((user) => {
                const isSelected = currentUser.email === user.email;
                return (
                  <button
                    key={user.email}
                    onClick={() => {
                      setShowMenu(false);
                      onUserSwitch(user);
                    }}
                    className={`w-full text-left p-2.5 rounded-xl flex items-start space-x-3 transition ${
                      isSelected
                        ? 'bg-indigo-600/20 border border-indigo-500/40 text-white'
                        : 'hover:bg-slate-800/80 text-slate-300'
                    }`}
                  >
                    <div
                      className={`w-8 h-8 rounded-lg flex-shrink-0 flex items-center justify-center text-white font-bold text-xs ${user.avatarBg}`}
                    >
                      {user.title.charAt(0)}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-xs truncate">{user.title}</span>
                        {isSelected && (
                          <span className="text-[10px] text-indigo-400 font-mono">● Active</span>
                        )}
                      </div>
                      <div className="text-[11px] text-slate-400 truncate">
                        {user.name} ({user.email})
                      </div>
                      <div className="text-[10px] text-slate-500 mt-0.5">{user.desc}</div>
                    </div>
                  </button>
                );
              })}
            </div>

            {currentUser.role === 'ROLE_ADMIN' && (
              <div className="mt-2 pt-2 border-t border-slate-800">
                <button
                  onClick={() => {
                    setShowMenu(false);
                    onOpenAuditModal();
                  }}
                  className="w-full text-left px-3 py-2 rounded-lg text-xs text-purple-300 hover:bg-purple-950/40 flex items-center justify-between font-medium"
                >
                  <span className="flex items-center gap-1.5">
                    <Activity className="w-3.5 h-3.5 text-purple-400" />
                    View Forensic Audit Logs
                  </span>
                  <ExternalLink className="w-3 h-3 text-purple-400" />
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </header>
  );
}

