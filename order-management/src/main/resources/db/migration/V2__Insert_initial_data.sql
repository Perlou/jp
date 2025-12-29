-- ========================================
-- V2__Insert_initial_data.sql
-- 插入初始测试数据
-- ========================================

-- 分类数据
MERGE INTO categories (
    id,
    name,
    parent_id,
    sort_order
) KEY (id)
VALUES (1, '电子产品', 0, 1);

MERGE INTO categories (
    id,
    name,
    parent_id,
    sort_order
) KEY (id)
VALUES (2, '手机', 1, 1);

MERGE INTO categories (
    id,
    name,
    parent_id,
    sort_order
) KEY (id)
VALUES (3, '电脑', 1, 2);

MERGE INTO categories (
    id,
    name,
    parent_id,
    sort_order
) KEY (id)
VALUES (4, '服装', 0, 2);

MERGE INTO categories (
    id,
    name,
    parent_id,
    sort_order
) KEY (id)
VALUES (5, '食品', 0, 3);

-- 商品数据
MERGE INTO products (
    id,
    category_id,
    name,
    description,
    price,
    stock,
    status
) KEY (id)
VALUES (
        1,
        2,
        'iPhone 15 Pro',
        'Apple iPhone 15 Pro 256GB',
        8999.00,
        100,
        1
    );

MERGE INTO products (
    id,
    category_id,
    name,
    description,
    price,
    stock,
    status
) KEY (id)
VALUES (
        2,
        2,
        'Xiaomi 14',
        '小米14 骁龙8 Gen3',
        3999.00,
        200,
        1
    );

MERGE INTO products (
    id,
    category_id,
    name,
    description,
    price,
    stock,
    status
) KEY (id)
VALUES (
        3,
        2,
        'Huawei Mate 60',
        '华为 Mate 60 Pro',
        6999.00,
        150,
        1
    );

MERGE INTO products (
    id,
    category_id,
    name,
    description,
    price,
    stock,
    status
) KEY (id)
VALUES (
        4,
        3,
        'MacBook Pro 14',
        'Apple M3 Pro 芯片',
        14999.00,
        50,
        1
    );

MERGE INTO products (
    id,
    category_id,
    name,
    description,
    price,
    stock,
    status
) KEY (id)
VALUES (
        5,
        3,
        'ThinkPad X1 Carbon',
        '联想商务笔记本',
        9999.00,
        80,
        1
    );

-- 测试用户
MERGE INTO users (
    id,
    username,
    password,
    phone,
    status
) KEY (id)
VALUES (
        1,
        'admin',
        '123456',
        '13800138000',
        1
    );

MERGE INTO users (
    id,
    username,
    password,
    phone,
    status
) KEY (id)
VALUES (
        2,
        'test',
        '123456',
        '13900139000',
        1
    );

MERGE INTO users (
    id,
    username,
    password,
    phone,
    status
) KEY (id)
VALUES (
        3,
        'user1',
        '123456',
        '13700137000',
        1
    );