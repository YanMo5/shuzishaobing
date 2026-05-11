export default function RiskIndicatorChart({ summary }) {
  if (!summary || !summary.riskAssessment) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#6b7280' }}>
        暂无风险评估数据
      </div>
    );
  }

  const { riskAssessment } = summary;

  const calculateRisk = (type, value) => {
    if (type === 'sleep') {
      const percent = Math.max(0, 10 - value) * 10;
      if (percent >= 50) return { percent: Math.min(100, percent), level: 'high', label: '高风险', color: '#dc2626' };
      if (percent >= 30) return { percent: Math.min(100, percent), level: 'medium', label: '中风险', color: '#f59e0b' };
      if (value > 8 && value < 10) return { percent: percent, level: 'low', label: '偏低', color: '#10b981' };
      return { percent: Math.max(0, percent), level: 'normal', label: '正常', color: '#10b981' };
    }
    if (type === 'lateNight') {
      const percent = (value / 7) * 100;
      if (percent >= 40) return { percent, level: 'high', label: '高风险', color: '#dc2626' };
      if (percent >= 20) return { percent, level: 'medium', label: '中风险', color: '#f59e0b' };
      return { percent, level: 'normal', label: '正常', color: '#10b981' };
    }
    if (type === 'nutrition') {
      const percent = 100 - (value * 20);
      if (percent >= 50) return { percent: Math.max(0, percent), level: 'high', label: '高风险', color: '#dc2626' };
      if (percent >= 30) return { percent: Math.max(0, percent), level: 'medium', label: '中风险', color: '#f59e0b' };
      return { percent: Math.max(0, percent), level: 'normal', label: '正常', color: '#10b981' };
    }
    if (type === 'activity') {
      const percent = Math.max(0, (240 - value) / 2);
      if (percent >= 50) return { percent: Math.min(100, percent), level: 'high', label: '高风险', color: '#dc2626' };
      if (percent >= 30) return { percent: Math.min(100, percent), level: 'medium', label: '中风险', color: '#f59e0b' };
      return { percent: Math.min(100, percent), level: 'normal', label: '正常', color: '#10b981' };
    }
    if (type === 'infection') {
      const base = value * 18;
      const percent = Math.min(100, base);
      if (percent >= 50) return { percent, level: 'high', label: '高风险', color: '#dc2626' };
      if (percent >= 30) return { percent, level: 'medium', label: '中风险', color: '#f59e0b' };
      return { percent, level: 'normal', label: '正常', color: '#10b981' };
    }
    if (type === 'mental') {
      const percent = (value / 5) * 100;
      if (percent >= 60) return { percent, level: 'high', label: '高压力', color: '#dc2626' };
      if (percent >= 40) return { percent, level: 'medium', label: '中压力', color: '#f59e0b' };
      return { percent, level: 'normal', label: '正常', color: '#10b981' };
    }
    return { percent: 0, level: 'normal', label: '未知', color: '#6b7280' };
  };

  const observation = riskAssessment.observation || {};
  const sleepRisk = calculateRisk('sleep', observation.sleepHours || 0);
  const lateNightRisk = calculateRisk('lateNight', observation.lateNightCountPerWeek || 0);
  const nutritionRisk = calculateRisk('nutrition', observation.nutritionScore || 0);
  const activityRisk = calculateRisk('activity', observation.physicalActivityMinutesPerWeek || 0);
  const infectionRisk = calculateRisk('infection', observation.infectionContacts || 0);
  const mentalRisk = calculateRisk('mental', observation.stressScore || 0);

  const riskItems = [
    { label: '睡眠风险', ...sleepRisk, value: observation.sleepHours || 0, unit: '小时' },
    { label: '熬夜风险', ...lateNightRisk, value: observation.lateNightCountPerWeek || 0, unit: '次/周' },
    { label: '营养风险', ...nutritionRisk, value: observation.nutritionScore || 0, unit: '分' },
    { label: '运动风险', ...activityRisk, value: observation.physicalActivityMinutesPerWeek || 0, unit: '分钟/周' },
    { label: '感染风险', ...infectionRisk, value: observation.infectionContacts || 0, unit: '人' },
    { label: '心理风险', ...mentalRisk, value: observation.stressScore || 0, unit: '分' }
  ];

  return (
    <div style={{ padding: '20px' }}>
      <h3 style={{ fontSize: '18px', fontWeight: 600, marginBottom: '20px', color: '#1f2937' }}>
        风险指标图
      </h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px' }}>
        {riskItems.map((item, index) => (
          <div
            key={index}
            style={{
              padding: '16px',
              backgroundColor: 'white',
              borderRadius: '8px',
              boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
              border: '1px solid #e5e7eb'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
              <span style={{ fontWeight: 600, fontSize: '14px', color: '#374151' }}>{item.label}</span>
              <span
                style={{
                  padding: '4px 8px',
                  borderRadius: '12px',
                  fontSize: '12px',
                  fontWeight: 500,
                  backgroundColor: `${item.color}15`,
                  color: item.color
                }}
              >
                {item.label}
              </span>
            </div>
            <div style={{ marginBottom: '8px' }}>
              <div
                style={{
                  height: '8px',
                  backgroundColor: '#f3f4f6',
                  borderRadius: '4px',
                  overflow: 'hidden'
                }}
              >
                <div
                  style={{
                    width: `${item.percent}%`,
                    height: '100%',
                    backgroundColor: item.color,
                    transition: 'width 0.3s ease'
                  }}
                />
              </div>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', color: '#6b7280' }}>
              <span>风险指数: {Math.round(item.percent)}%</span>
              <span>当前值: {item.value} {item.unit}</span>
            </div>
          </div>
        ))}
      </div>

      {riskAssessment.riskFactors && riskAssessment.riskFactors.length > 0 && (
        <div style={{ marginTop: '20px' }}>
          <h4 style={{ fontSize: '14px', fontWeight: 600, marginBottom: '12px', color: '#374151' }}>
            风险因子
          </h4>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
            {riskAssessment.riskFactors.map((factor, index) => (
              <span
                key={index}
                style={{
                  padding: '6px 12px',
                  backgroundColor: '#fef2f2',
                  color: '#dc2626',
                  borderRadius: '16px',
                  fontSize: '13px',
                  fontWeight: 500
                }}
              >
                {factor}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
