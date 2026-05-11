/**
 * 校园健康平台 - 前端主应用组件
 * 
 * 本文件包含完整的校园健康平台前端应用，支持三种角色：
 * 1. 学生端：查看个人健康数据、提交健康观测、获取AI建议
 * 2. 教师端：管理学生、查看学生健康状态、创建学生账号
 * 3. 管理员端：完整的系统管理功能
 * 
 * 技术栈：React + Vite + CSS3
 */
import { useEffect, useMemo, useState } from 'react';

// 示例学生数据（仅供展示）
const SAMPLE_STUDENTS = [
  { id: 'S1001', name: '李明', college: '计算机学院', major: '软件工程', className: '软工2班', grade: 2, dormitory: 'A3-512' },
  { id: 'S1002', name: '王婷', college: '生命科学学院', major: '食品科学', className: '食品1班', grade: 3, dormitory: 'B2-208' },
  { id: 'S1003', name: '张强', college: '管理学院', major: '工商管理', className: '工管1班', grade: 1, dormitory: 'C1-319' }
];

// 预设的管理员和教师令牌（用于测试）
const ADMIN_TOKEN = 'token-admin-platform';
const STAFF_TOKEN = 'token-staff-platform';

// 健康关注选项（用于AI推理聚焦）
const HEALTH_FOCUS_OPTIONS = ['睡眠管理', '心理压力', '传染病风险', '营养干预', '运动不足','身材管理'];

// 健康数据来源类型选项
const SOURCE_OPTIONS = [
  { value: 'MANUAL', label: '手工录入' },
  { value: 'CARD', label: '校园卡' },
  { value: 'DORMITORY', label: '宿舍设备' },
  { value: 'IOT', label: '物联网设备' },
  { value: 'QUESTIONNAIRE', label: '问卷' },
  { value: 'MODEL_PREDICTION', label: '模型预测' }
];

/**
 * 从 localStorage 读取数据
 * @param {string} key - 存储键名
 * @param {any} fallback - 默认值
 * @returns {any} 存储的值或默认值
 */
function readStorage(key, fallback) {
  if (typeof window === 'undefined') return fallback;
  return window.localStorage.getItem(key) || fallback;
}

/**
 * 写入数据到 localStorage
 * @param {string} key - 存储键名
 * @param {any} value - 要存储的值
 */
function writeStorage(key, value) {
  window.localStorage.setItem(key, value);
}

/**
 * 格式化日期时间为中文格式
 * @param {string|Date} value - 日期时间值
 * @returns {string} 格式化后的字符串
 */
function formatDateTime(value) {
  if (!value) return '-';
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}

/**
 * 格式化数字
 * @param {number} value - 数字值
 * @param {number} fractionDigits - 小数位数（默认0）
 * @returns {string} 格式化后的字符串
 */
function formatNumber(value, fractionDigits = 0) {
  return Number(value).toFixed(fractionDigits);
}

/**
 * 从令牌中解析用户角色
 * @param {string} token - API令牌
 * @returns {'admin' | 'staff' | 'student'} 用户角色
 */
function roleFromToken(token) {
  if ((token || '').includes('admin')) return 'admin';
  if ((token || '').includes('staff')) return 'staff';
  return 'student';
}

/**
 * API请求封装函数
 * @param {string} token - API令牌
 * @param {string} path - 请求路径
 * @param {object} options - 请求选项
 * @returns {any} 响应数据
 * @throws {Error} 请求失败时抛出错误
 */
async function apiFetch(token, path, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };

  if (token) {
    headers['X-Api-Token'] = token;
  }

  const response = await fetch(path, {
    ...options,
    headers
  });

  if (!response.ok) {
    const data = await response.json().catch(() => ({ message: `Request failed: ${response.status}` }));
    throw new Error(data.message || `Request failed: ${response.status}`);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

/**
 * 分页组件
 * 
 * @param {number} totalItems - 总条目数
 * @param {number} pageSize - 每页显示数量
 * @param {number} currentPage - 当前页码
 * @param {function} onPageChange - 页码变更回调
 * @returns {JSX.Element} 分页组件
 */
function Pagination({ totalItems, pageSize, currentPage, onPageChange }) {
  const totalPages = Math.ceil(totalItems / pageSize);
  const startIndex = (currentPage - 1) * pageSize + 1;
  const endIndex = Math.min(startIndex + pageSize - 1, totalItems);

  if (totalPages <= 1) return null;

  /**
   * 计算要显示的页码范围（最多显示5个页码）
   * @returns {number[]} 页码数组
   */
  const getPageNumbers = () => {
    const pages = [];
    const maxVisible = 5;
    let start = Math.max(1, currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(totalPages, start + maxVisible - 1);
    
    if (end - start + 1 < maxVisible) {
      start = Math.max(1, end - maxVisible + 1);
    }

    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    return pages;
  };

  return (
    <div className="pagination">
      <span className="pagination-info">{startIndex}-{endIndex} / {totalItems}</span>
      <button onClick={() => onPageChange(currentPage - 1)} disabled={currentPage === 1}>
        ←
      </button>
      {getPageNumbers().map((page) => (
        <button
          key={page}
          onClick={() => onPageChange(page)}
          className={currentPage === page ? 'active' : ''}
        >
          {page}
        </button>
      ))}
      <button onClick={() => onPageChange(currentPage + 1)} disabled={currentPage === totalPages}>
        →
      </button>
    </div>
  );
}

/**
 * KPI指标卡片组件
 * 
 * @param {string} label - 指标名称
 * @param {string|number} value - 指标值
 * @param {string} hint - 提示文字
 * @returns {JSX.Element} KPI卡片组件
 */
function KpiCard({ label, value, hint }) {
  return (
    <article className="kpi-card">
      <span>{label}</span>
      <strong>{value}</strong>
      <span>{hint}</span>
    </article>
  );
}

/**
 * 图表卡片容器组件
 * 
 * @param {string} eyebrow - 小标题（显示在标题上方）
 * @param {string} title - 主标题
 * @param {string} subtitle - 副标题/描述
 * @param {JSX.Element} action - 操作按钮或标签
 * @param {JSX.Element} children - 子内容
 * @returns {JSX.Element} 图表卡片组件
 */
function ChartCard({ eyebrow, title, subtitle, action, children }) {
  return (
    <article className="card panel chart-card">
      <div className="panel-head chart-header">
        <div>
          <span className="eyebrow">{eyebrow}</span>
          <h2>{title}</h2>
          {subtitle ? <p className="chart-subtitle">{subtitle}</p> : null}
        </div>
        {action}
      </div>
      {children}
    </article>
  );
}

/**
 * 标签按钮组件
 * 
 * @param {boolean} active - 是否激活状态
 * @param {JSX.Element} children - 标签内容
 * @param {function} onClick - 点击回调
 * @returns {JSX.Element} 标签按钮组件
 */
function Chip({ active = false, children, onClick }) {
  return (
    <button type="button" className={`chip ${active ? 'active' : ''}`} onClick={onClick}>
      {children}
    </button>
  );
}

/**
 * 折线图组件
 * 
 * @param {Array} series - 数据系列数组
 * @param {Array} labels - X轴标签数组
 * @returns {JSX.Element} 折线图组件
 */
function LineChart({ series, labels }) {
  const width = 760;
  const height = 260;
  const padding = 28;
  
  // 计算每个数据点的坐标
  const pointsBySeries = series.map((item) => {
    const max = item.max || 100;
    const values = item.values.length ? item.values : [0];
    return values.map((value, index) => {
      const x = padding + (index * (width - padding * 2)) / Math.max(values.length - 1, 1);
      const y = height - padding - (Math.max(0, Math.min(max, value)) / max) * (height - padding * 2);
      return `${x},${y}`;
    }).join(' ');
  });

  return (
    <div className="bi-chart">
      <svg viewBox={`0 0 ${width} ${height}`} className="bi-chart-svg" role="img" aria-label="健康指标趋势图">
        {/* 绘制网格线 */}
        {[0, 1, 2, 3, 4].map((tick) => {
          const y = padding + ((height - padding * 2) / 4) * tick;
          return <line key={tick} x1={padding} y1={y} x2={width - padding} y2={y} className="bi-grid-line" />;
        })}
        {/* 绘制数据系列 */}
        {series.map((item, index) => (
          <g key={item.label}>
            <polyline points={pointsBySeries[index]} className={`bi-series bi-series-${index + 1}`} />
            {/* 绘制数据点 */}
            {item.values.map((value, pointIndex) => {
              const max = item.max || 100;
              const x = padding + (pointIndex * (width - padding * 2)) / Math.max(item.values.length - 1, 1);
              const y = height - padding - (Math.max(0, Math.min(max, value)) / max) * (height - padding * 2);
              return <circle key={`${item.label}-${pointIndex}`} cx={x} cy={y} r="4" className={`bi-dot bi-dot-${index + 1}`} />;
            })}
          </g>
        ))}
      </svg>
      <div className="bi-axis-labels">
        {labels.map((label) => <span key={label}>{label}</span>)}
      </div>
      <div className="bi-legend">
        {series.map((item, index) => (
          <span key={item.label} className="bi-legend-item">
            <i className={`bi-legend-swatch bi-swatch-${index + 1}`} />
            {item.label}
          </span>
        ))}
      </div>
    </div>
  );
}

/**
 * 水平条形图组件
 * 
 * @param {Array} items - 条形图数据项数组
 * @returns {JSX.Element} 水平条形图组件
 */
function HorizontalBarChart({ items }) {
  return (
    <div className="bi-bars">
      {items.map((item) => (
        <div className="bi-bar-row" key={item.label}>
          <div className="bi-bar-row-head">
            <span>{item.label}</span>
            <strong>{item.valueLabel}</strong>
          </div>
          <div className="bi-bar-track">
            <div className="bi-bar-fill" style={{ 
              width: `${Math.max(0, Math.min(100, item.percent))}%`, 
              background: item.color || 'linear-gradient(90deg, var(--accent), var(--accent-2))' 
            }} />
          </div>
        </div>
      ))}
    </div>
  );
}

/**
 * 健康建议分类组件
 * 
 * 按不同健康方向分类显示建议，支持标签页切换
 * 
 * @param {Array} immediateActions - 立即行动建议数组
 * @returns {JSX.Element} 健康建议组件
 */
function HealthAdvice({ immediateActions = [] }) {
  const categorizedAdvice = useMemo(() => {
    const categories = {
      sleep: [],
      nutrition: [],
      exercise: [],
      mental: [],
      prevention: [],
      lifestyle: [],
    };

    if (!immediateActions || immediateActions.length === 0) return categories;

    let currentCategory = null;

    for (let i = 0; i < immediateActions.length; i++) {
      const line = immediateActions[i]?.trim();
      if (!line || line.startsWith('---')) continue;

      if (line.includes('【睡眠改善方案')) {
        currentCategory = 'sleep';
        categories[currentCategory].push({ type: 'title', text: line.replace(/【|】/g, '') });
      } else if (line.includes('【营养改善方案')) {
        currentCategory = 'nutrition';
        categories[currentCategory].push({ type: 'title', text: line.replace(/【|】/g, '') });
      } else if (line.includes('【运动健身方案')) {
        currentCategory = 'exercise';
        categories[currentCategory].push({ type: 'title', text: line.replace(/【|】/g, '') });
      } else if (line.includes('【心理健康方案')) {
        currentCategory = 'mental';
        categories[currentCategory].push({ type: 'title', text: line.replace(/【|】/g, '') });
      } else if (line.includes('【疾病防护方案')) {
        currentCategory = 'prevention';
        categories[currentCategory].push({ type: 'title', text: line.replace(/【|】/g, '') });
      } else if (line.includes('【生活习惯方案')) {
        currentCategory = 'lifestyle';
        categories[currentCategory].push({ type: 'title', text: line.replace(/【|】/g, '') });
      } else if (currentCategory) {
        if (line.match(/^第[1-9]周/)) {
          categories[currentCategory].push({ type: 'section', text: line });
        } else if (line.match(/^[-•*]/)) {
          categories[currentCategory].push({ type: 'item', text: line.replace(/^[-•*]\s*/, '') });
        } else if (line.includes('：')) {
          const [label, value] = line.split('：', 2);
          if (value) {
            categories[currentCategory].push({ type: 'info', label, value });
          } else {
            categories[currentCategory].push({ type: 'text', text: line });
          }
        } else {
          categories[currentCategory].push({ type: 'text', text: line });
        }
      }
    }

    return categories;
  }, [immediateActions]);

  const adviceTabs = [
    { id: 'sleep', name: '🌙 睡眠', keywords: ['睡眠', '熬夜', '作息'], priority: 1 },
    { id: 'nutrition', name: '🍎 营养', keywords: ['营养', '饮食', '食谱'], priority: 2 },
    { id: 'exercise', name: '🏃 运动', keywords: ['运动', '活动', '健身'], priority: 3 },
    { id: 'mental', name: '🧘 心理', keywords: ['心理', '压力', '情绪'], priority: 4 },
    { id: 'prevention', name: '🛡️ 防护', keywords: ['疾病', '防护', '感染', '发热', '咳嗽'], priority: 5 },
    { id: 'lifestyle', name: '📋 习惯', keywords: ['生活', '习惯', '作息', '饮水'], priority: 6 },
  ];

  const hasAdvice = (category) => categorizedAdvice[category] && categorizedAdvice[category].length > 0;
  const activeTabs = adviceTabs.filter(tab => hasAdvice(tab.id));
  const defaultTab = activeTabs.length > 0 ? activeTabs[0].id : 'sleep';
  const [activeTab, setActiveTab] = useState(defaultTab);

  const renderAdviceItem = (item, index) => {
    switch (item.type) {
      case 'title':
        return <h3 key={index} className="advice-title">{item.text}</h3>;
      case 'section':
        return <h4 key={index} className="advice-section">{item.text}</h4>;
      case 'info':
        return (
          <div key={index} className="advice-info">
            <span className="info-label">{item.label}</span>
            <span className="info-value">{item.value}</span>
          </div>
        );
      case 'item':
        return <li key={index} className="advice-item">{item.text}</li>;
      default:
        return <p key={index} className="advice-text">{item.text}</p>;
    }
  };

  return (
    <div className="health-advice-container">
      {activeTabs.length > 0 && (
        <div className="advice-tabs">
          {activeTabs.map((tab) => (
            <button
              key={tab.id}
              type="button"
              className={`advice-tab ${activeTab === tab.id ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.id)}
            >
              {tab.name}
            </button>
          ))}
        </div>
      )}
      
      <div className="advice-content">
        {categorizedAdvice[activeTab] && categorizedAdvice[activeTab].length > 0 ? (
          <div className="advice-list">
            {categorizedAdvice[activeTab].map((item, index) => renderAdviceItem(item, index))}
          </div>
        ) : (
          <div className="advice-empty">暂无健康建议</div>
        )}
      </div>
    </div>
  );
}

/**
 * 应用外壳组件
 * 
 * 提供应用的整体布局框架，包含背景装饰元素
 * 
 * @param {JSX.Element} children - 子组件
 * @returns {JSX.Element} 应用外壳组件
 */
function AppShell({ children }) {
  return (
    <main className="shell">
      <div className="bg-orbit bg-orbit-a" />
      <div className="bg-orbit bg-orbit-b" />
      {children}
    </main>
  );
}

export default function App() {
  const storedToken = useMemo(() => readStorage('campus-token', ''), []);
  const storedStudentId = useMemo(() => readStorage('campus-student-id', 'S1001'), []);
  const storedStudentName = useMemo(() => {
    const matchedStudent = SAMPLE_STUDENTS.find((item) => item.id === storedStudentId);
    return readStorage('campus-student-name', matchedStudent?.name || SAMPLE_STUDENTS[0].name);
  }, []);
  const [page, setPage] = useState('login');
  const [loginMode, setLoginMode] = useState('student');
  const [studentFormMode, setStudentFormMode] = useState('login');
  const [staffFormMode, setStaffFormMode] = useState('login');
  const [token, setToken] = useState(storedToken);
  const [studentId, setStudentId] = useState(storedStudentId);
  const [studentName, setStudentName] = useState(storedStudentName);
  const [currentRole, setCurrentRole] = useState(roleFromToken(storedToken));
  const [summary, setSummary] = useState(null);
  const [signals, setSignals] = useState([]);
  const [inference, setInference] = useState(null);
  const [auditEvents, setAuditEvents] = useState([]);
  const [studentSnapshots, setStudentSnapshots] = useState({});
  const [lastLoadedAt, setLastLoadedAt] = useState(null);
  const [apiStatus, setApiStatus] = useState({ online: false, message: 'API 未连接' });
  const [toast, setToast] = useState('');
  const [prompt, setPrompt] = useState('生成一份面向辅导员的校园健康干预建议');
  const [focus, setFocus] = useState('睡眠管理');
  const [studentQuery, setStudentQuery] = useState('');
  const [collegeFilter, setCollegeFilter] = useState('ALL');
  const [staffStudentQuery, setStaffStudentQuery] = useState('');
  const [staffCollegeFilter, setStaffCollegeFilter] = useState('ALL');
  const [auditFilter, setAuditFilter] = useState('ALL');
  const [studentList, setStudentList] = useState([]);
  const [staffList, setStaffList] = useState([]);
  const [adminTab, setAdminTab] = useState('students');
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(4);
  const [staffCurrentPage, setStaffCurrentPage] = useState(1);
  const [staffPageSize, setStaffPageSize] = useState(4);
  const [auditCurrentPage, setAuditCurrentPage] = useState(1);
  const [auditPageSize, setAuditPageSize] = useState(8);
  const [adminUsername, setAdminUsername] = useState('');
  const [adminPassword, setAdminPassword] = useState('');
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  useEffect(() => {
    writeStorage('campus-token', token);
  }, [token]);

  useEffect(() => {
    writeStorage('campus-student-id', studentId);
  }, [studentId]);

  useEffect(() => {
    writeStorage('campus-student-name', studentName);
  }, [studentName]);

  useEffect(() => {
    if (!toast) return undefined;
    const timer = window.setTimeout(() => setToast(''), 2600);
    return () => window.clearTimeout(timer);
  }, [toast]);

  async function pingApi(currentToken = token) {
    try {
      const response = await fetch('/api/v1/health/ping', {
        headers: currentToken ? { 'X-Api-Token': currentToken } : {}
      });
      if (!response.ok) {
        throw new Error(`Request failed: ${response.status}`);
      }
      setApiStatus({ online: true, message: 'API 在线' });
    } catch {
      setApiStatus({ online: false, message: 'API 离线：请确认后端已启动' });
    }
  }

  async function loadSummary(currentToken = token, currentStudentId = studentId) {
    const nextSummary = await apiFetch(currentToken, `/api/v1/students/${currentStudentId}/summary`);
    setSummary(nextSummary);
    setLastLoadedAt(new Date());
    setStudentSnapshots((current) => ({
      ...current,
      [currentStudentId]: {
        riskLevel: nextSummary.assessment.riskLevel,
        riskScore: nextSummary.assessment.riskScore,
        assessedAt: nextSummary.assessment.assessedAt
      }
    }));
    return nextSummary;
  }

  async function loadSignals(currentToken = token, currentStudentId = studentId) {
    const nextSignals = await apiFetch(currentToken, `/api/v1/students/${currentStudentId}/signals`);
    setSignals(nextSignals);
    return nextSignals;
  }

  async function loadAudit(currentToken = token) {
    if (!roleFromToken(currentToken).includes('admin')) {
      setToast('审计日志仅管理员令牌可访问。');
      return;
    }

    const nextAudit = await apiFetch(currentToken, '/api/v1/audit/events');
    setAuditEvents(nextAudit);
    setToast(`审计日志已加载：${nextAudit.length} 条`);
  }

  async function loadStudentList(currentToken = token) {
    try {
      const students = await apiFetch(currentToken, '/api/v1/students');
      setStudentList(students);
      return students;
    } catch (error) {
      setStudentList([]);
      return [];
    }
  }

  async function loadStaffList(currentToken = token) {
    try {
      const staff = await apiFetch(currentToken, '/api/v1/staff');
      setStaffList(staff);
    } catch (error) {
      setStaffList([]);
    }
  }

  async function loadAll(currentToken = token, currentStudentId = studentId) {
    await pingApi(currentToken);
    await Promise.all([
      loadSummary(currentToken, currentStudentId),
      loadSignals(currentToken, currentStudentId)
    ]);
  }

  async function runInference(currentToken = token, currentStudentId = studentId) {
    const nextInference = await apiFetch(currentToken, '/api/v1/models/inference', {
      method: 'POST',
      body: JSON.stringify({
        studentId: currentStudentId,
        prompt: prompt.trim(),
        focus: focus.trim()
      })
    });

    setInference(nextInference);
    setToast('模型推理已完成');
  }

  async function reassess(currentToken = token, currentSummary = summary) {
    if (!currentSummary) return;

    const assessment = await apiFetch(currentToken, '/api/v1/assessments', {
      method: 'POST',
      body: JSON.stringify({
        student: currentSummary.student,
        observation: currentSummary.observation,
        focus: focus
      })
    });

    setSummary({ ...currentSummary, assessment });
    setStudentSnapshots((current) => ({
      ...current,
      [currentSummary.student.studentId]: {
        riskLevel: assessment.riskLevel,
        riskScore: assessment.riskScore,
        assessedAt: assessment.assessedAt
      }
    }));
    setToast('风险重新评估完成');
  }

  async function submitSignal(event) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    await apiFetch(token, `/api/v1/students/${studentId}/signals`, {
      method: 'POST',
      body: JSON.stringify({
        sourceType: formData.get('sourceType'),
        sleepHours: Number(formData.get('sleepHours')),
        lateNightCountPerWeek: Number(formData.get('lateNightCountPerWeek')),
        nutritionScore: Number(formData.get('nutritionScore')),
        stressScore: Number(formData.get('stressScore')),
        physicalActivityMinutesPerWeek: Number(formData.get('physicalActivityMinutesPerWeek')),
        infectionContacts: Number(formData.get('infectionContacts')),
        feverReported: formData.get('feverReported') === 'on',
        coughReported: formData.get('coughReported') === 'on',
        note: String(formData.get('note') || '').trim()
      })
    });

    setToast('健康信号已提交');
    if (event.currentTarget) event.currentTarget.reset();
    await Promise.all([loadSummary(), loadSignals()]);
  }

  useEffect(() => {
    if (page !== 'login') return;
    pingApi(token).catch(() => undefined);
  }, [page]);

  const currentStudent = summary?.student;
  const currentObservation = summary?.observation;
  const currentAssessment = summary?.assessment;
  const selectedSnapshot = studentSnapshots[studentId];
  const confidencePercent = Math.round((inference?.confidence || 0) * 100);
  const signalSeries = useMemo(() => signals.slice(0, 7).reverse(), [signals]);
  const lineLabels = useMemo(() => signalSeries.map((signal) => formatDateTime(signal.observedAt).slice(5, 10)), [signalSeries]);
  const auditKinds = useMemo(() => ['ALL', ...new Set(auditEvents.map((event) => event.actionType))], [auditEvents]);
  const auditRows = useMemo(() => auditEvents.filter((event) => auditFilter === 'ALL' || event.actionType === auditFilter), [auditEvents, auditFilter]);

  const overviewCards = useMemo(() => [
    { label: '睡眠时长', value: currentObservation ? `${formatNumber(currentObservation.sleepHours, 1)}h` : '-', hint: '过去 24 小时' },
    { label: '压力分数', value: currentObservation ? currentObservation.stressScore : '-', hint: '越高越危险' },
    { label: '活动分钟', value: currentObservation ? currentObservation.physicalActivityMinutesPerWeek : '-', hint: '每周累计' },
    { label: '风险因子', value: currentAssessment ? currentAssessment.riskFactors.length : '-', hint: '自动识别' }
  ], [currentObservation, currentAssessment]);

  const riskBars = useMemo(() => currentObservation ? [
    { label: '睡眠时长', percent: Math.min(100, (currentObservation.sleepHours / 10) * 100), valueLabel: `${currentObservation.sleepHours.toFixed(1)}h / 10h`, color: 'linear-gradient(90deg, #7da8ff, #39d0c4)' },
    { label: '压力指数', percent: Math.min(100, currentObservation.stressScore), valueLabel: `${currentObservation.stressScore} / 100`, color: 'linear-gradient(90deg, #f7b267, #ff8f6b)' },
    { label: '营养摄入', percent: Math.min(100, (currentObservation.nutritionScore || currentObservation.dietaryBalanceScore || 0)), valueLabel: `${currentObservation.nutritionScore || currentObservation.dietaryBalanceScore || 0} / 100`, color: 'linear-gradient(90deg, #ffc857, #f7b267)' },
    { label: '运动时长', percent: Math.min(100, (currentObservation.physicalActivityMinutesPerWeek / 240) * 100), valueLabel: `${currentObservation.physicalActivityMinutesPerWeek} / 240分钟`, color: 'linear-gradient(90deg, #61d095, #39d0c4)' },
    { label: '感染风险', percent: Math.min(100, currentObservation.infectionContacts * 18 + (currentObservation.feverReported ? 18 : 0) + (currentObservation.coughReported ? 18 : 0)), valueLabel: `${currentObservation.infectionContacts}人接触`, color: 'linear-gradient(90deg, #ff6b6b, #ff8f6b)' }
  ] : [], [currentObservation]);

  const sleepSeries = useMemo(() => 
    signalSeries.map((signal) => Math.max(0, (10 - (signal.sleepHours || 0)) * 10)), 
    [signalSeries]
  );
  const stressSeries = useMemo(() => 
    signalSeries.map((signal) => signal.stressScore || 0), 
    [signalSeries]
  );

  async function handleStudentLogin(event) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const nextStudentId = String(formData.get('studentId') || '').trim().toUpperCase();
    const password = String(formData.get('password') || '').trim();

    if (!nextStudentId || !password) {
      setToast('请填写学号和密码');
      return;
    }

    try {
      const loginResponse = await apiFetch(null, '/api/v1/students/login', {
        method: 'POST',
        body: JSON.stringify({ studentId: nextStudentId, password })
      });

      setToken(loginResponse.token);
      setStudentId(nextStudentId);
      setStudentName(loginResponse.name);
      setCurrentRole('student');
      await loadAll(loginResponse.token, nextStudentId);
      setPage('student');
      setToast('学生登录成功');
    } catch (error) {
      setToast(error.message || '学生登录失败');
    }
  }

  async function handleStudentAuth(event) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const nextStudentId = String(formData.get('studentId') || '').trim().toUpperCase();
    const nextStudentName = String(formData.get('name') || '').trim();
    const password = String(formData.get('password') || '').trim();

    if (!nextStudentId || !nextStudentName || !password) {
      setToast('请填写完整信息');
      return;
    }

    if (password.length < 6) {
      setToast('密码至少6位');
      return;
    }

    try {
      const registrationResponse = await apiFetch(null, '/api/v1/students/register', {
        method: 'POST',
        body: JSON.stringify({
          studentId: nextStudentId,
          name: nextStudentName,
          password,
          college: String(formData.get('college') || '').trim(),
          major: String(formData.get('major') || '').trim(),
          className: String(formData.get('className') || '').trim(),
          grade: Number(formData.get('grade')) || 1,
          dormitory: String(formData.get('dormitory') || '').trim()
        })
      });

      setToken(registrationResponse.token);
      setStudentId(nextStudentId);
      setStudentName(nextStudentName);
      setCurrentRole('student');
      await loadAll(registrationResponse.token, nextStudentId);
      setPage('student');
      setToast('学生账号注册成功');
    } catch (error) {
      setToast(error.message || '学生注册失败');
    }
  }

  async function handleStaffLogin(event) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const staffId = String(formData.get('staffId') || '').trim();
    const password = String(formData.get('password') || '').trim();

    if (!staffId || !password) {
      setToast('请填写工号和密码');
      return;
    }

    try {
      const loginResponse = await apiFetch(null, '/api/v1/staff/login', {
        method: 'POST',
        body: JSON.stringify({ staffId, password })
      });

      setToken(loginResponse.token);
      setCurrentRole('staff');
      const students = await loadStudentList(loginResponse.token);
      const nextStudentId = students[0]?.studentId || students[0]?.id || '';
      if (nextStudentId) {
        setStudentId(nextStudentId);
        await loadAll(loginResponse.token, nextStudentId);
      } else {
        setStudentId('');
        setSummary(null);
        setSignals([]);
      }
      setPage('staff');
      setToast('教师登录成功');
    } catch (error) {
      setToast(error.message || '教师登录失败');
    }
  }

  async function handleStaffRegister(event) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const staffId = String(formData.get('staffId') || '').trim();
    const name = String(formData.get('name') || '').trim();
    const password = String(formData.get('password') || '').trim();

    if (!staffId || !name || !password) {
      setToast('请填写完整信息');
      return;
    }

    if (password.length < 6) {
      setToast('密码至少6位');
      return;
    }

    setToast('请联系管理员注册教师账号');
  }

  async function handleAdminLogin(event) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const username = String(formData.get('username') || '').trim();
    const password = String(formData.get('password') || '').trim();

    if (!username || !password) {
      setToast('请填写账号和密码');
      return;
    }

    try {
      const loginResponse = await apiFetch(null, '/api/v1/admin/login', {
        method: 'POST',
        body: JSON.stringify({ username, password })
      });

      setToken(loginResponse.token);
      setCurrentRole('admin');
      const students = await loadStudentList(loginResponse.token);
      const nextStudentId = students[0]?.studentId || students[0]?.id || '';
      if (nextStudentId) {
        setStudentId(nextStudentId);
        await loadAll(loginResponse.token, nextStudentId);
      } else {
        setStudentId('');
        setSummary(null);
        setSignals([]);
      }
      await loadAudit(loginResponse.token);
      await loadStaffList(loginResponse.token);
      setPage('admin');
      setToast('管理员登录成功');
    } catch (error) {
      setToast(error.message || '管理员登录失败');
    }
  }

  async function handleCreateStudent(event) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    
    const name = String(formData.get('name') || '').trim();
    const studentId = String(formData.get('studentId') || '').trim().toUpperCase();
    const password = String(formData.get('password') || '').trim();
    const college = String(formData.get('college') || '').trim();
    const major = String(formData.get('major') || '').trim();
    const className = String(formData.get('className') || '').trim();
    
    if (!name) {
      setToast('请输入姓名');
      return;
    }
    if (!studentId) {
      setToast('请输入学号');
      return;
    }
    if (!password) {
      setToast('请输入密码');
      return;
    }
    if (password.length < 6) {
      setToast('密码至少6位');
      return;
    }
    if (!college) {
      setToast('请输入系部');
      return;
    }
    if (!major) {
      setToast('请输入专业');
      return;
    }
    if (!className) {
      setToast('请输入班级');
      return;
    }

    try {
      await apiFetch(token, '/api/v1/admin/students', {
        method: 'POST',
        body: JSON.stringify({
          name,
          studentId,
          password,
          college,
          major,
          className,
          grade: Number(formData.get('grade')) || 1,
          dormitory: String(formData.get('dormitory') || '').trim()
        })
      });

      setToast('学生账号创建成功');
      if (event.currentTarget) event.currentTarget.reset();
      await loadStudentList(token);
    } catch (error) {
      setToast(error.message || '创建学生失败');
    }
  }

  async function handleDeleteStudent(targetStudentId) {
    if (!confirm(`确定要删除学生 ${targetStudentId} 吗？此操作无法撤销。`)) {
      return;
    }

    try {
      await apiFetch(token, `/api/v1/admin/students/${targetStudentId}`, {
        method: 'DELETE'
      });

      setToast('学生删除成功');
      await loadStudentList(token);
      if (targetStudentId === studentId) {
        setStudentId(null);
        setSummary(null);
        setSignals([]);
      }
    } catch (error) {
      setToast(error.message || '删除学生失败');
    }
  }

  async function handleStaffCreateStudent(event) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    
    const name = String(formData.get('name') || '').trim();
    const studentId = String(formData.get('studentId') || '').trim().toUpperCase();
    const password = String(formData.get('password') || '').trim();
    const college = String(formData.get('college') || '').trim();
    const major = String(formData.get('major') || '').trim();
    const className = String(formData.get('className') || '').trim();
    
    if (!name) {
      setToast('请输入姓名');
      return;
    }
    if (!studentId) {
      setToast('请输入学号');
      return;
    }
    if (!password) {
      setToast('请输入密码');
      return;
    }
    if (password.length < 6) {
      setToast('密码至少6位');
      return;
    }
    if (!college) {
      setToast('请输入系部');
      return;
    }
    if (!major) {
      setToast('请输入专业');
      return;
    }
    if (!className) {
      setToast('请输入班级');
      return;
    }

    try {
      await apiFetch(token, '/api/v1/staff/students', {
        method: 'POST',
        body: JSON.stringify({
          name,
          studentId,
          password,
          college,
          major,
          className,
          grade: Number(formData.get('grade')) || 1,
          dormitory: String(formData.get('dormitory') || '').trim()
        })
      });

      setToast('学生账号创建成功');
      if (event.currentTarget) event.currentTarget.reset();
      setStaffCurrentPage(1);
      await loadStudentList(token);
    } catch (error) {
      setToast(error.message || '创建学生失败');
    }
  }

  async function handleStaffDeleteStudent(targetStudentId) {
    if (!confirm(`确定要删除学生 ${targetStudentId} 吗？此操作无法撤销。`)) {
      return;
    }

    try {
      await apiFetch(token, `/api/v1/staff/students/${targetStudentId}`, {
        method: 'DELETE'
      });

      setToast('学生删除成功');
      setStaffCurrentPage(1);
      await loadStudentList(token);
      if (targetStudentId === studentId) {
        setStudentId(null);
        setSummary(null);
        setSignals([]);
      }
    } catch (error) {
      setToast(error.message || '删除学生失败');
    }
  }

  async function handleStaffResetStudentPassword(studentId) {
    const newPassword = window.prompt(`请输入学生 ${studentId} 的新密码（至少6位）`, '');
    if (newPassword == null) {
      return;
    }

    const trimmedPassword = newPassword.trim();
    if (!trimmedPassword) {
      setToast('请输入新密码');
      return;
    }
    if (trimmedPassword.length < 6) {
      setToast('新密码至少6位');
      return;
    }

    try {
      await apiFetch(token, `/api/v1/staff/students/${studentId}/password`, {
        method: 'POST',
        body: JSON.stringify({ newPassword: trimmedPassword })
      });

      setToast(`已重置学生 ${studentId} 的密码`);
      setStaffCurrentPage(1);
    } catch (error) {
      setToast(error.message || '重置密码失败');
    }
  }

  async function handleDeleteStaff(staffId) {
    if (!confirm(`确定要删除教师 ${staffId} 吗？此操作无法撤销。`)) {
      return;
    }

    try {
      await apiFetch(token, `/api/v1/admin/staff/${staffId}`, {
        method: 'DELETE'
      });

      setToast('教师删除成功');
      await loadStaffList(token);
    } catch (error) {
      setToast(error.message || '删除教师失败');
    }
  }

  async function handleChangeAdminPassword(event) {
    event.preventDefault();
    
    if (!oldPassword) {
      setToast('请输入原密码');
      return;
    }
    if (!newPassword) {
      setToast('请输入新密码');
      return;
    }
    if (newPassword.length < 6) {
      setToast('新密码至少6位');
      return;
    }
    if (newPassword !== confirmPassword) {
      setToast('两次输入的密码不一致');
      return;
    }

    try {
      await apiFetch(token, '/api/v1/admin/password', {
        method: 'POST',
        body: JSON.stringify({
          oldPassword,
          newPassword
        })
      });

      setToast('密码修改成功');
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (error) {
      setToast(error.message || '修改密码失败');
    }
  }

  async function handleCreateStaff(event) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    
    const name = String(formData.get('name') || '').trim();
    const staffId = String(formData.get('staffId') || '').trim().toUpperCase();
    const password = String(formData.get('password') || '').trim();
    
    if (!name) {
      setToast('请输入姓名');
      return;
    }
    if (!staffId) {
      setToast('请输入工号');
      return;
    }
    if (!password) {
      setToast('请输入密码');
      return;
    }
    if (password.length < 6) {
      setToast('密码至少6位');
      return;
    }

    try {
      await apiFetch(token, '/api/v1/staff/register', {
        method: 'POST',
        body: JSON.stringify({
          name,
          staffId,
          password,
          department: String(formData.get('department') || '').trim(),
          title: String(formData.get('title') || '').trim()
        })
      });

      setToast('教师账号创建成功');
      if (event.currentTarget) event.currentTarget.reset();
      await loadStaffList(token);
    } catch (error) {
      setToast(error.message || '创建教师失败');
    }
  }

  function logout() {
    setPage('login');
    setSummary(null);
    setSignals([]);
    setInference(null);
    setAuditEvents([]);
    setToast('已退出登录');
  }

  async function selectRosterStudent(student) {
    const nextStudentId = student.studentId || student.id;
    setStudentId(nextStudentId);
    try {
      await loadAll(token, nextStudentId);
      setToast(`已切换到 ${student.name}`);
    } catch (error) {
      setToast(error.message || '切换学生失败');
    }
  }

  function renderLoginPage() {
    return (
      <>
        <section className="login-header">
          <div className="login-brand">
            <div className="brand-icon">🏥</div>
            <div>
              <span className="eyebrow">Campus Health Platform</span>
              <h1>校园健康平台</h1>
            </div>
          </div>
          <p className="login-desc">登录系统以查看和管理健康数据</p>
        </section>

        <section className="login-container">
          <div className="login-role-tabs">
            <button type="button" className={`role-tab ${loginMode === 'student' ? 'active' : ''}`} onClick={() => setLoginMode('student')}>
              <span className="role-icon">👨‍🎓</span>
              <span className="role-title">学生</span>
              <span className="role-desc">查看个人健康数据</span>
            </button>
            <button type="button" className={`role-tab ${loginMode === 'staff' ? 'active' : ''}`} onClick={() => setLoginMode('staff')}>
              <span className="role-icon">👨‍🏫</span>
              <span className="role-title">教师</span>
              <span className="role-desc">管理学生健康</span>
            </button>
            <button type="button" className={`role-tab ${loginMode === 'admin' ? 'active' : ''}`} onClick={() => setLoginMode('admin')}>
              <span className="role-icon">🔧</span>
              <span className="role-title">管理员</span>
              <span className="role-desc">系统管理</span>
            </button>
          </div>

          <div className="login-form-wrapper">
            <div className={`login-form-card ${loginMode === 'student' ? 'active' : ''}`}>
              <div className="form-tabs">
                <button type="button" className={`form-tab ${studentFormMode === 'login' ? 'active' : ''}`} onClick={() => setStudentFormMode('login')}>登录</button>
                <button type="button" className={`form-tab ${studentFormMode === 'register' ? 'active' : ''}`} onClick={() => setStudentFormMode('register')}>注册</button>
              </div>

              {studentFormMode === 'login' ? (
                <form className="auth-form" onSubmit={handleStudentLogin}>
                  <div className="form-group">
                    <label htmlFor="student-login-id">学号</label>
                    <input id="student-login-id" name="studentId" onChange={(event) => setStudentId(event.target.value.toUpperCase())} placeholder="请输入学号" />
                  </div>
                  <div className="form-group">
                    <label htmlFor="student-login-password">密码</label>
                    <input id="student-login-password" name="password" type="password" placeholder="请输入密码" />
                  </div>
                  <button className="primary-btn form-submit" type="submit">登录</button>
                </form>
              ) : (
                <form className="auth-form" onSubmit={handleStudentAuth}>
                  <div className="form-group">
                    <label htmlFor="student-reg-name">姓名</label>
                    <input id="student-reg-name" name="name" value={studentName} onChange={(event) => setStudentName(event.target.value)} placeholder="请输入姓名" />
                  </div>
                  <div className="form-group">
                    <label htmlFor="student-reg-id">学号</label>
                    <input id="student-reg-id" name="studentId" value={studentId} onChange={(event) => setStudentId(event.target.value.toUpperCase())} placeholder="请输入学号" />
                  </div>
                  <div className="form-group">
                    <label htmlFor="student-reg-password">密码</label>
                    <input id="student-reg-password" name="password" type="password" placeholder="请输入密码（至少6位）" />
                  </div>
                  <div className="form-group">
                    <label htmlFor="student-reg-college">系部</label>
                    <input id="student-reg-college" name="college" placeholder="请输入系部" />
                  </div>
                  <div className="form-group">
                    <label htmlFor="student-reg-major">专业</label>
                    <input id="student-reg-major" name="major" placeholder="请输入专业" />
                  </div>
                  <div className="form-group">
                    <label htmlFor="student-reg-class">班级</label>
                    <input id="student-reg-class" name="className" placeholder="请输入班级" />
                  </div>
                  <div className="form-group">
                    <label htmlFor="student-reg-grade">年级</label>
                    <input id="student-reg-grade" name="grade" type="number" placeholder="请输入年级" defaultValue="1" />
                  </div>
                  <div className="form-group">
                    <label htmlFor="student-reg-dorm">宿舍</label>
                    <input id="student-reg-dorm" name="dormitory" placeholder="请输入宿舍" />
                  </div>
                  <button className="primary-btn form-submit" type="submit">注册</button>
                </form>
              )}
            </div>

            <div className={`login-form-card ${loginMode === 'staff' ? 'active' : ''}`}>
              <div className="form-tabs">
                <button type="button" className={`form-tab ${staffFormMode === 'login' ? 'active' : ''}`} onClick={() => setStaffFormMode('login')}>登录</button>
                <button type="button" className={`form-tab ${staffFormMode === 'register' ? 'active' : ''}`} onClick={() => setStaffFormMode('register')}>注册</button>
              </div>

              {staffFormMode === 'login' ? (
                <form className="auth-form" onSubmit={handleStaffLogin}>
                  <div className="form-group">
                    <label htmlFor="staff-login-id">工号</label>
                    <input id="staff-login-id" name="staffId" placeholder="请输入工号" />
                  </div>
                  <div className="form-group">
                    <label htmlFor="staff-login-password">密码</label>
                    <input id="staff-login-password" name="password" type="password" placeholder="请输入密码" />
                  </div>
                  <button className="primary-btn form-submit" type="submit">登录</button>
                </form>
              ) : (
                <form className="auth-form" onSubmit={handleStaffRegister}>
                  <div className="form-group">
                    <label htmlFor="staff-reg-name">姓名</label>
                    <input id="staff-reg-name" name="name" placeholder="请输入姓名" />
                  </div>
                  <div className="form-group">
                    <label htmlFor="staff-reg-id">工号</label>
                    <input id="staff-reg-id" name="staffId" placeholder="请输入工号" />
                  </div>
                  <div className="form-group">
                    <label htmlFor="staff-reg-password">密码</label>
                    <input id="staff-reg-password" name="password" type="password" placeholder="请输入密码（至少6位）" />
                  </div>
                  <div className="form-group">
                    <label htmlFor="staff-reg-dept">部门</label>
                    <input id="staff-reg-dept" name="department" placeholder="请输入部门" />
                  </div>
                  <div className="form-group">
                    <label htmlFor="staff-reg-title">职称</label>
                    <input id="staff-reg-title" name="title" placeholder="请输入职称" />
                  </div>
                  <button className="primary-btn form-submit" type="submit">注册</button>
                </form>
              )}
            </div>

            <div className={`login-form-card ${loginMode === 'admin' ? 'active' : ''}`}>
              <div className="form-header">
                <h2>管理员登录</h2>
                <p>使用管理员账号和密码登录</p>
              </div>
              <form className="auth-form" onSubmit={handleAdminLogin}>
                <div className="form-group">
                  <label htmlFor="admin-username">管理员账号</label>
                  <input id="admin-username" name="username" placeholder="请输入管理员账号" value={adminUsername} onChange={(e) => setAdminUsername(e.target.value)} />
                </div>
                <div className="form-group">
                  <label htmlFor="admin-password">密码</label>
                  <input id="admin-password" name="password" type="password" placeholder="请输入密码" value={adminPassword} onChange={(e) => setAdminPassword(e.target.value)} />
                </div>
                <div className="form-note">
                  <span className="note-icon"></span>
                  <span>管理员可进行学生检索、筛选、审计和用户管理</span>
                </div>
                <button className="primary-btn form-submit" type="submit">登录管理台</button>
              </form>
            </div>
          </div>
        </section>
      </>
    );
  }

  function renderStudentPage() {
    return (
      <>
        <section className="hero card">
          <div className="hero-copy">
            <div className="eyebrow">Student Portal</div>
            <h1>学生健康主页</h1>
            <p>查看个人健康画像、风险趋势和模型建议，并自助提交最新观测。</p>
            <div className="hero-actions">
              <button className="primary-btn" type="button" onClick={() => loadAll().catch((error) => setToast(error.message))}>刷新我的数据</button>
              <button className="ghost-btn" type="button" onClick={logout}>退出登录</button>
            </div>
          </div>
          <div className="hero-status card-soft">
            <div className="status-header">
              <span><span className={`status-dot ${apiStatus.online ? 'online' : 'offline'}`} />{apiStatus.message}</span>
              <span>学生端</span>
            </div>
            <div className="status-grid">
              <div><span className="status-label">当前学生</span><strong>{currentStudent?.name || '-'}</strong></div>
              <div><span className="status-label">风险等级</span><strong>{currentAssessment?.riskLevel || '-'}</strong></div>
              <div><span className="status-label">风险分数</span><strong>{currentAssessment?.riskScore ?? '-'}</strong></div>
              <div><span className="status-label">最近更新时间</span><strong>{formatDateTime(currentAssessment?.assessedAt || lastLoadedAt)}</strong></div>
            </div>
          </div>
        </section>

        <section className="kpi-grid">
          {overviewCards.map((card) => <KpiCard key={card.label} {...card} />)}
        </section>

        <section className="dashboard-grid">
          <ChartCard eyebrow="我的画像" title="个人概览" subtitle="展示基础档案与最近观测摘要" action={<span className="tag">Self Service</span>}>
            <div className="student-card">
              <div className="student-avatar">{(currentStudent?.name || 'S').slice(0, 1)}</div>
              <div>
                <h3>{currentStudent?.name || '-'}</h3>
                <p>{currentStudent?.studentId || studentId} · {currentStudent?.college || '-'} / {currentStudent?.major || '-'}</p>
              </div>
            </div>
            <div className="info-grid">
              <div className="info-card"><span className="status-label">宿舍</span><strong>{currentStudent?.dormitory || '-'}</strong></div>
              <div className="info-card"><span className="status-label">学院</span><strong>{currentStudent?.college || '-'}</strong></div>
              <div className="info-card"><span className="status-label">专业</span><strong>{currentStudent?.major || '-'}</strong></div>
              <div className="info-card"><span className="status-label">班级</span><strong>{currentStudent?.className || '-'}</strong></div>
              <div className="info-card"><span className="status-label">年级</span><strong>{currentStudent?.grade ? `${currentStudent.grade}` : '-'}</strong></div>
            </div>
          </ChartCard>

          <ChartCard eyebrow="BI 趋势" title="睡眠与压力走势" subtitle="基于风险指标图计算的趋势分析" action={<button className="secondary-btn" type="button" onClick={() => loadSignals().catch((error) => setToast(error.message))}>刷新历史</button>}>
            <LineChart
              labels={lineLabels}
              series={[
                { label: '睡眠风险', values: sleepSeries, max: 100 },
                { label: '压力风险', values: stressSeries, max: 100 }
              ]}
            />
          </ChartCard>

          <ChartCard eyebrow="风险剖面" title="风险指标图" subtitle="以条形图呈现关键风险源，便于快速判断优先级" action={<button className="secondary-btn" type="button" onClick={() => reassess().catch((error) => setToast(error.message))}>重新评估</button>}>
            <HorizontalBarChart items={riskBars} />
            <div className="mini-section">
              <div className="mini-section-head">
                <h3>风险因子</h3>
                <span>{currentAssessment?.riskFactors?.length || 0} 项</span>
              </div>
              <div className="chip-row">
                {(currentAssessment?.riskFactors?.length ? currentAssessment.riskFactors : ['暂无显著异常']).map((factor, index) => <Chip key={factor} active={index === 0}>{factor}</Chip>)}
              </div>
            </div>
          </ChartCard>
        </section>

        <section className="dashboard-grid lower-grid">
          <ChartCard eyebrow="健康建议" title="个性化健康管理方案" subtitle="根据风险因子生成的健康管理方案" action={<button className="secondary-btn" type="button" onClick={() => reassess().catch((error) => setToast(error.message))}>刷新建议</button>}>
            {currentAssessment?.interventionPlan ? (
              <HealthAdvice immediateActions={currentAssessment.interventionPlan.immediateActions} />
            ) : (
              <span className="audit-empty">暂无健康建议，请先提交健康数据</span>
            )}
          </ChartCard>
          
          <ChartCard eyebrow="模型建议" title="健康建议生成" subtitle="在学生端运行辅助推理并查看推荐动作" action={<button className="secondary-btn" type="button" onClick={() => runInference().catch((error) => setToast(error.message))}>运行模型</button>}>
            <label className="field-label" htmlFor="prompt-input">Prompt</label>
            <textarea id="prompt-input" rows="3" value={prompt} onChange={(event) => setPrompt(event.target.value)} placeholder="例如：生成一份面向辅导员的干预建议" />

            <div className="confidence-card card-soft">
              <div className="confidence-head">
                <span>模型置信度</span>
                <strong>{confidencePercent ? `${confidencePercent}%` : '-'}</strong>
              </div>
              <div className="progress-track"><div className="progress-bar" style={{ width: `${confidencePercent}%` }} /></div>
            </div>
            <div className="mini-section">
              <div className="mini-section-head">
                <h3>模型叙述</h3>
                <span>{inference?.modelName || '-'}</span>
              </div>
              <p className="narrative">{inference?.narrative || '等待生成结果。'}</p>
            </div>
            <div className="mini-section">
              <div className="mini-section-head">
                <h3>推荐动作</h3>
                <span>{formatDateTime(inference?.generatedAt)}</span>
              </div>
              {(inference?.recommendedActions || []).length > 0 ? (
                <ul className="action-list">
                  {inference.recommendedActions.slice(0, 30).map((action, index) => (
                    <li key={index} className="action-item">
                      <span className="action-number">{index + 1}</span>
                      <span className="action-text">{action}</span>
                    </li>
                  ))}
                </ul>
              ) : (
                <span className="audit-empty">暂无推荐动作</span>
              )}
            </div>
          </ChartCard>

          <ChartCard eyebrow="自助录入" title="新增健康观测" subtitle="提交新的学生观测并触发后端评估" action={<span className="tag warning">Self Report</span>}>
            <form className="form-grid" onSubmit={(event) => submitSignal(event).catch((error) => setToast(error.message))}>
              <label><span>来源类型</span><select name="sourceType" defaultValue="MANUAL">{SOURCE_OPTIONS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label>
              <label><span>睡眠时长</span><input name="sleepHours" type="number" min="0" max="24" step="0.1" defaultValue="7.0" /></label>
              <label><span>熬夜次数/周</span><input name="lateNightCountPerWeek" type="number" min="0" step="1" defaultValue="2" /></label>
              <label><span>营养分数</span><input name="nutritionScore" type="number" min="0" max="100" step="1" defaultValue="80" /></label>
              <label><span>压力分数</span><input name="stressScore" type="number" min="0" max="100" step="1" defaultValue="40" /></label>
              <label><span>活动分钟/周</span><input name="physicalActivityMinutesPerWeek" type="number" min="0" step="10" defaultValue="120" /></label>
              <label><span>接触感染人数</span><input name="infectionContacts" type="number" min="0" step="1" defaultValue="0" /></label>
              <label><span>备注</span><input name="note" type="text" placeholder="例如：晚自习后疲劳明显" /></label>
              <label className="checkbox-row"><input name="feverReported" type="checkbox" /><span>报告发热</span></label>
              <label className="checkbox-row"><input name="coughReported" type="checkbox" /><span>报告咳嗽</span></label>
              <button className="primary-btn form-submit" type="submit">提交观测</button>
            </form>
          </ChartCard>
        </section>
      </>
    );
  }

  function renderStaffPage() {
    return (
      <>
        <section className="hero card">
          <div className="hero-copy">
            <div className="eyebrow">Staff Portal</div>
            <h1>教师健康管理台</h1>
            <p>查看所管理学生的健康状态、风险评估和干预建议。</p>
            <div className="hero-actions">
              <button className="primary-btn" type="button" onClick={() => loadAll().catch((error) => setToast(error.message))}>刷新数据</button>
              <button className="ghost-btn" type="button" onClick={logout}>退出登录</button>
            </div>
          </div>
          <div className="hero-status card-soft">
            <div className="status-header">
              <span><span className={`status-dot ${apiStatus.online ? 'online' : 'offline'}`} />{apiStatus.message}</span>
              <span>教师端</span>
            </div>
            <div className="status-grid">
              <div><span className="status-label">当前学生</span><strong>{currentStudent?.name || '-'}</strong></div>
              <div><span className="status-label">风险等级</span><strong>{currentAssessment?.riskLevel || '-'}</strong></div>
              <div><span className="status-label">风险分数</span><strong>{currentAssessment?.riskScore ?? '-'}</strong></div>
              <div><span className="status-label">最近更新时间</span><strong>{formatDateTime(currentAssessment?.assessedAt || lastLoadedAt)}</strong></div>
            </div>
          </div>
        </section>

        <section className="kpi-grid">
          {overviewCards.map((card) => <KpiCard key={card.label} {...card} />)}
        </section>

        <section className="dashboard-grid">
          <ChartCard eyebrow="学生检索" title="学生列表" subtitle="选择要查看的学生" action={<button className="secondary-btn" type="button" onClick={() => { setStaffCurrentPage(1); loadStudentList(token).catch((error) => setToast(error.message)); }}>刷新列表</button>}>
            <div className="student-filter-panel">
              <div className="student-search">
                <label className="field-label" htmlFor="staff-student-query">搜索学生</label>
                <input
                  id="staff-student-query"
                  value={staffStudentQuery}
                  onChange={(event) => {
                    setStaffStudentQuery(event.target.value);
                    setStaffCurrentPage(1);
                  }}
                  placeholder="输入学号、姓名、学院或专业"
                  className="student-search-input"
                />
              </div>
              <div className="student-filter-bar" role="tablist" aria-label="按学院筛选学生">
                {['ALL', ...new Set(studentList.map((student) => student.college).filter(Boolean))].map((college) => (
                  <button
                    key={college}
                    type="button"
                    className={`student-filter-chip ${staffCollegeFilter === college ? 'active' : ''}`}
                    onClick={() => {
                      setStaffCollegeFilter(college);
                      setStaffCurrentPage(1);
                    }}
                  >
                    {college === 'ALL' ? '全部学院' : college}
                  </button>
                ))}
              </div>
            </div>
            <div className="roster-grid">
              {(() => {
                const startIndex = (staffCurrentPage - 1) * staffPageSize;
                const filteredStudents = studentList.filter((student) => {
                  const keyword = staffStudentQuery.trim().toLowerCase();
                  const matchesQuery = !keyword || [student.studentId, student.name, student.college, student.major, student.dormitory].some((field) => String(field).toLowerCase().includes(keyword));
                  const matchesCollege = staffCollegeFilter === 'ALL' || student.college === staffCollegeFilter;
                  return matchesQuery && matchesCollege;
                });
                const paginatedStudents = filteredStudents.slice(startIndex, startIndex + staffPageSize);
                const groupedStudents = paginatedStudents.reduce((groups, student) => {
                  const collegeKey = student.college || '未分配学院';
                  if (!groups[collegeKey]) {
                    groups[collegeKey] = [];
                  }
                  groups[collegeKey].push(student);
                  return groups;
                }, {});

                if (filteredStudents.length === 0) {
                  return <div className="audit-empty">暂无学生数据，请先注册学生账号。</div>;
                }

                return (
                  <>
                    {Object.entries(groupedStudents).map(([college, students]) => (
                      <div key={college} className="student-college-group">
                        <div className="student-college-header">
                          <strong>{college}</strong>
                          <span>{students.length} 名学生</span>
                        </div>
                        <div className="student-college-list">
                          {students.map((student) => (
                            <div key={student.studentId} className={`roster-card-wrapper ${studentId === student.studentId ? 'active' : ''}`}>
                              <button type="button" className="student-btn roster-card" onClick={() => selectRosterStudent(student)}>
                                <div className="roster-card-head">
                                  <strong>{student.name}</strong>
                                  <span>{student.studentId}</span>
                                </div>
                                <span>{student.major}</span>
                                <span>{student.dormitory}</span>
                              </button>
                              <div className="roster-action-stack">
                                <button type="button" className="roster-action-btn primary" onClick={() => handleStaffResetStudentPassword(student.studentId)}>
                                  <span>重置密码</span>
                                </button>
                                <button type="button" className="roster-delete-btn" onClick={() => handleStaffDeleteStudent(student.studentId)}>
                                  <span>删除</span>
                                </button>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    ))}
                    <Pagination
                      totalItems={filteredStudents.length}
                      pageSize={staffPageSize}
                      currentPage={staffCurrentPage}
                      onPageChange={(page) => setStaffCurrentPage(page)}
                    />
                  </>
                );
              })()}
            </div>
          </ChartCard>

          <ChartCard eyebrow="创建学生" title="添加新学生" subtitle="教师可以为学生创建账号" action={<span className="tag">Create</span>}>
            <form className="form-grid" onSubmit={handleStaffCreateStudent}>
              <label><span>姓名</span><input name="name" placeholder="请输入姓名" /></label>
              <label><span>学号</span><input name="studentId" placeholder="请输入学号" /></label>
              <label><span>密码</span><input name="password" type="password" placeholder="请输入密码" /></label>
              <label><span>系部</span><input name="college" placeholder="请输入系部" /></label>
              <label><span>专业</span><input name="major" placeholder="请输入专业" /></label>
              <label><span>班级</span><input name="className" placeholder="请输入班级" /></label>
              <label><span>年级</span><input name="grade" type="number" placeholder="请输入年级" defaultValue="1" /></label>
              <label><span>宿舍</span><input name="dormitory" placeholder="请输入宿舍" /></label>
              <button className="primary-btn form-submit" type="submit">创建学生</button>
            </form>
          </ChartCard>

          <ChartCard eyebrow="学生画像" title="个人概览" subtitle="当前选中学生的基础档案" action={<span className="tag">Profile</span>}>
            <div className="student-card">
              <div className="student-avatar">{(currentStudent?.name || 'S').slice(0, 1)}</div>
              <div>
                <h3>{currentStudent?.name || '-'}</h3>
                <p>{currentStudent?.studentId || studentId} · {currentStudent?.college || '-'} / {currentStudent?.major || '-'}</p>
              </div>
            </div>
            <div className="info-grid">
              <div className="info-card"><span className="status-label">宿舍</span><strong>{currentStudent?.dormitory || '-'}</strong></div>
              <div className="info-card"><span className="status-label">学院</span><strong>{currentStudent?.college || '-'}</strong></div>
              <div className="info-card"><span className="status-label">专业</span><strong>{currentStudent?.major || '-'}</strong></div>
              <div className="info-card"><span className="status-label">班级</span><strong>{currentStudent?.className || '-'}</strong></div>
              <div className="info-card"><span className="status-label">年级</span><strong>{currentStudent?.grade ? `${currentStudent.grade}` : '-'}</strong></div>
            </div>
          </ChartCard>

          <ChartCard eyebrow="风险剖面" title="风险指标图" subtitle="以条形图呈现关键风险源" action={<button className="secondary-btn" type="button" onClick={() => reassess().catch((error) => setToast(error.message))}>重新评估</button>}>
            <HorizontalBarChart items={riskBars} />
            <div className="mini-section">
              <div className="mini-section-head">
                <h3>风险因子</h3>
                <span>{currentAssessment?.riskFactors?.length || 0} 项</span>
              </div>
              <div className="chip-row">
                {(currentAssessment?.riskFactors?.length ? currentAssessment.riskFactors : ['暂无显著异常']).map((factor, index) => <Chip key={factor} active={index === 0}>{factor}</Chip>)}
              </div>
            </div>
          </ChartCard>
        </section>

        <section className="dashboard-grid lower-grid">
          <ChartCard eyebrow="趋势分析" title="睡眠与压力走势" subtitle="基于风险指标图计算的趋势分析" action={<button className="secondary-btn" type="button" onClick={() => loadSignals().catch((error) => setToast(error.message))}>刷新历史</button>}>
            <LineChart
              labels={lineLabels}
              series={[
                { label: '睡眠风险', values: sleepSeries, max: 100 },
                { label: '压力风险', values: stressSeries, max: 100 }
              ]}
            />
          </ChartCard>

          <ChartCard eyebrow="模型建议" title="健康建议" subtitle="基于学生数据生成的干预建议" action={<button className="secondary-btn" type="button" onClick={() => runInference().catch((error) => setToast(error.message))}>生成建议</button>}>
            <label className="field-label" htmlFor="prompt-input-staff">Prompt</label>
            <textarea id="prompt-input-staff" rows="3" value={prompt} onChange={(event) => setPrompt(event.target.value)} />
            <div className="mini-section">
              <div className="mini-section-head">
                <h3>模型叙述</h3>
                <span>{inference?.modelName || '-'}</span>
              </div>
              <p className="narrative">{inference?.narrative || '等待生成结果。'}</p>
            </div>
            <div className="mini-section">
              <div className="mini-section-head">
                <h3>推荐动作</h3>
                <span>{formatDateTime(inference?.generatedAt)}</span>
              </div>
              {(inference?.recommendedActions || []).length > 0 ? (
                <ul className="action-list">
                  {inference.recommendedActions.slice(0, 30).map((action, index) => (
                    <li key={index} className="action-item">
                      <span className="action-number">{index + 1}</span>
                      <span className="action-text">{action}</span>
                    </li>
                  ))}
                </ul>
              ) : (
                <span className="audit-empty">暂无推荐动作</span>
              )}
            </div>
          </ChartCard>
        </section>
      </>
    );
  }

  function renderAdminPage() {
    const collegeOptions = ['ALL', ...new Set(studentList.map((student) => student.college).filter(Boolean))];
    const currentRiskLabel = selectedSnapshot?.riskLevel || currentAssessment?.riskLevel || '待加载';

    return (
      <>
        <section className="hero card admin-hero">
          <div className="hero-copy">
            <div className="eyebrow">Management Console</div>
            <h1>校园健康管理台</h1>
            <p>通过学生检索、筛选和审计看板，统一处理校园健康运营和风险处置。</p>
            <div className="hero-actions">
              <button className="primary-btn" type="button" onClick={() => loadAll().catch((error) => setToast(error.message))}>刷新管理视图</button>
              <button className="ghost-btn" type="button" onClick={logout}>退出登录</button>
            </div>
          </div>
          <div className="hero-status card-soft">
            <div className="status-header">
              <span><span className={`status-dot ${apiStatus.online ? 'online' : 'offline'}`} />{apiStatus.message}</span>
              <span>管理台</span>
            </div>
            <div className="status-grid">
              <div><span className="status-label">活动学生</span><strong>{studentList.length}</strong></div>
              <div><span className="status-label">审计记录</span><strong>{auditEvents.length}</strong></div>
              <div><span className="status-label">当前角色</span><strong>{currentRole === 'admin' ? '管理台' : currentRole === 'staff' ? '辅导员端' : '学生端'}</strong></div>
              <div><span className="status-label">最近同步</span><strong>{formatDateTime(lastLoadedAt)}</strong></div>
            </div>
          </div>
        </section>

        <div className="admin-tabs">
          <button type="button" className={`admin-tab ${adminTab === 'students' ? 'active' : ''}`} onClick={() => setAdminTab('students')}>学生管理</button>
          <button type="button" className={`admin-tab ${adminTab === 'staff' ? 'active' : ''}`} onClick={() => setAdminTab('staff')}>教师管理</button>
          <button type="button" className={`admin-tab ${adminTab === 'audit' ? 'active' : ''}`} onClick={() => setAdminTab('audit')}>审计日志</button>
          <button type="button" className={`admin-tab ${adminTab === 'password' ? 'active' : ''}`} onClick={() => setAdminTab('password')}>修改密码</button>
        </div>

        {adminTab === 'students' && (
          <>
            <section className="kpi-grid">
              <KpiCard label="全局风险" value={currentRiskLabel} hint="当前选中学生风险等级" />
              <KpiCard label="当前分数" value={selectedSnapshot?.riskScore ?? currentAssessment?.riskScore ?? '-'} hint="选中学生风险评分" />
              <KpiCard label="学生总数" value={studentList.length} hint="已注册学生数量" />
              <KpiCard label="模型置信度" value={confidencePercent ? `${confidencePercent}%` : '-'} hint="最近一次推理" />
            </section>

            <section className="dashboard-grid">
              <ChartCard eyebrow="学生检索" title="学生筛选面板" subtitle="按学号、姓名和学院过滤并快速切换学生" action={<button className="secondary-btn" type="button" onClick={() => loadStudentList(token).catch((error) => setToast(error.message))}>刷新列表</button>}>
                <div className="student-filter-panel">
                  <div className="student-search">
                    <label className="field-label" htmlFor="student-query">搜索学生</label>
                    <input id="student-query" value={studentQuery} onChange={(event) => { setStudentQuery(event.target.value); setCurrentPage(1); }} placeholder="输入学号、姓名、学院或专业" className="student-search-input" />
                  </div>
                  <div className="student-filter-bar" role="tablist" aria-label="按学院筛选学生">
                    {collegeOptions.map((college) => (
                      <button
                        key={college}
                        type="button"
                        className={`student-filter-chip ${collegeFilter === college ? 'active' : ''}`}
                        onClick={() => {
                          setCollegeFilter(college);
                          setCurrentPage(1);
                        }}
                      >
                        {college === 'ALL' ? '全部学院' : college}
                      </button>
                    ))}
                  </div>
                </div>
                <div className="roster-grid">
                  {(() => {
                    const filteredStudents = studentList.filter((student) => {
                      const keyword = studentQuery.trim().toLowerCase();
                      const matchesQuery = !keyword || [student.studentId, student.name, student.college, student.major, student.dormitory].some((field) => String(field).toLowerCase().includes(keyword));
                      const matchesCollege = collegeFilter === 'ALL' || student.college === collegeFilter;
                      return matchesQuery && matchesCollege;
                    });
                    
                    const startIndex = (currentPage - 1) * pageSize;
                    const paginatedStudents = filteredStudents.slice(startIndex, startIndex + pageSize);
                    const groupedStudents = paginatedStudents.reduce((groups, student) => {
                      const collegeKey = student.college || '未分配学院';
                      if (!groups[collegeKey]) {
                        groups[collegeKey] = [];
                      }
                      groups[collegeKey].push(student);
                      return groups;
                    }, {});
                    
                    if (filteredStudents.length === 0) {
                      return <div className="audit-empty">暂无学生数据。</div>;
                    }
                    
                    return (
                      <>
                        {Object.entries(groupedStudents).map(([college, students]) => (
                          <div key={college} className="student-college-group">
                            <div className="student-college-header">
                              <strong>{college}</strong>
                              <span>{students.length} 名学生</span>
                            </div>
                            <div className="student-college-list">
                              {students.map((student) => {
                                const snapshot = studentSnapshots[student.studentId];
                                return (
                                  <div key={student.studentId} className={`roster-card-wrapper ${studentId === student.studentId ? 'active' : ''}`}>
                                    <button type="button" className="student-btn roster-card" onClick={() => selectRosterStudent(student)}>
                                      <div className="roster-card-head">
                                        <strong>{student.name}</strong>
                                        <span>{student.studentId}</span>
                                      </div>
                                      <span>{student.major}</span>
                                      <span>{student.dormitory}</span>
                                      <span className="roster-risk">{snapshot ? `${snapshot.riskLevel} / ${snapshot.riskScore}` : '尚未加载风险信息'}</span>
                                    </button>
                                    <button type="button" className="roster-delete-btn" onClick={() => handleDeleteStudent(student.studentId)}>
                                      <span>删除</span>
                                    </button>
                                  </div>
                                );
                              })}
                            </div>
                          </div>
                        ))}
                        <Pagination
                          totalItems={filteredStudents.length}
                          pageSize={pageSize}
                          currentPage={currentPage}
                          onPageChange={(page) => setCurrentPage(page)}
                        />
                      </>
                    );
                  })()}
                </div>
              </ChartCard>

              <ChartCard eyebrow="创建学生" title="添加新学生" subtitle="管理员可以为学生创建账号" action={<span className="tag">Create</span>}>
                <form className="form-grid" onSubmit={handleCreateStudent}>
                  <label><span>姓名</span><input name="name" placeholder="请输入姓名" /></label>
                  <label><span>学号</span><input name="studentId" placeholder="请输入学号" /></label>
                  <label><span>密码</span><input name="password" type="password" placeholder="请输入密码" /></label>
                  <label><span>系部</span><input name="college" placeholder="请输入系部" /></label>
                  <label><span>专业</span><input name="major" placeholder="请输入专业" /></label>
                  <label><span>班级</span><input name="className" placeholder="请输入班级" /></label>
                  <label><span>年级</span><input name="grade" type="number" placeholder="年级" defaultValue="1" /></label>
                  <label><span>宿舍</span><input name="dormitory" placeholder="请输入宿舍" /></label>
                  <button className="primary-btn form-submit" type="submit">创建学生</button>
                </form>
              </ChartCard>

              <ChartCard eyebrow="学生画像" title="个人概览" subtitle="当前选中学生的基础档案" action={<span className="tag">Profile</span>}>
                <div className="student-card">
                  <div className="student-avatar">{(currentStudent?.name || 'S').slice(0, 1)}</div>
                  <div>
                    <h3>{currentStudent?.name || '-'}</h3>
                    <p>{currentStudent?.studentId || studentId} · {currentStudent?.college || '-'} / {currentStudent?.major || '-'}</p>
                  </div>
                </div>
                <div className="info-grid">
                  <div className="info-card"><span className="status-label">宿舍</span><strong>{currentStudent?.dormitory || '-'}</strong></div>
                  <div className="info-card"><span className="status-label">学院</span><strong>{currentStudent?.college || '-'}</strong></div>
                  <div className="info-card"><span className="status-label">专业</span><strong>{currentStudent?.major || '-'}</strong></div>
                  <div className="info-card"><span className="status-label">班级</span><strong>{currentStudent?.className || '-'}</strong></div>
                  <div className="info-card"><span className="status-label">年级</span><strong>{currentStudent?.grade ? `${currentStudent.grade}` : '-'}</strong></div>
                </div>
              </ChartCard>
            </section>

            <section className="dashboard-grid lower-grid">
              <ChartCard eyebrow="风险剖面" title="风险指标图" subtitle="以条形图呈现关键风险源" action={<button className="secondary-btn" type="button" onClick={() => reassess().catch((error) => setToast(error.message))}>重新评估</button>}>
                <HorizontalBarChart items={riskBars} />
              </ChartCard>

              <ChartCard eyebrow="趋势分析" title="睡眠与压力走势" subtitle="基于风险指标图计算的趋势分析" action={<button className="secondary-btn" type="button" onClick={() => loadSignals().catch((error) => setToast(error.message))}>刷新历史</button>}>
                <LineChart
                  labels={lineLabels}
                  series={[
                    { label: '睡眠风险', values: sleepSeries, max: 100 },
                    { label: '压力风险', values: stressSeries, max: 100 }
                  ]}
                />
              </ChartCard>
            </section>
          </>
        )}

        {adminTab === 'staff' && (
          <section className="dashboard-grid">
            <ChartCard eyebrow="教师列表" title="教师管理" subtitle="查看所有已注册教师" action={<button className="secondary-btn" type="button" onClick={() => { loadStaffList(token).catch((error) => setToast(error.message)); setStaffCurrentPage(1); }}>刷新</button>}>
              <div className="roster-grid">
                {(() => {
                  const startIndex = (staffCurrentPage - 1) * staffPageSize;
                  const paginatedStaff = staffList.slice(startIndex, startIndex + staffPageSize);
                  
                  if (staffList.length === 0) {
                    return <div className="audit-empty">暂无教师数据</div>;
                  }
                  
                  return (
                    <>
                      {paginatedStaff.map((staff) => (
                        <div key={staff.staffId} className="roster-card-wrapper">
                          <div className="student-btn roster-card">
                            <div className="roster-card-head">
                              <strong>{staff.name}</strong>
                              <span>{staff.staffId}</span>
                            </div>
                            <span>{staff.department} · {staff.title}</span>
                          </div>
                          <button type="button" className="roster-delete-btn" onClick={() => handleDeleteStaff(staff.staffId)}>
                            <span>删除</span>
                          </button>
                        </div>
                      ))}
                      <Pagination
                        totalItems={staffList.length}
                        pageSize={staffPageSize}
                        currentPage={staffCurrentPage}
                        onPageChange={(page) => setStaffCurrentPage(page)}
                      />
                    </>
                  );
                })()}
              </div>
            </ChartCard>

            <ChartCard eyebrow="创建教师" title="添加新教师" subtitle="管理员可以为教师创建账号" action={<span className="tag">Create</span>}>
              <form className="form-grid" onSubmit={handleCreateStaff}>
                <label><span>姓名</span><input name="name" placeholder="请输入姓名" /></label>
                <label><span>工号</span><input name="staffId" placeholder="请输入工号" /></label>
                <label><span>密码</span><input name="password" type="password" placeholder="请输入密码" /></label>
                <label><span>部门</span><input name="department" placeholder="请输入部门" /></label>
                <label><span>职称</span><input name="title" placeholder="请输入职称" /></label>
                <button className="primary-btn form-submit" type="submit">创建教师</button>
              </form>
            </ChartCard>
          </section>
        )}

        {adminTab === 'audit' && (
          <section className="dashboard-grid">
            <ChartCard eyebrow="审计日志" title="事件日志" subtitle="查看系统操作记录" action={<button className="secondary-btn" type="button" onClick={() => { loadAudit(token).catch((error) => setToast(error.message)); setAuditCurrentPage(1); }}>刷新</button>}>
              <div className="toolbar-group compact-group">
                <label htmlFor="audit-filter">筛选事件类型</label>
                <select id="audit-filter" value={auditFilter} onChange={(event) => { setAuditFilter(event.target.value); setAuditCurrentPage(1); }}>
                  {auditKinds.map((kind) => <option key={kind} value={kind}>{kind}</option>)}
                </select>
              </div>
              <div className="audit-list">
                {(() => {
                  const startIndex = (auditCurrentPage - 1) * auditPageSize;
                  const paginatedAudit = auditRows.slice(startIndex, startIndex + auditPageSize);
                  
                  if (auditRows.length === 0) {
                    return <div className="audit-empty">暂无审计记录。</div>;
                  }
                  
                  return (
                    <>
                      {paginatedAudit.map((event) => (
                        <article className="audit-item" key={event.eventId}>
                          <div className="audit-item-head">
                            <strong>{event.actionType}</strong>
                            <span>{formatDateTime(event.timestamp)}</span>
                          </div>
                          <p>{event.actorUserId} · {event.actorRole} · {event.resource} · {event.outcome}{event.details ? ` · ${event.details}` : ''}</p>
                        </article>
                      ))}
                      <Pagination
                        totalItems={auditRows.length}
                        pageSize={auditPageSize}
                        currentPage={auditCurrentPage}
                        onPageChange={(page) => setAuditCurrentPage(page)}
                      />
                    </>
                  );
                })()}
              </div>
            </ChartCard>
          </section>
        )}

        {adminTab === 'password' && (
          <section className="dashboard-grid">
            <ChartCard eyebrow="安全设置" title="修改管理员密码" subtitle="定期更换密码以保障账户安全" action={<span className="tag">Security</span>}>
              <form className="form-grid" onSubmit={handleChangeAdminPassword}>
                <label><span>原密码</span><input name="oldPassword" type="password" placeholder="请输入原密码" value={oldPassword} onChange={(e) => setOldPassword(e.target.value)} /></label>
                <label><span>新密码</span><input name="newPassword" type="password" placeholder="请输入新密码（至少6位）" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} /></label>
                <label><span>确认密码</span><input name="confirmPassword" type="password" placeholder="请再次输入新密码" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} /></label>
                <button className="primary-btn form-submit" type="submit">修改密码</button>
              </form>
            </ChartCard>
          </section>
        )}
      </>
    );
  }

  return (
    <AppShell>
      {page === 'login' ? renderLoginPage() : null}
      {page !== 'login' ? (
        <>
          <div className="hero-actions" style={{ marginTop: 16 }}>
            <button className="ghost-btn" type="button" onClick={() => setPage('login')}>返回登录页</button>
          </div>
          {page === 'student' ? renderStudentPage() : page === 'staff' ? renderStaffPage() : renderAdminPage()}
        </>
      ) : null}
      <div className={`toast ${toast ? 'visible' : ''}`}>{toast}</div>
    </AppShell>
  );
}