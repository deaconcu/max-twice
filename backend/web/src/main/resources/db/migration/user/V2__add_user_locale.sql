-- 为 user 表增加 locale 字段，记录用户偏好语言。
--
-- 背景：项目中文/英文内容分库存放（twicemax_zh / twicemax_en），UI 语言必须跟内容语言一致。
-- 现状：未登录走 localStorage + Accept-Language；登录后需要让偏好跟账号走，跨设备一致。
--
-- 取值：'zh' / 'en'。默认 'en'：海外产品，非中文用户是主流；注册流程会根据
-- Accept-Language 覆盖（zh* → zh，其余 → en）。
--
-- 对老数据的影响：存量用户被统一置为 'en'，下次登录时前端会读到并切换；用户可在 UI
-- 里一键切回 'zh'（LanguageSwitcher），切换会同步写回后端。
ALTER TABLE user
    ADD COLUMN locale VARCHAR(10) NOT NULL DEFAULT 'en' AFTER timezone;
