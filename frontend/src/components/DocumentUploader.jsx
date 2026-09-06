import React, { useRef, useState } from 'react';
import { UploadCloud, FileText, Camera, X } from 'lucide-react';

export default function DocumentUploader({ docName, file, previewUrl, onFileSelect, onRemove }) {
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef(null);
  const cameraInputRef = useRef(null);

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      onFileSelect(e.dataTransfer.files[0]);
    }
  };

  return (
    <div className="space-y-2">
      <label className="text-xs font-semibold text-slate-300 uppercase tracking-wider flex items-center justify-between">
        <span>{docName} File</span>
        <span className="text-[10px] text-slate-400 font-mono">PDF, JPEG, PNG (Max 10MB)</span>
      </label>

      {/* Hidden file input */}
      <input
        type="file"
        ref={fileInputRef}
        className="hidden"
        accept="image/jpeg,image/png,application/pdf"
        onChange={(e) => e.target.files && onFileSelect(e.target.files[0])}
      />

      {/* Hidden smartphone camera input */}
      <input
        type="file"
        ref={cameraInputRef}
        className="hidden"
        accept="image/*"
        capture="environment"
        onChange={(e) => e.target.files && onFileSelect(e.target.files[0])}
      />

      <div
        onClick={() => fileInputRef.current?.click()}
        onDragOver={(e) => {
          e.preventDefault();
          setIsDragging(true);
        }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={handleDrop}
        className={`border-2 border-dashed rounded-2xl p-4 sm:p-5 text-center cursor-pointer transition flex flex-col items-center justify-center min-h-[140px] relative overflow-hidden ${
          isDragging
            ? 'border-indigo-500 bg-indigo-950/20'
            : file
            ? 'border-indigo-500/60 bg-slate-900/80'
            : 'border-slate-800 hover:border-slate-700 bg-slate-900/40'
        }`}
      >
        {previewUrl ? (
          <div className="w-full flex flex-col items-center relative">
            <img
              src={previewUrl}
              alt="Document Preview"
              className="max-h-32 object-contain rounded-lg shadow-md border border-slate-700"
            />
            <span className="text-[11px] text-slate-300 font-medium mt-2 truncate max-w-[240px]">
              {file.name} ({(file.size / 1024).toFixed(1)} KB)
            </span>
            <span className="text-[10px] text-indigo-400">Click to replace file</span>
            {onRemove && (
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  onRemove();
                }}
                className="absolute top-0 right-0 p-1 rounded-full bg-slate-800/80 hover:bg-slate-700 text-slate-400 hover:text-white"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
        ) : file ? (
          <div className="text-center">
            <div className="w-12 h-12 rounded-xl bg-indigo-950 border border-indigo-800 flex items-center justify-center mx-auto text-indigo-400 mb-2">
              <FileText className="w-6 h-6" />
            </div>
            <div className="text-xs font-semibold text-white">{file.name}</div>
            <div className="text-[10px] text-slate-400">{(file.size / 1024).toFixed(1)} KB</div>
          </div>
        ) : (
          <div className="space-y-2">
            <div className="w-10 h-10 rounded-full bg-slate-800/80 flex items-center justify-center mx-auto text-indigo-400">
              <UploadCloud className="w-5 h-5" />
            </div>
            <div className="text-xs text-slate-300 font-medium">
              Drag & drop {docName} here, or <span className="text-indigo-400 underline">browse</span>
            </div>
            <div className="text-[10px] text-slate-500">
              Protected by magic-byte checking & anti-tamper SHA-256
            </div>
          </div>
        )}
      </div>

      {/* Quick mobile camera button */}
      <div className="flex items-center gap-2 pt-1">
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            cameraInputRef.current?.click();
          }}
          className="flex-1 py-1.5 px-3 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-[11px] text-slate-300 flex items-center justify-center gap-1.5 font-medium transition"
        >
          <Camera className="w-3.5 h-3.5 text-indigo-400" />
          Use Smartphone Camera
        </button>
      </div>
    </div>
  );
}

