export default function StudentList({ students, onSelect, selectedId, searchTerm, onSearchChange }) {
  const filteredStudents = students?.filter(student =>
    student.name?.toLowerCase().includes(searchTerm?.toLowerCase() || '') ||
    (student.studentId || student.id)?.toLowerCase().includes(searchTerm?.toLowerCase() || '') ||
    student.college?.toLowerCase().includes(searchTerm?.toLowerCase() || '')
  ) || [];

  return (
    <div className="student-list">
      <div className="student-search">
        <input
          type="text"
          placeholder="搜索学生姓名、学号或学院..."
          value={searchTerm || ''}
          onChange={(e) => onSearchChange?.(e.target.value)}
          className="student-search-input"
        />
      </div>

      <div className="student-list-scroll">
        {filteredStudents.length === 0 ? (
          <div className="empty-placeholder">暂无学生数据</div>
        ) : (
          <div className="student-list-grid">
            {filteredStudents.map((student) => {
              const initial = (student.name || '').charAt(0).toUpperCase() || '?';
              const studentKey = student.studentId || student.id;
              const selected = selectedId === studentKey;
              return (
                <div
                  key={studentKey}
                  className={`student-card ${selected ? 'selected' : ''}`}
                  onClick={() => onSelect?.(student)}
                >
                  <div className="student-left">
                    <div className="student-avatar">{initial}</div>
                    <div className="student-info">
                      <div className="student-name">{student.name}</div>
                      <div className="student-id">{studentKey}</div>
                    </div>
                  </div>
                  <div className="student-mid">
                    <div className="student-college">{student.college || '未知学院'}</div>
                    <div className="student-major">{student.major || '未知专业'}</div>
                  </div>
                  <div className="student-right">
                    <div className="student-class">{student.className || ''}</div>
                    <div className="student-dorm">{student.dormitory || ''}</div>
                    <div className="badge-grade">{student.grade ? `Grade ${student.grade}` : ''}</div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
