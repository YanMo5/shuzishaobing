# Campus Health Platform API Overview

## Base URL
`/api/v1`

## Health Check
- `GET /health/ping`

## Student Summary
- `GET /students/{studentId}/summary`
- Returns a sample student profile, current observation, and generated risk assessment.

## Risk Assessment
- `POST /assessments`
- Request body:

```json
{
  "student": {
    "studentId": "S1001",
    "name": "李明",
    "college": "计算机学院",
    "major": "软件工程",
    "grade": 2,
    "dormitory": "A3-512"
  },
  "observation": {
    "sleepHours": 5.5,
    "lateNightCountPerWeek": 4,
    "nutritionScore": 52,
    "stressScore": 78,
    "physicalActivityMinutesPerWeek": 60,
    "infectionContacts": 1,
    "feverReported": false,
    "coughReported": true
  }
}
```

## Staff Student Password Reset
- `POST /staff/students/{studentId}/password`
- Body:

```json
{
  "newPassword": "reset456"
}
```

说明：教师可使用该接口重置学生密码，系统会记录审计日志；学生随后可用新密码重新登录。
