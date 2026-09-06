import React from 'react';
import { Shield, X } from 'lucide-react';

export default function AuditLogModal({ isOpen, onClose, logs, loading }) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="glass-card bg-slate-900 border border-slate-700/80 rounded-3xl max-w-4xl w-full p-6 shadow-2xl space-y-4 max-h-[85vh] flex flex-col">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div>
            <h3 className="font-bold text-base text-white flex items-center gap-2">
              <Shield className="w-5 h-5 text-purple-400" />
              Append-Only System Audit Trail
            </h3>
            <p className="text-xs text-slate-400">Forensically logged events stored in PostgreSQL</p>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto custom-scrollbar space-y-2 pr-1">
          {loading ? (
            <div className="py-12 text-center text-xs text-slate-400">Loading audit records...</div>
          ) : logs.length === 0 ? (
            <div className="py-12 text-center text-xs text-slate-400">No audit events recorded yet.</div>
          ) : (
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-800 text-slate-400 text-[10px] uppercase font-mono">
                  <th className="py-2">Timestamp</th>
                  <th className="py-2">Event</th>
                  <th className="py-2">Actor</th>
                  <th className="py-2">Entity ID</th>
                  <th className="py-2">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono text-[11px]">
                {logs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-800/40">
                    <td className="py-2 text-slate-400 whitespace-nowrap">
                      {new Date(log.timestamp).toLocaleTimeString()}
                    </td>
                    <td className="py-2">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-indigo-950 text-indigo-300 border border-indigo-900/60">
                        {log.eventType}
                      </span>
                    </td>
                    <td className="py-2 text-slate-300 truncate max-w-[140px]">{log.userEmail}</td>
                    <td className="py-2 text-indigo-400">#{log.entityId}</td>
                    <td className="py-2 text-slate-400 truncate max-w-[200px]">{log.details}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
