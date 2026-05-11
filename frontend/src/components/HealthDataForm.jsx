import { useState } from 'react';

const HEALTH_FOCUS_OPTIONS = ['睡眠管理', '心理压力', '传染病风险', '营养干预', '运动不足','身材管理'];
const SOURCE_OPTIONS = [
  { value: 'MANUAL', label: '手工录入' },
  { value: 'CARD', label: '校园卡' },
  { value: 'DORMITORY', label: '宿舍设备' },
  { value: 'IOT', label: '物联网设备' },
  { value: 'QUESTIONNAIRE', label: '问卷' },
  { value: 'MODEL_PREDICTION', label: '模型预测' }
];

export default function HealthDataForm({ onSubmit, onCancel, initialData = null }) {
  const [formData, setFormData] = useState(initialData || {
    sleepHours: '',
    deepSleepHours: '',
    sleepQuality: 3,
    lateNightCountPerWeek: 0,
    exerciseFrequencyPerWeek: 0,
    physicalActivityMinutesPerWeek: 0,
    waterIntakeGlasses: 8,
    snackFrequencyPerDay: 0,
    dietaryBalanceScore: 3,
    stressLevel: 3,
    moodScore: 3,
    socialInteractionScore: 3,
    infectionContacts: 0,
    feverReported: false,
    coughReported: false,
    dataSource: 'MANUAL',
    healthFocus: [],
    notes: ''
  });

  const handleChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const processedData = {
      ...formData,
      sleepHours: parseFloat(formData.sleepHours) || 0,
      deepSleepHours: parseFloat(formData.deepSleepHours) || 0,
      physicalActivityMinutesPerWeek: parseInt(formData.physicalActivityMinutesPerWeek) || 0,
      waterIntakeGlasses: parseInt(formData.waterIntakeGlasses) || 0,
      lateNightCountPerWeek: parseInt(formData.lateNightCountPerWeek) || 0,
      exerciseFrequencyPerWeek: parseInt(formData.exerciseFrequencyPerWeek) || 0,
      snackFrequencyPerDay: parseInt(formData.snackFrequencyPerDay) || 0,
      infectionContacts: parseInt(formData.infectionContacts) || 0
    };
    onSubmit(processedData);
  };

  return (
    <form onSubmit={handleSubmit} style={{ display: 'grid', gap: '16px' }}>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
        <div>
          <label style={{ display: 'block', marginBottom: '4px', fontWeight: 500 }}>睡眠时长（小时）</label>
          <input
            type="number"
            step="0.5"
            min="0"
            max="24"
            value={formData.sleepHours}
            onChange={(e) => handleChange('sleepHours', e.target.value)}
            required
            style={{ width: '100%', padding: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
          />
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: '4px', fontWeight: 500 }}>深睡眠时长（小时）</label>
          <input
            type="number"
            step="0.5"
            min="0"
            max="24"
            value={formData.deepSleepHours}
            onChange={(e) => handleChange('deepSleepHours', e.target.value)}
            style={{ width: '100%', padding: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
          />
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: '4px', fontWeight: 500 }}>睡眠质量（1-5）</label>
          <input
            type="range"
            min="1"
            max="5"
            value={formData.sleepQuality}
            onChange={(e) => handleChange('sleepQuality', parseInt(e.target.value))}
            style={{ width: '100%' }}
          />
          <span>{formData.sleepQuality}</span>
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: '4px', fontWeight: 500 }}>每周熬夜次数</label>
          <input
            type="number"
            min="0"
            max="7"
            value={formData.lateNightCountPerWeek}
            onChange={(e) => handleChange('lateNightCountPerWeek', e.target.value)}
            style={{ width: '100%', padding: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
          />
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: '4px', fontWeight: 500 }}>每周运动次数</label>
          <input
            type="number"
            min="0"
            max="7"
            value={formData.exerciseFrequencyPerWeek}
            onChange={(e) => handleChange('exerciseFrequencyPerWeek', e.target.value)}
            style={{ width: '100%', padding: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
          />
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: '4px', fontWeight: 500 }}>每周运动时长（分钟）</label>
          <input
            type="number"
            min="0"
            value={formData.physicalActivityMinutesPerWeek}
            onChange={(e) => handleChange('physicalActivityMinutesPerWeek', e.target.value)}
            style={{ width: '100%', padding: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
          />
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: '4px', fontWeight: 500 }}>每日饮水量（杯）</label>
          <input
            type="number"
            min="0"
            value={formData.waterIntakeGlasses}
            onChange={(e) => handleChange('waterIntakeGlasses', e.target.value)}
            style={{ width: '100%', padding: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
          />
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: '4px', fontWeight: 500 }}>每日零食次数</label>
          <input
            type="number"
            min="0"
            value={formData.snackFrequencyPerDay}
            onChange={(e) => handleChange('snackFrequencyPerDay', e.target.value)}
            style={{ width: '100%', padding: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
          />
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: '4px', fontWeight: 500 }}>饮食均衡评分（1-5）</label>
          <input
            type="range"
            min="1"
            max="5"
            value={formData.dietaryBalanceScore}
            onChange={(e) => handleChange('dietaryBalanceScore', parseInt(e.target.value))}
            style={{ width: '100%' }}
          />
          <span>{formData.dietaryBalanceScore}</span>
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: '4px', fontWeight: 500 }}>压力水平（1-5）</label>
          <input
            type="range"
            min="1"
            max="5"
            value={formData.stressLevel}
            onChange={(e) => handleChange('stressLevel', parseInt(e.target.value))}
            style={{ width: '100%' }}
          />
          <span>{formData.stressLevel}</span>
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: '4px', fontWeight: 500 }}>情绪评分（1-5）</label>
          <input
            type="range"
            min="1"
            max="5"
            value={formData.moodScore}
            onChange={(e) => handleChange('moodScore', parseInt(e.target.value))}
            style={{ width: '100%' }}
          />
          <span>{formData.moodScore}</span>
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: '4px', fontWeight: 500 }}>社交互动评分（1-5）</label>
          <input
            type="range"
            min="1"
            max="5"
            value={formData.socialInteractionScore}
            onChange={(e) => handleChange('socialInteractionScore', parseInt(e.target.value))}
            style={{ width: '100%' }}
          />
          <span>{formData.socialInteractionScore}</span>
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: '4px', fontWeight: 500 }}>感染接触人数</label>
          <input
            type="number"
            min="0"
            value={formData.infectionContacts}
            onChange={(e) => handleChange('infectionContacts', e.target.value)}
            style={{ width: '100%', padding: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
          />
        </div>
        <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            <input
              type="checkbox"
              checked={formData.feverReported}
              onChange={(e) => handleChange('feverReported', e.target.checked)}
            />
            发烧
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            <input
              type="checkbox"
              checked={formData.coughReported}
              onChange={(e) => handleChange('coughReported', e.target.checked)}
            />
            咳嗽
          </label>
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: '4px', fontWeight: 500 }}>数据来源</label>
          <select
            value={formData.dataSource}
            onChange={(e) => handleChange('dataSource', e.target.value)}
            style={{ width: '100%', padding: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
          >
            {SOURCE_OPTIONS.map(opt => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </div>
      </div>

      <div>
        <label style={{ display: 'block', marginBottom: '4px', fontWeight: 500 }}>健康关注（可多选）</label>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
          {HEALTH_FOCUS_OPTIONS.map(opt => (
            <button
              key={opt}
              type="button"
              onClick={() => {
                const newFocus = formData.healthFocus.includes(opt)
                  ? formData.healthFocus.filter(f => f !== opt)
                  : [...formData.healthFocus, opt];
                handleChange('healthFocus', newFocus);
              }}
              style={{
                padding: '6px 12px',
                border: formData.healthFocus.includes(opt) ? '2px solid #2563eb' : '1px solid #ddd',
                borderRadius: '16px',
                background: formData.healthFocus.includes(opt) ? '#dbeafe' : 'white',
                cursor: 'pointer',
                fontSize: '14px'
              }}
            >
              {opt}
            </button>
          ))}
        </div>
      </div>

      <div>
        <label style={{ display: 'block', marginBottom: '4px', fontWeight: 500 }}>备注</label>
        <textarea
          value={formData.notes}
          onChange={(e) => handleChange('notes', e.target.value)}
          rows="3"
          style={{ width: '100%', padding: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
        />
      </div>

      <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            style={{
              padding: '10px 20px',
              backgroundColor: '#f3f4f6',
              border: '1px solid #d1d5db',
              borderRadius: '4px',
              cursor: 'pointer'
            }}
          >
            取消
          </button>
        )}
        <button
          type="submit"
          style={{
            padding: '10px 20px',
            backgroundColor: '#2563eb',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          提交
        </button>
      </div>
    </form>
  );
}
