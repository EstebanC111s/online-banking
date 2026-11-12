export function Logo({ size = 36 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" role="img" aria-labelledby="logo-title" focusable="false">
      <title id="logo-title">Banco Gocomezbo</title>
      <rect width="64" height="64" rx="14" fill="#0f5132" />
      <text x="32" y="45" textAnchor="middle" fontFamily="Georgia, 'Times New Roman', serif" fontSize={40} fontWeight={700} fill="#d1e7dd">G</text>
    </svg>
  );
}
