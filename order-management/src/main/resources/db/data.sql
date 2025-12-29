-- 初始数据 (使用 INSERT IGNORE 避免重复插入)

-- 分类
INSERT IGNORE INTO
    categories (
        id,
        name,
        parent_id,
        sort_order
    )
VALUES (1, '电子产品', 0, 1);

INSERT IGNORE INTO
    categories (
        id,
        name,
        parent_id,
        sort_order
    )
VALUES (2, '手机', 1, 1);

INSERT IGNORE INTO
    categories (
        id,
        name,
        parent_id,
        sort_order
    )
VALUES (3, '电脑', 1, 2);

INSERT IGNORE INTO
    categories (
        id,
        name,
        parent_id,
        sort_order
    )
VALUES (4, '服装', 0, 2);

INSERT IGNORE INTO
    categories (
        id,
        name,
        parent_id,
        sort_order
    )
VALUES (5, '食品', 0, 3);

-- 商品
INSERT IGNORE INTO
    products (
        id,
        category_id,
        name,
        description,
        price,
        stock,
        status
    )
VALUES (
        1,
        2,
        'iPhone 15 Pro',
        'Apple iPhone 15 Pro 256GB',
        8999.00,
        100,
        1
    );

INSERT IGNORE INTO
    products (
        id,
        category_id,
        name,
        description,
        price,
        stock,
        status
    )
VALUES (
        2,
        2,
        'Xiaomi 14',
        '小米14 骁龙8 Gen3',
        3999.00,
        200,
        1
    );

INSERT IGNORE INTO
    products (
        id,
        category_id,
        name,
        description,
        price,
        stock,
        status
    )
VALUES (
        3,
        2,
        'Huawei Mate 60',
        '华为 Mate 60 Pro',
        6999.00,
        150,
        1
    );

INSERT IGNORE INTO
    products (
        id,
        category_id,
        name,
        description,
        price,
        stock,
        status
    )
VALUES (
        4,
        3,
        'MacBook Pro 14',
        'Apple M3 Pro 芯片',
        14999.00,
        50,
        1
    );

INSERT IGNORE INTO
    products (
        id,
        category_id,
        name,
        description,
        price,
        stock,
        status
    )
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
INSERT IGNORE INTO
    users (
        id,
        username,
        password,
        phone,
        status
    )
VALUES (
        1,
        'admin',
        '123456',
        '13800138000',
        1
    );

INSERT IGNORE INTO
    users (
        id,
        username,
        password,
        phone,
        status
    )
VALUES (
        2,
        'test',
        '123456',
        '13900139000',
        1
    );

INSERT IGNORE INTO
    users (
        id,
        username,
        password,
        phone,
        status
    )
VALUES (
        3,
        'user1',
        '123456',
        '13700137000',
        1
    );