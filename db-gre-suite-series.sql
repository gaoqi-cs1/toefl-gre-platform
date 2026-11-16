UPDATE gre_suite SET title='GRE 全真模考 02 · 强化卷'          WHERE id=4;
UPDATE gre_suite SET title='GRE 全真模考 03 · 阅读侧重卷'      WHERE id=5;
UPDATE gre_suite SET title='GRE 全真模考 04 · 冲刺卷'          WHERE id=6;
UPDATE gre_suite SET title='GRE 全真模考 05 · 数学侧重卷'      WHERE id=7;
SELECT id, title, source, total_minutes FROM gre_suite ORDER BY id;