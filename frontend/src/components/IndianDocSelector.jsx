import React from 'react';
import { CheckCircle, Fingerprint, CreditCard, Vote, Car, Globe, GraduationCap, Truck, ShoppingBag } from 'lucide-react';

export const INDIAN_DOCUMENTS = [
  {
    id: 'AADHAAR_CARD',
    name: 'Aadhaar Card',
    dept: 'UIDAI',
    format: '12-Digit UID (XXXX XXXX XXXX)',
    icon: Fingerprint,
    color: 'from-amber-500/20 to-orange-500/20 border-orange-500/40 text-orange-400',
    badge: 'UIDAI Verified',
  },
  {
    id: 'PAN_CARD',
    name: 'Permanent Account Number (PAN)',
    dept: 'Income Tax Dept',
    format: '10-Digit Alphanumeric (ABCDE1234F)',
    icon: CreditCard,
    color: 'from-blue-500/20 to-cyan-500/20 border-blue-500/40 text-blue-400',
    badge: 'Tax Dept',
  },
  {
    id: 'VOTER_ID',
    name: 'Voter ID Card (EPIC)',
    dept: 'Election Commission of India',
    format: 'EPIC No. (WBF2938172)',
    icon: Vote,
    color: 'from-emerald-500/20 to-teal-500/20 border-emerald-500/40 text-emerald-400',
    badge: 'ECI',
  },
  {
    id: 'DRIVING_LICENSE',
    name: 'Driving Licence (DL)',
    dept: 'MoRTH / Sarathi',
    format: 'State Code + Year (WB-0420180029381)',
    icon: Car,
    color: 'from-purple-500/20 to-indigo-500/20 border-purple-500/40 text-purple-400',
    badge: 'Sarathi',
  },
  {
    id: 'INDIAN_PASSPORT',
    name: 'Indian Passport (Republic of India)',
    dept: 'Ministry of External Affairs',
    format: 'Letter + 7 Digits (P8291048)',
    icon: Globe,
    color: 'from-sky-500/20 to-blue-500/20 border-sky-500/40 text-sky-400',
    badge: 'MEA MRZ',
  },
  {
    id: 'STUDENT_ID',
    name: 'Academic / College Student ID',
    dept: 'Recognized University / Institute',
    format: 'Roll / Enrollment (BWU/BTECH/2022/041)',
    icon: GraduationCap,
    color: 'from-pink-500/20 to-rose-500/20 border-pink-500/40 text-pink-400',
    badge: 'Institute',
  },
  {
    id: 'VEHICLE_RC',
    name: 'Vehicle Registration Certificate (RC)',
    dept: 'MoRTH / Vahan',
    format: 'Reg No (WB-02-AB-1234)',
    icon: Truck,
    color: 'from-yellow-500/20 to-amber-500/20 border-yellow-500/40 text-yellow-400',
    badge: 'Vahan',
  },
  {
    id: 'RATION_CARD',
    name: 'Ration Card / NFSA Smart Card',
    dept: 'Food & Supplies Dept',
    format: 'NFSA Family ID (RC-192837461)',
    icon: ShoppingBag,
    color: 'from-green-500/20 to-emerald-500/20 border-green-500/40 text-green-400',
    badge: 'NFSA',
  },
];

export default function IndianDocSelector({ selectedDocType, onSelect }) {
  return (
    <div className="space-y-2">
      <label className="text-xs font-semibold text-slate-300 uppercase tracking-wider flex items-center justify-between">
        <span>Indian Document Format</span>
        <span className="text-[10px] text-indigo-400 font-mono">8 Formats Supported</span>
      </label>

      <div className="grid grid-cols-2 gap-2 max-h-56 overflow-y-auto pr-1 custom-scrollbar">
        {INDIAN_DOCUMENTS.map((doc) => {
          const isSelected = selectedDocType === doc.id;
          const IconComponent = doc.icon;

          return (
            <button
              key={doc.id}
              type="button"
              onClick={() => onSelect(doc.id)}
              className={`p-2.5 rounded-2xl border text-left transition relative flex flex-col justify-between ${
                isSelected
                  ? 'bg-gradient-to-br from-indigo-950/70 to-slate-900 border-indigo-500 shadow-lg shadow-indigo-500/10'
                  : 'bg-slate-900/60 hover:bg-slate-800/70 border-slate-800 text-slate-400'
              }`}
            >
              <div className="flex items-center justify-between w-full mb-1">
                <span className={`text-[10px] font-bold uppercase tracking-wider px-1.5 py-0.5 rounded border ${doc.color}`}>
                  {doc.badge}
                </span>
                {isSelected && <CheckCircle className="w-3.5 h-3.5 text-indigo-400" />}
              </div>
              <div
                className={`text-xs font-semibold leading-tight line-clamp-1 ${
                  isSelected ? 'text-white' : 'text-slate-300'
                }`}
              >
                {doc.name}
              </div>
              <div className="text-[10px] text-slate-500 truncate mt-0.5 font-mono">
                {doc.format}
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}

