-- 种子数据：模板 + 演示用户
-- 封面图随源码放在 backend/src/main/resources/static/demo/covers/，由 Spring Boot 静态资源对外提供

INSERT INTO app_user (public_id, nickname, email) VALUES
    ('00000000-0000-4000-8000-000000000001', '演示用户 A', 'demo-a@example.com'),
    ('00000000-0000-4000-8000-000000000002', '演示用户 B', 'demo-b@example.com')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);

INSERT INTO template
    (id, title, category, cover_url, description, required_image_count, accepted_mime_types, max_file_size_bytes, sort_order)
VALUES
    ('demo-postcard', '旅行明信片', '创意图片', '/demo/covers/postcard.svg',
     '上传两张照片，模拟合成一张带边框与印章的旅行明信片。', 2, 'image/jpeg,image/png', 5242880, 10),

    ('demo-watercolor', '水彩头像', '创意图片', '/demo/covers/watercolor.svg',
     '上传一张人像，模拟生成水彩风格头像。', 1, 'image/jpeg,image/png', 5242880, 20),

    ('demo-santa', '圣诞贺卡', '节日贺卡', '/demo/covers/santa.svg',
     '上传两张照片，模拟生成带雪景与彩灯的圣诞贺卡。', 2, 'image/jpeg,image/png', 5242880, 30),

    ('demo-newyear', '新年祝福', '节日贺卡', '/demo/covers/newyear.svg',
     '上传一张照片，模拟生成红金配色的新年祝福卡片。', 1, 'image/jpeg,image/png', 5242880, 40),

    ('demo-wedding', '婚纱写真', '婚纱写真', '/demo/covers/wedding.svg',
     '上传两张照片，模拟生成柔光婚纱写真。支持 JPEG / PNG / WebP。', 2, 'image/jpeg,image/png,image/webp', 10485760, 50),

    ('demo-proposal', '求婚大片', '婚纱写真', '/demo/covers/proposal.svg',
     '上传三张照片，模拟生成电影感求婚场景海报。', 3, 'image/jpeg,image/png', 10485760, 60),

    ('demo-pet', '萌宠写真', '萌宠写真', '/demo/covers/pet.svg',
     '上传两张宠物照片，模拟生成影棚风格萌宠写真。', 2, 'image/jpeg,image/png', 5242880, 70),

    ('demo-pet-family', '宠物全家福', '萌宠写真', '/demo/covers/pet-family.svg',
     '最多四张宠物照片，模拟合成一张全家福。', 4, 'image/jpeg,image/png', 5242880, 80)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    category = VALUES(category),
    cover_url = VALUES(cover_url),
    description = VALUES(description),
    required_image_count = VALUES(required_image_count),
    accepted_mime_types = VALUES(accepted_mime_types),
    max_file_size_bytes = VALUES(max_file_size_bytes),
    sort_order = VALUES(sort_order);
