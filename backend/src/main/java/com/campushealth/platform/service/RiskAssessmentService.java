package com.campushealth.platform.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.campushealth.platform.model.HealthObservation;
import com.campushealth.platform.model.InterventionPlan;
import com.campushealth.platform.model.RiskAssessment;
import com.campushealth.platform.model.RiskLevel;
import com.campushealth.platform.model.StudentProfile;

@Service
public class RiskAssessmentService {

    public RiskAssessment assess(StudentProfile student, HealthObservation observation) {
        return assess(student, observation, null);
    }

    public RiskAssessment assess(StudentProfile student, HealthObservation observation, String focus) {
        int score = 0;
        List<String> riskFactors = new ArrayList<>();
        Map<String, Integer> factorScores = new LinkedHashMap<>();

        // Calculate risk based on radar chart formula
        double sleepPercent = Math.max(0, 10 - observation.sleepHours()) * 10;
        if (sleepPercent >= 50) {
            score += 25;
            riskFactors.add("睡眠严重不足");
            factorScores.put("睡眠严重不足", 25);
        } else if (sleepPercent >= 30) {
            score += 15;
            riskFactors.add("睡眠不足");
            factorScores.put("睡眠不足", 15);
        } else if (observation.sleepHours() > 8 && observation.sleepHours() < 10) {
            score += 5;
            riskFactors.add("睡眠偏多");
            factorScores.put("睡眠偏多", 5);
        }

        double lateNightPercent = observation.lateNightCountPerWeek() / 7.0 * 100;
        if (lateNightPercent >= 40) {
            score += 20;
            riskFactors.add("频繁熬夜");
            factorScores.put("频繁熬夜", 20);
        } else if (lateNightPercent >= 20) {
            score += 10;
            riskFactors.add("偶尔熬夜");
            factorScores.put("偶尔熬夜", 10);
        }

        double nutritionPercent = 100 - observation.nutritionScore();
        if (nutritionPercent >= 50) {
            score += 20;
            riskFactors.add("营养不良风险");
            factorScores.put("营养不良风险", 20);
        } else if (nutritionPercent >= 30) {
            score += 10;
            riskFactors.add("饮食不均衡");
            factorScores.put("饮食不均衡", 10);
        }

        if (observation.stressScore() >= 85) {
            score += 30;
            riskFactors.add("心理压力过高");
            factorScores.put("心理压力过高", 30);
        } else if (observation.stressScore() >= 70) {
            score += 18;
            riskFactors.add("心理压力偏高");
            factorScores.put("心理压力偏高", 18);
        } else if (observation.stressScore() >= 50) {
            score += 8;
            riskFactors.add("轻度压力");
            factorScores.put("轻度压力", 8);
        }

        double activityPercent = Math.max(0, 240 - observation.physicalActivityMinutesPerWeek()) / 2.0;
        if (activityPercent >= 75) {
            score += 15;
            riskFactors.add("运动严重不足");
            factorScores.put("运动严重不足", 15);
        } else if (activityPercent >= 30) {
            score += 8;
            riskFactors.add("运动偏少");
            factorScores.put("运动偏少", 8);
        }

        double infectionPercent = Math.min(100, 
            observation.infectionContacts() * 18 + 
            (observation.feverReported() ? 18 : 0) + 
            (observation.coughReported() ? 18 : 0));
        if (infectionPercent > 0) {
            score += (int)infectionPercent;
            if (observation.feverReported()) {
                riskFactors.add("发热症状");
                factorScores.put("发热症状", 18);
            }
            if (observation.coughReported()) {
                riskFactors.add("咳嗽症状");
                factorScores.put("咳嗽症状", 18);
            }
            if (observation.infectionContacts() > 0) {
                riskFactors.add("传染病接触");
                factorScores.put("传染病接触", (int)(observation.infectionContacts() * 18));
            }
        }

        int finalScore = Math.min(score, 100);
        RiskLevel riskLevel = classify(finalScore);

        return new RiskAssessment(
            student.studentId(),
            student.name(),
            riskLevel,
            finalScore,
            riskFactors,
            buildPlan(riskLevel, observation, factorScores, focus),
            Instant.now()
        );
    }

    private RiskLevel classify(int score) {
        if (score >= 80) {
            return RiskLevel.CRITICAL;
        }
        if (score >= 60) {
            return RiskLevel.HIGH;
        }
        if (score >= 30) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private InterventionPlan buildPlan(RiskLevel riskLevel, HealthObservation observation, Map<String, Integer> factorScores) {
        return buildPlan(riskLevel, observation, factorScores, null);
    }

    private InterventionPlan buildPlan(RiskLevel riskLevel, HealthObservation observation, Map<String, Integer> factorScores, String focus) {
        List<String> immediateActions = new ArrayList<>();
        List<String> followUpActions = new ArrayList<>();
        List<String> supportingServices = new ArrayList<>();

        immediateActions.add("--------------------------------------------------");
        immediateActions.add("您的个性化健康管理方案（根据风险因子生成）");
        immediateActions.add("--------------------------------------------------");
        immediateActions.add("");

        if (focus != null && !focus.isEmpty()) {
            generateFocusedAdvice(immediateActions, focus, observation, factorScores);
        } else {
            double sleepPercent = Math.max(0, 10 - observation.sleepHours()) * 10;
            double lateNightPercent = observation.lateNightCountPerWeek() / 7.0 * 100;
            double nutritionPercent = 100 - observation.nutritionScore();
            double activityPercent = Math.max(0, 240 - observation.physicalActivityMinutesPerWeek()) / 2.0;
            double infectionPercent = Math.min(100, 
                observation.infectionContacts() * 18 + 
                (observation.feverReported() ? 18 : 0) + 
                (observation.coughReported() ? 18 : 0));

            boolean hasSleepIssue = sleepPercent >= 30 || lateNightPercent >= 20;
            if (hasSleepIssue) {
                immediateActions.addAll(generateSleepAdvice(observation, factorScores, sleepPercent, lateNightPercent));
                immediateActions.add("");
            }

            if (nutritionPercent >= 30) {
                immediateActions.addAll(generateNutritionAdvice(observation, factorScores, nutritionPercent));
                immediateActions.add("");
            }

            if (activityPercent >= 30) {
                immediateActions.addAll(generateExerciseAdvice(observation, factorScores, activityPercent));
                immediateActions.add("");
            }

            if (observation.stressScore() >= 50) {
                immediateActions.addAll(generateMentalHealthAdvice(observation, factorScores));
                immediateActions.add("");
            }

            if (infectionPercent > 0) {
                immediateActions.addAll(generateDiseasePreventionAdvice(observation, factorScores, infectionPercent));
                immediateActions.add("");
            }

            immediateActions.addAll(generateLifestyleAdvice(observation, factorScores));
            immediateActions.add("");
        }

        switch (riskLevel) {
            case CRITICAL:
                immediateActions.add("--------------------------------------------------");
                immediateActions.add("紧急提醒：您的健康风险等级为严重");
                immediateActions.add("请立即采取行动，联系校医务室！");
                immediateActions.add("--------------------------------------------------");
                immediateActions.add("");
                immediateActions.add("紧急联系：");
                immediateActions.add("- 校医务室：立即前往检查");
                immediateActions.add("- 辅导员：联系安排面谈");
                immediateActions.add("- 防护措施：佩戴口罩，避免人群");
                immediateActions.add("");
                followUpActions.add("24小时内回访：");
                followUpActions.add("- 校医健康复核");
                followUpActions.add("- 辅导员联系了解情况");
                followUpActions.add("- 每日体温监测");
                supportingServices.add("校医务室（紧急）");
                supportingServices.add("心理咨询中心");
                supportingServices.add("辅导员工作台");
                supportingServices.add("校医院绿色通道");
                break;
            case HIGH:
                immediateActions.add("--------------------------------------------------");
                immediateActions.add("重要提醒：您的健康风险等级为高");
                immediateActions.add("建议本周内采取改善措施");
                immediateActions.add("--------------------------------------------------");
                immediateActions.add("");
                immediateActions.add("本周目标：");
                immediateActions.add("- 预约校医健康评估");
                immediateActions.add("- 联系心理咨询中心");
                immediateActions.add("- 调整作息，减少熬夜");
                immediateActions.add("");
                followUpActions.add("三天内复查：");
                followUpActions.add("- 复测睡眠、情绪、饮食指标");
                followUpActions.add("一周后跟踪：");
                followUpActions.add("- 提交新一轮健康数据");
                followUpActions.add("- 对比评估改善效果");
                supportingServices.add("校医务室");
                supportingServices.add("心理咨询中心");
                supportingServices.add("辅导员工作台");
                supportingServices.add("营养师咨询");
                break;
            case MEDIUM:
                immediateActions.add("--------------------------------------------------");
                immediateActions.add("温馨提醒：您的健康风险等级为中等");
                immediateActions.add("建议关注以下改善方向");
                immediateActions.add("--------------------------------------------------");
                immediateActions.add("");
                immediateActions.add("两周计划：");
                immediateActions.add("- 调整作息时间");
                immediateActions.add("- 增加每日运动");
                immediateActions.add("- 注意营养均衡");
                immediateActions.add("");
                followUpActions.add("两周后评估：");
                followUpActions.add("- 提交新一轮健康数据");
                followUpActions.add("- 对比改善效果");
                followUpActions.add("持续监测：");
                followUpActions.add("- 每周记录健康数据");
                supportingServices.add("校园健康管理平台");
                supportingServices.add("体育教师指导");
                supportingServices.add("健康教育课程");
                break;
            case LOW:
                immediateActions.add("--------------------------------------------------");
                immediateActions.add("良好状态：您的健康风险等级为低");
                immediateActions.add("继续保持健康生活习惯！");
                immediateActions.add("--------------------------------------------------");
                immediateActions.add("");
                immediateActions.add("保持习惯：");
                immediateActions.add("- 保持良好作息");
                immediateActions.add("- 维持适量运动");
                immediateActions.add("- 营养均衡摄入");
                immediateActions.add("");
                followUpActions.add("定期复查：");
                followUpActions.add("- 每周记录健康数据");
                followUpActions.add("- 关注指标变化趋势");
                followUpActions.add("健康提升：");
                followUpActions.add("- 适当增加运动时长");
                supportingServices.add("健康教育内容推荐");
                supportingServices.add("校园健身活动");
                supportingServices.add("定期体检提醒");
                break;
        }

        return new InterventionPlan(immediateActions, followUpActions, supportingServices);
    }

    private void generateFocusedAdvice(List<String> actions, String focus, HealthObservation observation, Map<String, Integer> factorScores) {
        actions.add("当前焦点：" + focus);
        actions.add("--------------------------------------------------");
        actions.add("");

        switch (focus) {
            case "睡眠管理":
                double sleepPercent = Math.max(0, 10 - observation.sleepHours()) * 10;
                double lateNightPercent = observation.lateNightCountPerWeek() / 7.0 * 100;
                actions.addAll(generateSleepAdvice(observation, factorScores, sleepPercent, lateNightPercent));
                break;
            case "心理压力":
                actions.addAll(generateMentalHealthAdvice(observation, factorScores));
                break;
            case "传染病风险":
                double infectionPercent = Math.min(100, 
                    observation.infectionContacts() * 18 + 
                    (observation.feverReported() ? 18 : 0) + 
                    (observation.coughReported() ? 18 : 0));
                actions.addAll(generateDiseasePreventionAdvice(observation, factorScores, infectionPercent));
                break;
            case "营养干预":
                double nutritionPercent = 100 - observation.nutritionScore();
                actions.addAll(generateNutritionAdvice(observation, factorScores, nutritionPercent));
                break;
            case "运动不足":
                double activityPercent = Math.max(0, 240 - observation.physicalActivityMinutesPerWeek()) / 2.0;
                actions.addAll(generateExerciseAdvice(observation, factorScores, activityPercent));
                break;
            case "身材管理":
                double activityPercentBody = Math.max(0, 240 - observation.physicalActivityMinutesPerWeek()) / 2.0;
                double nutritionPercentBody = 100 - observation.nutritionScore();
                actions.addAll(generateExerciseAdvice(observation, factorScores, activityPercentBody));
                actions.add("");
                actions.addAll(generateNutritionAdvice(observation, factorScores, nutritionPercentBody));
                break;
            default:
                actions.add("未知焦点方向：" + focus);
                actions.add("请选择以下焦点方向之一：睡眠管理、心理压力、传染病风险、营养干预、运动不足、身材管理");
        }
        actions.add("");
    }

    private List<String> generateSleepAdvice(HealthObservation observation, Map<String, Integer> factorScores, double sleepPercent, double lateNightPercent) {
        List<String> advice = new ArrayList<>();
        advice.add("【睡眠改善方案】");
        advice.add("--------------------------------------------------");

        boolean hasSleepIssue = sleepPercent >= 30;
        boolean hasLateNightIssue = lateNightPercent >= 20;

        if (hasSleepIssue) {
            advice.add("当前睡眠风险指数：" + (int)sleepPercent + "%");
            advice.add("当前状态：睡眠时长 " + String.format("%.1f", observation.sleepHours()) + "小时/天");
            if (sleepPercent >= 50) {
                advice.add("问题：睡眠严重不足！");
            } else {
                advice.add("问题：睡眠不足");
            }
            advice.add("目标：每日睡眠7-8小时");
            advice.add("");
            advice.add("阶梯式改善计划：");
            advice.add("- 第1-3天：记录当前睡眠时间，分析熬夜原因");
            advice.add("- 第4-7天：提前30分钟就寝，逐步增加睡眠");
            advice.add("- 第2周：固定就寝时间，睡前1小时远离电子设备");
            advice.add("- 第3-4周：形成稳定作息，睡眠时长达到7小时以上");
            advice.add("");
            advice.add("睡前助眠技巧：");
            advice.add("- 睡前温水澡：帮助身体放松，提升睡眠质量");
            advice.add("- 阅读替代刷手机：选择纸质书或听轻音乐");
            advice.add("- 呼吸放松法：4-7-8呼吸法（吸气4秒、屏息7秒、呼气8秒）");
            advice.add("- 环境优化：保持卧室18-22度，黑暗、安静");
        }

        if (hasLateNightIssue) {
            if (!hasSleepIssue) {
                advice.add("当前熬夜风险指数：" + (int)lateNightPercent + "%");
            }
            advice.add("当前状态：每周熬夜 " + observation.lateNightCountPerWeek() + "次");
            advice.add("问题：熬夜频率偏高，影响生物钟");
            advice.add("目标：每周熬夜控制在1次以内");
            advice.add("");
            advice.add("拒绝熬夜攻略：");
            advice.add("- 时间管理：设定每日学习截止时间（建议22:00前）");
            advice.add("- 任务拆分：将大任务分解为小目标，避免拖延");
            advice.add("- 早起替代熬夜：凌晨学习改为清晨7点起床");
            advice.add("- 周末不补觉：周末起床时间与平时不超过1小时");
        }

        if (!hasSleepIssue && !hasLateNightIssue) {
            advice.add("您的睡眠状况良好！");
            advice.add("睡眠保持建议：");
            advice.add("- 继续保持固定的作息时间");
            advice.add("- 睡前避免摄入咖啡因");
            advice.add("- 营造舒适的睡眠环境");
        }

        advice.add("");
        advice.add("专家提示：坚持2周后，睡眠质量将明显改善，精力充沛！");
        return advice;
    }

    private List<String> generateNutritionAdvice(HealthObservation observation, Map<String, Integer> factorScores, double nutritionPercent) {
        List<String> advice = new ArrayList<>();
        advice.add("【营养改善方案】");
        advice.add("--------------------------------------------------");

        if (nutritionPercent > 0) {
            advice.add("当前营养风险指数：" + (int)nutritionPercent + "%");
            advice.add("当前状态：营养评分 " + observation.nutritionScore() + "分（满分100）");
            if (nutritionPercent >= 50) {
                advice.add("问题：营养严重失衡！");
            } else {
                advice.add("问题：饮食结构需要调整");
            }
            advice.add("目标：营养评分提升至70分以上");
        } else {
            advice.add("您的营养状况良好！");
            advice.add("营养保持建议：");
        }

        advice.add("");
        advice.add("三餐营养搭配：");
        advice.add("- 早餐（吃好）：能量来源");
        advice.add("  - 碳水化合物：燕麦、全麦面包、杂粮粥");
        advice.add("  - 蛋白质：鸡蛋、牛奶、豆浆");
        advice.add("  - 维生素：水果（香蕉、苹果、橙子）");
        advice.add("- 午餐（吃饱）：补充能量");
        advice.add("  - 主食：米饭、面条（适量）");
        advice.add("  - 蔬菜：深色蔬菜为主（菠菜、青菜、西兰花）");
        advice.add("  - 蛋白质：鸡胸肉、鱼、豆腐、牛肉");
        advice.add("- 晚餐（吃少）：清淡为主");
        advice.add("  - 蔬菜为主，适量蛋白质");
        advice.add("  - 避免油炸、辛辣、过咸过甜");
        advice.add("");
        advice.add("健康饮食原则：");
        advice.add("- 每天5份蔬菜水果（约500克）");
        advice.add("- 每天饮水1500-2000ml，少喝碳酸饮料");
        advice.add("- 减少零食、甜食、油炸食品摄入");
        advice.add("- 主食粗细搭配，增加杂粮比例");
        advice.add("");
        advice.add("推荐一周食谱：");
        advice.add("- 周一：燕麦粥+水煮蛋+苹果 | 米饭+清炒时蔬+蒸鱼 | 蔬菜沙拉+酸奶");
        advice.add("- 周二：全麦面包+牛奶+香蕉 | 杂粮饭+宫保鸡丁+凉拌黄瓜 | 番茄蛋汤+玉米");
        advice.add("- 周三：豆浆+油条（适量） | 面条+牛肉+青菜 | 水果+坚果");
        advice.add("- 周四：小米粥+咸蛋 | 米饭+红烧肉+炒豆角 | 酸奶+水果");
        advice.add("- 周五：馄饨+小笼包 | 炒饭+紫菜蛋花汤 | 蔬菜+凉拌木耳");

        advice.add("");
        advice.add("专家提示：坚持1个月后营养评分可提升15-20分！");
        return advice;
    }

    private List<String> generateExerciseAdvice(HealthObservation observation, Map<String, Integer> factorScores, double activityPercent) {
        List<String> advice = new ArrayList<>();
        advice.add("【运动健身方案】");
        advice.add("--------------------------------------------------");

        if (activityPercent > 0) {
            advice.add("当前运动风险指数：" + (int)activityPercent + "%");
            advice.add("当前状态：每周运动 " + observation.physicalActivityMinutesPerWeek() + "分钟");
            if (activityPercent >= 75) {
                advice.add("问题：运动严重不足！");
            } else {
                advice.add("问题：运动量偏少，体质偏弱");
            }
            advice.add("目标：每周运动达到150分钟以上");
        } else {
            advice.add("您的运动量达标！");
            advice.add("运动保持建议：");
        }

        advice.add("");
        advice.add("四周循序渐进计划：");
        advice.add("第1周：适应期");
        advice.add("  - 每天散步20-30分钟（校园、公园）");
        advice.add("  - 早起做广播体操、拉伸运动");
        advice.add("  - 目标：累计60分钟");
        advice.add("");
        advice.add("第2周：提升期");
        advice.add("  - 每天快走30-40分钟");
        advice.add("  - 加入简单力量训练（深蹲、俯卧撑、平板支撑）");
        advice.add("  - 目标：累计120分钟");
        advice.add("");
        advice.add("第3周：强化期");
        advice.add("  - 每周3次慢跑（每次20-30分钟）");
        advice.add("  - 每天快走+力量训练");
        advice.add("  - 目标：累计180分钟");
        advice.add("");
        advice.add("第4周：保持期");
        advice.add("  - 慢跑/游泳/骑行（每周3-4次，每次30分钟）");
        advice.add("  - 每天拉伸放松");
        advice.add("  - 目标：累计210分钟以上");
        advice.add("");
        advice.add("校园运动推荐：");
        advice.add("- 有氧运动：跑步、游泳、骑行");
        advice.add("- 球类运动：羽毛球、乒乓球、篮球");
        advice.add("- 室内运动：瑜伽、普拉提、健身操");
        advice.add("- 力量训练：哑铃、器械、自重训练");
        advice.add("");
        advice.add("运动注意事项：");
        advice.add("- 运动前：5-10分钟热身拉伸");
        advice.add("- 运动中：适量补充水分");
        advice.add("- 运动后：放松拉伸，避免立即洗澡");
        advice.add("");
        advice.add("专家提示：坚持运动1个月后，体质明显提升，精力充沛！");
        return advice;
    }

    private List<String> generateMentalHealthAdvice(HealthObservation observation, Map<String, Integer> factorScores) {
        List<String> advice = new ArrayList<>();
        advice.add("【心理健康方案】");
        advice.add("--------------------------------------------------");

        if (observation.stressScore() > 0) {
            advice.add("当前状态：压力评分 " + observation.stressScore() + "分");
            if (observation.stressScore() >= 85) {
                advice.add("问题：心理压力过高！");
            } else if (observation.stressScore() >= 70) {
                advice.add("问题：心理压力偏高");
            } else {
                advice.add("问题：有轻度压力");
            }
            advice.add("目标：压力评分降低到50分以下");
        } else {
            advice.add("您的心理状态良好！");
            advice.add("心情保持建议：");
        }

        advice.add("");
        advice.add("压力缓解技巧：");
        advice.add("- 每日放松练习（推荐时间：早起、睡前）");
        advice.add("  - 深呼吸：4-7-8呼吸法，重复3-5次");
        advice.add("  - 冥想：闭眼静思5-10分钟，专注呼吸");
        advice.add("  - 渐进性肌肉放松：从脚到头，每组肌肉先紧张后放松");
        advice.add("");
        advice.add("- 时间管理技巧");
        advice.add("  - 制定每日任务清单，按优先级完成");
        advice.add("  - 学会说\"不\"，避免过度承诺");
        advice.add("  - 将大任务分解为小步骤");
        advice.add("  - 番茄工作法：25分钟专注工作，5分钟休息");
        advice.add("");
        advice.add("- 情绪释放方式");
        advice.add("  - 写日记：记录情绪和想法");
        advice.add("  - 艺术表达：绘画、音乐、写作");
        advice.add("  - 倾诉交流：与朋友、家人、辅导员聊天");
        advice.add("  - 兴趣爱好：培养一项能带来快乐的兴趣");
        advice.add("");
        advice.add("- 身心调节方法");
        advice.add("  - 每周3-4次有氧运动，释放内啡肽");
        advice.add("  - 保证7-8小时睡眠");
        advice.add("  - 减少咖啡因和酒精摄入");
        advice.add("");
        advice.add("寻求专业帮助：");
        advice.add("- 心理咨询中心：预约专业心理咨询师");
        advice.add("- 辅导员：可提供心理支持和资源转介");
        advice.add("- 紧急情况：校心理危机干预热线");
        advice.add("");
        advice.add("专家提示：1个月内压力分数可降低10-20分！");
        return advice;
    }

    private List<String> generateDiseasePreventionAdvice(HealthObservation observation, Map<String, Integer> factorScores, double infectionPercent) {
        List<String> advice = new ArrayList<>();
        advice.add("【疾病防护方案】");
        advice.add("--------------------------------------------------");

        advice.add("当前感染风险指数：" + (int)infectionPercent + "%");

        if (observation.feverReported()) {
            advice.add("当前状态：报告发热症状");
            advice.add("问题：体温异常，需要关注");
            advice.add("");
            advice.add("发热应对措施：");
            advice.add("- 体温监测：每4小时测量一次，记录变化");
            advice.add("- 退热处理：");
            advice.add("  - 体温<38.5度：物理降温（温水擦浴、多喝水）");
            advice.add("  - 体温>=38.5度：可服用退烧药（遵医嘱）");
            advice.add("- 就医建议：");
            advice.add("  - 立即前往校医务室或医院就诊");
            advice.add("  - 如持续高热超过24小时，立即就医");
            advice.add("- 防护措施：");
            advice.add("  - 佩戴口罩，避免传染他人");
            advice.add("  - 多饮温水，注意休息");
            advice.add("  - 通知辅导员和家人");
        }

        if (observation.coughReported()) {
            if (!observation.feverReported()) {
                advice.add("当前状态：报告咳嗽症状");
                advice.add("问题：呼吸系统不适");
            }
            advice.add("");
            advice.add("咳嗽护理措施：");
            advice.add("- 多饮温水：保持喉部湿润，缓解咳嗽");
            advice.add("- 食疗缓解：蜂蜜水、梨汤、川贝枇杷膏");
            advice.add("- 避免刺激：辛辣、油腻、过甜过咸食物");
            advice.add("- 环境调节：保持室内空气清新，避免烟尘");
            advice.add("- 药物治疗：可服用止咳润喉药物");
            advice.add("- 就医建议：");
            advice.add("  - 咳嗽持续超过1周");
            advice.add("  - 伴有发热、胸闷、呼吸困难");
            advice.add("  - 立即就医检查");
        }

        if (observation.infectionContacts() > 0) {
            if (!observation.feverReported() && !observation.coughReported()) {
                advice.add("当前状态：近一周接触传染病患者" + observation.infectionContacts() + "次");
                advice.add("问题：存在感染风险");
            }
            advice.add("");
            advice.add("防护措施：");
            advice.add("- 个人防护：");
            advice.add("  - 外出佩戴口罩");
            advice.add("  - 勤洗手（肥皂+流水，不少于20秒）");
            advice.add("  - 避免用手触摸眼、口、鼻");
            advice.add("- 环境防护：");
            advice.add("  - 每天开窗通风不少于30分钟");
            advice.add("  - 定期消毒常接触物品（手机、门把手等）");
            advice.add("  - 保持宿舍清洁干燥");
            advice.add("- 增强免疫：");
            advice.add("  - 保证充足睡眠7-8小时");
            advice.add("  - 均衡营养，多吃蔬菜水果");
            advice.add("  - 适度运动，提高抵抗力");
            advice.add("- 健康监测：");
            advice.add("  - 每日测量体温");
            advice.add("  - 如有不适症状立即就医并报告");
        }

        if (!observation.feverReported() && !observation.coughReported() && observation.infectionContacts() == 0) {
            advice.add("您没有感染风险！");
            advice.add("日常防护建议：");
            advice.add("- 保持良好的个人卫生习惯");
            advice.add("- 避免人群聚集场所戴口罩");
            advice.add("- 均衡饮食，适度运动");
        }

        advice.add("");
        advice.add("专家提示：做好防护可有效降低感染风险！");
        return advice;
    }

    private List<String> generateLifestyleAdvice(HealthObservation observation, Map<String, Integer> factorScores) {
        List<String> advice = new ArrayList<>();
        advice.add("【生活习惯方案】");
        advice.add("--------------------------------------------------");
        advice.add("健康生活习惯建议：");
        advice.add("");

        double sleepPercent = Math.max(0, 10 - observation.sleepHours()) * 10;
        double lateNightPercent = observation.lateNightCountPerWeek() / 7.0 * 100;
        boolean hasSleepIssue = sleepPercent >= 30 || lateNightPercent >= 20;

        if (hasSleepIssue) {
            advice.add("- 作息规律");
            advice.add("  - 固定起床时间（建议7:00左右）");
            advice.add("  - 固定就寝时间（建议22:30前）");
            advice.add("  - 避免睡前使用电子设备");
            advice.add("");
        }

        advice.add("- 饮水习惯");
        advice.add("  - 每天饮水1500-2000ml");
        advice.add("  - 早起一杯温水");
        advice.add("  - 少喝碳酸饮料和奶茶");
        advice.add("");

        advice.add("- 学习与休息平衡");
        advice.add("  - 每学习1小时，休息10分钟");
        advice.add("  - 适当户外活动，呼吸新鲜空气");
        advice.add("  - 培养兴趣爱好，丰富课余生活");

        return advice;
    }
}
