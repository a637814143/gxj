-- ============================================
-- 性能优化索引迁移脚本
-- 版本: V2
-- 创建日期: 2026-01-05
-- 说明: 为高频查询字段添加索引以提升查询性能
-- ============================================

-- YieldRecord表索引
-- 用于按区域和作物查询产量记录
CREATE INDEX IF NOT EXISTS idx_yield_record_region_crop 
ON yield_record(region_id, crop_id);

-- 用于按年份查询
CREATE INDEX IF NOT EXISTS idx_yield_record_year 
ON yield_record(year);

-- 复合索引：区域+作物+年份（最常用的查询组合）
CREATE INDEX IF NOT EXISTS idx_yield_record_region_crop_year 
ON yield_record(region_id, crop_id, year);

-- 用于数据集关联查询
CREATE INDEX IF NOT EXISTS idx_yield_record_dataset 
ON yield_record(dataset_file_id);

-- ============================================
-- WeatherRecord表索引
-- ============================================

-- 用于按区域查询天气记录
CREATE INDEX IF NOT EXISTS idx_weather_record_region 
ON weather_record(region_id);

-- 用于按日期查询
CREATE INDEX IF NOT EXISTS idx_weather_record_date 
ON weather_record(date);

-- 复合索引：区域+日期（最常用的查询组合）
CREATE INDEX IF NOT EXISTS idx_weather_record_region_date 
ON weather_record(region_id, date);

-- 用于数据集关联查询
CREATE INDEX IF NOT EXISTS idx_weather_record_dataset 
ON weather_record(dataset_file_id);

-- ============================================
-- ForecastResult表索引
-- ============================================

-- 用于按任务查询预测结果
CREATE INDEX IF NOT EXISTS idx_forecast_result_task 
ON forecast_result(task_id);

-- 用于按目标年份查询
CREATE INDEX IF NOT EXISTS idx_forecast_result_target_year 
ON forecast_result(target_year);

-- 复合索引：任务+年份（用于查找特定任务的特定年份结果）
CREATE INDEX IF NOT EXISTS idx_forecast_result_task_year 
ON forecast_result(task_id, target_year);

-- ============================================
-- ForecastSnapshot表索引
-- ============================================

-- 用于按运行ID查询快照
CREATE INDEX IF NOT EXISTS idx_forecast_snapshot_run 
ON forecast_snapshot(run_id);

-- 用于按创建时间倒序查询（仪表盘最近记录）
CREATE INDEX IF NOT EXISTS idx_forecast_snapshot_created_at 
ON forecast_snapshot(created_at DESC);

-- 复合索引：运行ID+周期（用于查询特定运行的时间序列）
CREATE INDEX IF NOT EXISTS idx_forecast_snapshot_run_period 
ON forecast_snapshot(run_id, period);

-- 用于按年份查询
CREATE INDEX IF NOT EXISTS idx_forecast_snapshot_year 
ON forecast_snapshot(year);

-- ============================================
-- ForecastRun表索引
-- ============================================

-- 用于按状态查询运行记录
CREATE INDEX IF NOT EXISTS idx_forecast_run_status 
ON forecast_run(status);

-- 用于按创建时间查询
CREATE INDEX IF NOT EXISTS idx_forecast_run_created_at 
ON forecast_run(created_at DESC);

-- 复合索引：模型+作物+区域（用于查找特定组合的运行）
CREATE INDEX IF NOT EXISTS idx_forecast_run_model_crop_region 
ON forecast_run(model_id, crop_id, region_id);

-- ============================================
-- ForecastTask表索引
-- ============================================

-- 复合索引：模型+作物+区域（用于查找或创建任务）
CREATE INDEX IF NOT EXISTS idx_forecast_task_model_crop_region 
ON forecast_task(model_id, crop_id, region_id);

-- 用于按状态查询任务
CREATE INDEX IF NOT EXISTS idx_forecast_task_status 
ON forecast_task(status);

-- 用于检查区域是否有任务
CREATE INDEX IF NOT EXISTS idx_forecast_task_region 
ON forecast_task(region_id);

-- 用于检查作物是否有任务
CREATE INDEX IF NOT EXISTS idx_forecast_task_crop 
ON forecast_task(crop_id);

-- ============================================
-- Consultation表索引
-- ============================================

-- 用于按状态查询咨询
CREATE INDEX IF NOT EXISTS idx_consultation_status 
ON consultation(status);

-- 用于按分配人查询
CREATE INDEX IF NOT EXISTS idx_consultation_assigned_to 
ON consultation(assigned_to);

-- 用于按创建时间倒序查询
CREATE INDEX IF NOT EXISTS idx_consultation_created_at 
ON consultation(created_at DESC);

-- 复合索引：状态+分配人（用于查询待处理的咨询）
CREATE INDEX IF NOT EXISTS idx_consultation_status_assigned 
ON consultation(status, assigned_to);

-- 用于按创建人查询
CREATE INDEX IF NOT EXISTS idx_consultation_created_by 
ON consultation(created_by);

-- 用于按部门查询
CREATE INDEX IF NOT EXISTS idx_consultation_department 
ON consultation(department_code);

-- ============================================
-- Report表索引
-- ============================================

-- 用于按发布时间倒序查询
CREATE INDEX IF NOT EXISTS idx_report_published_at 
ON report(published_at DESC);

-- 用于按预测结果查询报告
CREATE INDEX IF NOT EXISTS idx_report_forecast_result 
ON report(forecast_result_id);

-- 用于按创建时间查询
CREATE INDEX IF NOT EXISTS idx_report_created_at 
ON report(created_at DESC);

-- ============================================
-- User表索引
-- ============================================

-- 用于用户名登录查询（唯一索引）
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_username 
ON sys_user(username);

-- 用于邮箱查询（唯一索引）
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_email 
ON sys_user(email);

-- 用于按部门查询用户
CREATE INDEX IF NOT EXISTS idx_user_department 
ON sys_user(department_code);

-- 用于按状态查询
CREATE INDEX IF NOT EXISTS idx_user_enabled 
ON sys_user(enabled);

-- ============================================
-- LoginLog表索引
-- ============================================

-- 用于按用户名查询登录日志
CREATE INDEX IF NOT EXISTS idx_login_log_username 
ON sys_login_log(username);

-- 用于按时间倒序查询
CREATE INDEX IF NOT EXISTS idx_login_log_created_at 
ON sys_login_log(created_at DESC);

-- 用于按成功状态查询
CREATE INDEX IF NOT EXISTS idx_login_log_success 
ON sys_login_log(success);

-- 复合索引：用户名+时间（用于查询特定用户的登录历史）
CREATE INDEX IF NOT EXISTS idx_login_log_username_created 
ON sys_login_log(username, created_at DESC);

-- ============================================
-- RefreshToken表索引
-- ============================================

-- 用于按token查询（唯一索引）
CREATE UNIQUE INDEX IF NOT EXISTS idx_refresh_token_token 
ON sys_refresh_token(token);

-- 用于按用户ID查询
CREATE INDEX IF NOT EXISTS idx_refresh_token_user 
ON sys_refresh_token(user_id);

-- 用于按过期时间查询（清理过期token）
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires_at 
ON sys_refresh_token(expires_at);

-- ============================================
-- DatasetFile表索引
-- ============================================

-- 用于按文件类型查询
CREATE INDEX IF NOT EXISTS idx_dataset_file_type 
ON dataset_file(file_type);

-- 用于按上传时间查询
CREATE INDEX IF NOT EXISTS idx_dataset_file_uploaded_at 
ON dataset_file(uploaded_at DESC);

-- 用于按状态查询
CREATE INDEX IF NOT EXISTS idx_dataset_file_status 
ON dataset_file(status);

-- ============================================
-- DataImportJob表索引
-- ============================================

-- 用于按状态查询导入任务
CREATE INDEX IF NOT EXISTS idx_data_import_job_status 
ON data_import_job(status);

-- 用于按创建时间查询
CREATE INDEX IF NOT EXISTS idx_data_import_job_created_at 
ON data_import_job(created_at DESC);

-- 用于按数据集文件查询
CREATE INDEX IF NOT EXISTS idx_data_import_job_dataset 
ON data_import_job(dataset_file_id);

-- ============================================
-- SystemLog表索引
-- ============================================

-- 用于按创建时间倒序查询
CREATE INDEX IF NOT EXISTS idx_system_log_created_at 
ON sys_system_log(created_at DESC);

-- 用于按操作类型查询
CREATE INDEX IF NOT EXISTS idx_system_log_operation 
ON sys_system_log(operation);

-- 用于按用户查询
CREATE INDEX IF NOT EXISTS idx_system_log_username 
ON sys_system_log(username);

-- ============================================
-- 索引创建完成
-- ============================================

-- 验证索引创建
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
    'yield_record', 'weather_record', 'forecast_result', 
    'forecast_snapshot', 'forecast_run', 'forecast_task',
    'consultation', 'report', 'sys_user', 'sys_login_log'
  )
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;
