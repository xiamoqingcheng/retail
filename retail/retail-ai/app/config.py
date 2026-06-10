"""全局配置常量。"""

IMAGE_HEIGHT = 1080
IMAGE_WIDTH = 1920

# 摄像头流参数
STREAM_JPEG_QUALITY = 80
STREAM_FPS_LIMIT = 30
STREAM_IDLE_TIMEOUT = 60  # 空闲超时秒数，超过后自动释放摄像头

# 商品目录 — 与 best.pt YOLO 模型 200 类对齐
# class_id -> (goods_id, category_name)
GOODS_CATALOG = [
    (1, "puffed_food"), (2, "puffed_food"), (3, "puffed_food"), (4, "puffed_food"),
    (5, "puffed_food"), (6, "puffed_food"), (7, "puffed_food"), (8, "puffed_food"),
    (9, "puffed_food"), (10, "puffed_food"), (11, "puffed_food"), (12, "puffed_food"),
    (13, "dried_fruit"), (14, "dried_fruit"), (15, "dried_fruit"), (16, "dried_fruit"),
    (17, "dried_fruit"), (18, "dried_fruit"), (19, "dried_fruit"), (20, "dried_fruit"),
    (21, "dried_fruit"),
    (22, "dried_food"), (23, "dried_food"), (24, "dried_food"), (25, "dried_food"),
    (26, "dried_food"), (27, "dried_food"), (28, "dried_food"), (29, "dried_food"),
    (30, "dried_food"),
    (31, "instant_drink"), (32, "instant_drink"), (33, "instant_drink"), (34, "instant_drink"),
    (35, "instant_drink"), (36, "instant_drink"), (37, "instant_drink"), (38, "instant_drink"),
    (39, "instant_drink"), (40, "instant_drink"), (41, "instant_drink"),
    (42, "instant_noodles"), (43, "instant_noodles"), (44, "instant_noodles"),
    (45, "instant_noodles"), (46, "instant_noodles"), (47, "instant_noodles"),
    (48, "instant_noodles"), (49, "instant_noodles"), (50, "instant_noodles"),
    (51, "instant_noodles"), (52, "instant_noodles"), (53, "instant_noodles"),
    (54, "dessert"), (55, "dessert"), (56, "dessert"), (57, "dessert"), (58, "dessert"),
    (59, "dessert"), (60, "dessert"), (61, "dessert"), (62, "dessert"), (63, "dessert"),
    (64, "dessert"), (65, "dessert"), (66, "dessert"), (67, "dessert"), (68, "dessert"),
    (69, "dessert"), (70, "dessert"),
    (71, "drink"), (72, "drink"), (73, "drink"), (74, "drink"), (75, "drink"),
    (76, "drink"), (77, "drink"), (78, "drink"), (79, "alcohol"), (80, "alcohol"),
    (81, "drink"), (82, "drink"), (83, "drink"), (84, "drink"), (85, "drink"),
    (86, "drink"), (87, "drink"), (88, "alcohol"), (89, "alcohol"), (90, "alcohol"),
    (91, "alcohol"), (92, "alcohol"), (93, "alcohol"), (94, "alcohol"), (95, "alcohol"),
    (96, "alcohol"),
    (97, "milk"), (98, "milk"), (99, "milk"), (100, "milk"), (101, "milk"),
    (102, "milk"), (103, "milk"), (104, "milk"), (105, "milk"), (106, "milk"),
    (107, "milk"),
    (108, "canned_food"), (109, "canned_food"), (110, "canned_food"), (111, "canned_food"),
    (112, "canned_food"), (113, "canned_food"), (114, "canned_food"), (115, "canned_food"),
    (116, "canned_food"), (117, "canned_food"), (118, "canned_food"), (119, "canned_food"),
    (120, "canned_food"), (121, "canned_food"),
    (122, "chocolate"), (123, "chocolate"), (124, "chocolate"), (125, "chocolate"),
    (126, "chocolate"), (127, "chocolate"), (128, "chocolate"), (129, "chocolate"),
    (130, "chocolate"), (131, "chocolate"), (132, "chocolate"), (133, "chocolate"),
    (134, "gum"), (135, "gum"), (136, "gum"), (137, "gum"), (138, "gum"),
    (139, "gum"), (140, "gum"), (141, "gum"),
    (142, "candy"), (143, "candy"), (144, "candy"), (145, "candy"), (146, "candy"),
    (147, "candy"), (148, "candy"), (149, "candy"), (150, "candy"), (151, "candy"),
    (152, "seasoner"), (153, "seasoner"), (154, "seasoner"), (155, "seasoner"),
    (156, "seasoner"), (157, "seasoner"), (158, "seasoner"), (159, "seasoner"),
    (160, "seasoner"), (161, "seasoner"), (162, "seasoner"), (163, "seasoner"),
    (164, "personal_hygiene"), (165, "personal_hygiene"), (166, "personal_hygiene"),
    (167, "personal_hygiene"), (168, "personal_hygiene"), (169, "personal_hygiene"),
    (170, "personal_hygiene"), (171, "personal_hygiene"), (172, "personal_hygiene"),
    (173, "personal_hygiene"),
    (174, "tissue"), (175, "tissue"), (176, "tissue"), (177, "tissue"), (178, "tissue"),
    (179, "tissue"), (180, "tissue"), (181, "tissue"), (182, "tissue"), (183, "tissue"),
    (184, "tissue"), (185, "tissue"), (186, "tissue"), (187, "tissue"), (188, "tissue"),
    (189, "tissue"), (190, "tissue"), (191, "tissue"), (192, "tissue"), (193, "tissue"),
    (194, "stationery"), (195, "stationery"), (196, "stationery"), (197, "stationery"),
    (198, "stationery"), (199, "stationery"), (200, "stationery"),
]

# goods_id -> category_name 快速查找映射
GOODS_NAME_MAP = {goods_id: name for goods_id, name in GOODS_CATALOG}
