import React, { useRef } from 'react';
import { UserCheck, X } from 'lucide-react';

export default function SelfieCapture({ file, previewUrl, onSelect, onRemove, docName }) {
  const inputRef = useRef(null);

  return (
    <div className="space-y-2 pt-1 border-t border-slate-800/80">
      <label className="text-xs font-semibold text-slate-300 uppercase tracking-wider flex items-center justify-between">
        <span>Face Selfie Match (Optional)</span>
        <span className="text-[10px] text-emerald-400 font-mono">Biometric 1:1</span>
      </label>

      <input
        type="file"
        ref={inputRef}
        className="hidden"
        accept="image/*"
        capture="user"
        onChange={(e) => e.target.files && onSelect(e.target.files[0])}
      />

      <div
        onClick={() => inputRef.current?.click()}
        className="border border-slate-800 hover:border-slate-700 bg-slate-900/30 rounded-2xl p-3 text-center cursor-pointer flex items-center justify-between gap-3 transition"
      >
        <div className="flex items-center space-x-3">
          {previewUrl ? (
            <img
              src={previewUrl}
              alt="Selfie"
              className="w-10 h-10 rounded-full object-cover border border-indigo-500"
            />
          ) : (
            <div className="w-10 h-10 rounded-full bg-slate-800 flex items-center justify-center text-slate-400">
              <UserCheck className="w-5 h-5" />
            </div>
          )}
          <div className="text-left">
            <div className="text-xs font-medium text-slate-300">
              {file ? file.name : 'Take or upload selfie'}
            </div>
            <div className="text-[10px] text-slate-500">
              Matches face on {docName} with live capture
            </div>
          </div>
        </div>

        <div className="flex items-center space-x-1.5">
          <span className="text-[11px] text-indigo-400 font-medium px-2 py-1 rounded bg-indigo-950/40 border border-indigo-900/60">
            {file ? 'Change' : 'Capture'}
          </span>
          {file && onRemove && (
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                onRemove();
              }}
              className="p-1 rounded-full text-slate-400 hover:text-white"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
