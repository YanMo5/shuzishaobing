export default function StatsCard({ title, value, subtitle, icon, trend, trendUp = true }) {
  return (
    <div style={{
      backgroundColor: 'white',
      borderRadius: '8px',
      padding: '20px',
      boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
      border: '1px solid #e5e7eb'
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <p style={{ fontSize: '14px', color: '#6b7280', marginBottom: '8px' }}>{title}</p>
          <p style={{ fontSize: '28px', fontWeight: 700, color: '#1f2937', marginBottom: '4px' }}>{value}</p>
          {subtitle && <p style={{ fontSize: '13px', color: '#9ca3af' }}>{subtitle}</p>}
        </div>
        {icon && (
          <div style={{
            width: '48px',
            height: '48px',
            borderRadius: '8px',
            backgroundColor: '#dbeafe',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '24px'
          }}>
            {icon}
          </div>
        )}
      </div>
      {trend && (
        <div style={{
          marginTop: '12px',
          paddingTop: '12px',
          borderTop: '1px solid #f3f4f6',
          display: 'flex',
          alignItems: 'center',
          gap: '4px',
          fontSize: '13px',
          color: trendUp ? '#10b981' : '#ef4444'
        }}>
          <span>{trendUp ? '↑' : '↓'}</span>
          <span>{trend}</span>
        </div>
      )}
    </div>
  );
}
