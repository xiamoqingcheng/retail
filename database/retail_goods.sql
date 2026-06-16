-- ======================================================
-- 智能零售系统 - YOLO模型对齐商品数据（完整 200 件）
-- 17个类别, goods_id = YOLO class_id + 1
-- ======================================================
USE `retail_db`;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 清空旧数据
TRUNCATE TABLE `sys_goods`;
TRUNCATE TABLE `sys_goods_category`;

-- ---------------------------
-- 1. 商品分类
-- ---------------------------
INSERT INTO `sys_goods_category` (`id`, `name`, `sort_order`) VALUES
(1,  '膨化食品', 1),
(2,  '果仁果脯', 2),
(3,  '干货', 3),
(4,  '冲调', 4),
(5,  '方便面', 5),
(6,  '饼干点心', 6),
(7,  '饮料', 7),
(8,  '酒', 8),
(9,  '奶制品', 9),
(10, '罐头', 10),
(11, '巧克力', 11),
(12, '口香糖', 12),
(13, '糖果', 13),
(14, '调味料', 14),
(15, '个人卫生', 15),
(16, '纸巾', 16),
(17, '文具', 17);

-- ---------------------------
-- 2. 商品
-- ---------------------------

-- 膨化食品 (id: 1-12, shelf: SHELF-A)
INSERT INTO `sys_goods` (`id`, `name`, `barcode`, `category_id`, `price`, `stock`, `shelf_id`, `image_url`)
VALUES
(1,  '上好佳荷兰豆55g',            '6909409012031', 1,  5.50,  80, 'SHELF-A', '/uploads/goods/001.jpg'),
(2,  '菜园小饼80g',                '6901845043112', 1,  5.50,  60, 'SHELF-A', '/uploads/goods/002.jpg'),
(3,  '上好佳鲜虾片40g',            '6909409012024', 1,  4.00,  100, 'SHELF-A', '/uploads/goods/003.jpg'),
(4,  '上好佳蟹味逸族40g',          '6926265388100', 1,  4.00,  75, 'SHELF-A', '/uploads/goods/004.jpg'),
(5,  '妙脆角魔力炭烧味65g',        '6924743920330', 1,  6.50,  45, 'SHELF-A', '/uploads/goods/005.jpg'),
(6,  '盼盼烧烤牛排味块105g',       '6920912342002', 1,  5.50,  55, 'SHELF-A', '/uploads/goods/006.jpg'),
(7,  '上好佳鲜虾条40g',            '6926265301024', 1,  4.00,  90, 'SHELF-A', '/uploads/goods/007.jpg'),
(8,  '上好佳洋葱圈40g',            '6909409040799', 1,  4.00,  80, 'SHELF-A', '/uploads/goods/008.jpg'),
(9,  '上好佳日式鱼果海苔味50g',    '6926265301130', 1,  4.50,  60, 'SHELF-A', '/uploads/goods/009.jpg'),
(10, '奇多日式牛排味90g',          '6924743913721', 1,  7.00,  50, 'SHELF-A', '/uploads/goods/010.jpg'),
(11, '奇多美式火鸡味90g',          '6924743913738', 1,  7.00,  45, 'SHELF-A', '/uploads/goods/011.jpg'),
(12, '上好佳粟米条草莓味40g',      '6909409012802', 1,  4.00,  70, 'SHELF-A', '/uploads/goods/012.jpg');

-- 果仁果脯 (id: 13-21)
INSERT INTO `sys_goods` (`id`, `name`, `barcode`, `category_id`, `price`, `stock`, `shelf_id`, `image_url`)
VALUES
(13, '甘源蟹黄味瓜子仁75g',        '6940188803618', 2,  8.50,  50, 'SHELF-B', '/uploads/goods/013.jpg'),
(14, '惠宜开心果140g',              '6907777825963', 2, 19.90,  40, 'SHELF-B', '/uploads/goods/014.jpg'),
(15, '惠宜咸味花生350g',            '6907777800519', 2,  9.90,  60, 'SHELF-B', '/uploads/goods/015.jpg'),
(16, '惠宜腰果160g',                '6907777821811', 2, 22.00,  35, 'SHELF-B', '/uploads/goods/016.jpg'),
(17, '惠宜枸杞100g',                '6907777830523', 2, 15.00,  45, 'SHELF-B', '/uploads/goods/017.jpg'),
(18, '惠宜地瓜干228g',              '6907777819061', 2, 11.50,  55, 'SHELF-B', '/uploads/goods/018.jpg'),
(19, '惠宜泰国芒果干80g',           '6907777821903', 2, 13.00,  30, 'SHELF-B', '/uploads/goods/019.jpg'),
(20, '惠宜黄桃果干75g',             '6907777834712', 2, 12.00,  40, 'SHELF-B', '/uploads/goods/020.jpg'),
(21, '惠宜柠檬片65g',               '6907777834705', 2, 10.00,  50, 'SHELF-B', '/uploads/goods/021.jpg');

-- 干货 (id: 22-30)
INSERT INTO `sys_goods` (`id`, `name`, `barcode`, `category_id`, `price`, `stock`, `shelf_id`, `image_url`)
VALUES
(22, '新疆和田滩枣454g',            '6940737300148', 3, 18.00,  40, 'SHELF-B', '/uploads/goods/022.jpg'),
(23, '惠宜香菇100g',                '6907777831995', 3, 22.00,  30, 'SHELF-B', '/uploads/goods/023.jpg'),
(24, '惠宜桂圆干500g',              '6907777800151', 3, 25.00,  25, 'SHELF-B', '/uploads/goods/024.jpg'),
(25, '惠宜茶树菇200g',              '6907777808584', 3, 15.00,  35, 'SHELF-B', '/uploads/goods/025.jpg'),
(26, '豪雄单片黑木耳150g',          '6934848931155', 3, 18.00,  30, 'SHELF-B', '/uploads/goods/026.jpg'),
(27, '惠宜煮花生454g',              '6907777825468', 3, 13.00,  40, 'SHELF-B', '/uploads/goods/027.jpg'),
(28, '惠宜黄花菜100g',              '6907777815186', 3, 16.00,  20, 'SHELF-B', '/uploads/goods/028.jpg'),
(29, '洽洽凉茶瓜子150g',            '6924187829428', 3,  8.00,  60, 'SHELF-B', '/uploads/goods/029.jpg'),
(30, '洽洽奶香味瓜子150g',          '6924187828964', 3,  8.00,  65, 'SHELF-B', '/uploads/goods/030.jpg');

-- 冲调 (id: 31-41)
INSERT INTO `sys_goods` (`id`, `name`, `barcode`, `category_id`, `price`, `stock`, `shelf_id`, `image_url`)
VALUES
(31, '车仔茶包绿茶50g',             '6913221220161', 4,  6.00,  50, 'SHELF-C', '/uploads/goods/031.jpg'),
(32, '车仔茶包红茶50g',             '6913221220109', 4,  6.00,  45, 'SHELF-C', '/uploads/goods/032.jpg'),
(33, '优乐美香芋味80g',             '6926475203170', 4,  5.00,  70, 'SHELF-C', '/uploads/goods/033.jpg'),
(34, '优乐美红豆奶茶65g',           '6926475206263', 4,  5.00,  65, 'SHELF-C', '/uploads/goods/034.jpg'),
(35, '欢泥冲调土豆粥25g',           '6959619480205', 4,  4.50,  40, 'SHELF-C', '/uploads/goods/035.jpg'),
(36, '江中猴姑早餐米稀40g',         '6939947700169', 4, 12.00,  30, 'SHELF-C', '/uploads/goods/036.jpg'),
(37, '永和豆浆甜豆浆粉210g',        '6950361040808', 4, 15.00,  25, 'SHELF-C', '/uploads/goods/037.jpg'),
(38, '立顿柠檬风味茶180g',          '6922848642133', 4, 12.00,  30, 'SHELF-C', '/uploads/goods/038.jpg'),
(39, '桂格多种莓果麦片40g',         '6924743921436', 4,  9.00,  45, 'SHELF-C', '/uploads/goods/039.jpg'),
(40, '荣怡谷麦加黑米味30g',         '6953787800124', 4,  5.00,  55, 'SHELF-C', '/uploads/goods/040.jpg'),
(41, '荣怡谷麦加红豆味30g',         '6953787800117', 4,  5.00,  50, 'SHELF-C', '/uploads/goods/041.jpg');

-- 方便面 (id: 42-53)
INSERT INTO `sys_goods` (`id`, `name`, `barcode`, `category_id`, `price`, `stock`, `shelf_id`, `image_url`)
VALUES
(42, '今野香辣牛肉面112g',          '6921555581674', 5,  4.00,  100, 'SHELF-C', '/uploads/goods/042.jpg'),
(43, '今野老坛酸菜牛肉面118g',      '6921555510568', 5,  4.00,  100, 'SHELF-C', '/uploads/goods/043.jpg'),
(44, '今野红烧牛肉面114g',          '6921555581667', 5,  4.00,  100, 'SHELF-C', '/uploads/goods/044.jpg'),
(45, '合味道海鲜风味84g',            '6917536014026', 5,  6.50,  50, 'SHELF-C', '/uploads/goods/045.jpg'),
(46, '康师傅白胡椒肉骨面76g',       '6920152493915', 5,  5.50,  80, 'SHELF-C', '/uploads/goods/046.jpg'),
(47, '康师傅香辣牛肉面105g',        '6920152400975', 5,  5.00,  90, 'SHELF-C', '/uploads/goods/047.jpg'),
(48, '康师傅香辣蒜味排骨面108g',    '6920152496176', 5,  5.50,  60, 'SHELF-C', '/uploads/goods/048.jpg'),
(49, '康师傅藤椒牛肉面82g',         '6920152497029', 5,  5.00,  70, 'SHELF-C', '/uploads/goods/049.jpg'),
(50, '华丰鸡肉三鲜伊面87g',         '6901715291209', 5,  3.50,  90, 'SHELF-C', '/uploads/goods/050.jpg'),
(51, '康师傅黑胡椒牛排面104g',      '6920152485095', 5,  5.50,  75, 'SHELF-C', '/uploads/goods/051.jpg'),
(52, '五谷道场红烧牛肉面100g',      '6936986841044', 5,  5.00,  65, 'SHELF-C', '/uploads/goods/052.jpg'),
(53, '康师傅老坛酸菜牛肉面114g',    '6920152439005', 5,  5.00,  85, 'SHELF-C', '/uploads/goods/053.jpg');

-- 饼干点心 (id: 54-70)
INSERT INTO `sys_goods` (`id`, `name`, `barcode`, `category_id`, `price`, `stock`, `shelf_id`, `image_url`)
VALUES
(54, 'Aji泡芙饼干芒果菠萝味60g',    '4894375013507', 6,  8.00,  55, 'SHELF-D', '/uploads/goods/054.jpg'),
(55, '庆联蓝莓味夹心饼63g',         '6922907011535', 6,  7.00,  50, 'SHELF-D', '/uploads/goods/055.jpg'),
(56, '庆联凤梨味夹心饼63g',         '6922907011528', 6,  7.00,  50, 'SHELF-D', '/uploads/goods/056.jpg'),
(57, '庆联草莓味夹心饼63g',         '6922907011511', 6,  7.00,  50, 'SHELF-D', '/uploads/goods/057.jpg'),
(58, '嘉顿威化饼干草莓味50g',       '6902227014829', 6,  9.00,  45, 'SHELF-D', '/uploads/goods/058.jpg'),
(59, '嘉顿威化饼干柠檬味50g',       '6902227014843', 6,  9.00,  45, 'SHELF-D', '/uploads/goods/059.jpg'),
(60, '爱时乐香草牛奶味50g',         '6953042700220', 6,  8.00,  40, 'SHELF-D', '/uploads/goods/060.jpg'),
(61, '爱时乐巧克力味50g',           '6953042700206', 6,  8.00,  40, 'SHELF-D', '/uploads/goods/061.jpg'),
(62, '百力滋海苔味60g',             '6901845042993', 6,  7.50,  50, 'SHELF-D', '/uploads/goods/062.jpg'),
(63, '百力滋草莓牛奶味45g',         '6901845042627', 6,  7.50,  50, 'SHELF-D', '/uploads/goods/063.jpg'),
(64, '雀巢脆脆鲨80g',               '6917878035284', 6, 10.00,  40, 'SHELF-D', '/uploads/goods/064.jpg'),
(65, '纳宝帝巧克力味威化58g',       '8993175540629', 6,  9.00,  35, 'SHELF-D', '/uploads/goods/065.jpg'),
(66, '桂力地中海风味面包条50g',     '8411145202563', 6,  8.50,  35, 'SHELF-D', '/uploads/goods/066.jpg'),
(67, '康师傅妙芙巧克力味48g',       '6920731700205', 6,  6.00,  60, 'SHELF-D', '/uploads/goods/067.jpg'),
(68, '爱乡亲唱片面包90g',           '6956367187172', 6,  6.00,  55, 'SHELF-D', '/uploads/goods/068.jpg'),
(69, '达利园派草莓味单个装',        '6911988005397', 6,  3.00,  100, 'SHELF-D', '/uploads/goods/069.jpg'),
(70, 'mini奥利奥55g',               '6901668054715', 6,  7.00,  50, 'SHELF-D', '/uploads/goods/070.jpg');

-- 饮料 (id: 71-87)
INSERT INTO `sys_goods` (`id`, `name`, `barcode`, `category_id`, `price`, `stock`, `shelf_id`, `image_url`)
VALUES
(71, '农夫山泉矿泉水550ml',         '6921168509256', 7,  2.00,  200, 'SHELF-E', '/uploads/goods/071.jpg'),
(72, '怡宝矿泉水555ml',             '6901285991219', 7,  2.00,  200, 'SHELF-E', '/uploads/goods/072.jpg'),
(73, '可口可乐零度500ml',           '6928804010114', 7,  3.50,  130, 'SHELF-E', '/uploads/goods/073.jpg'),
(74, '可口可乐500ml',               '6928804011173', 7,  3.50,  150, 'SHELF-E', '/uploads/goods/074.jpg'),
(75, '百事可乐600ml',               '6924882496116', 7,  3.50,  150, 'SHELF-E', '/uploads/goods/075.jpg'),
(76, '芬达苹果味500ml',             '6928804011456', 7,  3.50,  120, 'SHELF-E', '/uploads/goods/076.jpg'),
(77, '芬达橙味500ml',               '6928804011326', 7,  3.50,  130, 'SHELF-E', '/uploads/goods/077.jpg'),
(78, '雪碧500ml',                   '6928804010220', 7,  3.50,  150, 'SHELF-E', '/uploads/goods/078.jpg'),
(81, '百事可乐330ml',               '6924882486100', 7,  2.50,  120, 'SHELF-E', '/uploads/goods/081.jpg'),
(82, '可口可乐330ml',               '6928804011142', 7,  2.50,  120, 'SHELF-E', '/uploads/goods/082.jpg'),
(83, '王老吉310ml',                 '6956367338680', 7,  5.00,  100, 'SHELF-E', '/uploads/goods/083.jpg'),
(84, '茶派柚子绿茶500ml',           '6921168593576', 7,  4.50,  80, 'SHELF-E', '/uploads/goods/084.jpg'),
(85, '茶派玫瑰荔枝红茶500ml',       '6921168593736', 7,  4.50,  80, 'SHELF-E', '/uploads/goods/085.jpg'),
(86, '康师傅冰红茶250ml',           '6920459902387', 7,  3.00,  140, 'SHELF-E', '/uploads/goods/086.jpg'),
(87, '加多宝250ml',                 '4891599601138', 7,  5.00,  90, 'SHELF-E', '/uploads/goods/087.jpg');

-- 酒 (id: 79-96)
INSERT INTO `sys_goods` (`id`, `name`, `barcode`, `category_id`, `price`, `stock`, `shelf_id`, `image_url`)
VALUES
(79, '喜力啤酒500ml',               '',                8,  8.00,  80, 'SHELF-F', '/uploads/goods/079.jpg'),
(80, '百威啤酒600ml',               '',                8,  8.50,  70, 'SHELF-F', '/uploads/goods/080.jpg'),
(88, 'RIO果酒水蜜桃味275ml',        '',                8, 12.00,  45, 'SHELF-F', '/uploads/goods/088.jpg'),
(89, 'RIO果酒蓝玫瑰威士忌味275ml',  '6935145301047',   8, 12.00,  50, 'SHELF-F', '/uploads/goods/089.jpg'),
(90, '牛栏山二锅头100ml',           '6906151601353',   8,  6.50,  55, 'SHELF-F', '/uploads/goods/090.jpg'),
(91, '哈尔滨啤酒330ml',             '6948960100429',   8,  5.00,  100, 'SHELF-F', '/uploads/goods/091.jpg'),
(92, '青岛啤酒330ml',               '6901035613699',   8,  5.50,  100, 'SHELF-F', '/uploads/goods/092.jpg'),
(93, '雪花啤酒330ml',               '6949352201106',   8,  5.00,  90, 'SHELF-F', '/uploads/goods/093.jpg'),
(94, '哈尔滨啤酒500ml',             '6948960100993',   8,  7.00,  60, 'SHELF-F', '/uploads/goods/094.jpg'),
(95, 'KELER啤酒500ml',              '8410793186126',   8,  9.00,  40, 'SHELF-F', '/uploads/goods/095.jpg'),
(96, '百威啤酒500ml',               '6948960100078',   8,  8.00,  75, 'SHELF-F', '/uploads/goods/096.jpg');

-- 奶制品 (id: 97-107)
INSERT INTO `sys_goods` (`id`, `name`, `barcode`, `category_id`, `price`, `stock`, `shelf_id`, `image_url`)
VALUES
(97, 'QQ星全聪奶125ml',             '6907992510446',   9,  4.00,  70, 'SHELF-E', '/uploads/goods/097.jpg'),
(98, 'QQ星均膳奶125ml',             '6907992511559',   9,  4.00,  80, 'SHELF-E', '/uploads/goods/098.jpg'),
(99, '娃哈哈AD钙奶220g',            '6902083881085',   9,  3.50,  100, 'SHELF-E', '/uploads/goods/099.jpg'),
(100,'活力宝动力源105ml',           '6959791800068',   9,  5.00,  60, 'SHELF-E', '/uploads/goods/100.jpg'),
(101,'旺仔牛奶复原乳250ml',         '6931958014105',   9,  6.00,  90, 'SHELF-E', '/uploads/goods/101.jpg'),
(102,'伊利纯牛奶250ml',             '6907992100272',   9,  3.50,  110, 'SHELF-E', '/uploads/goods/102.jpg'),
(103,'维他低糖原味豆奶250ml',       '4891028707851',   9,  5.50,  70, 'SHELF-E', '/uploads/goods/103.jpg'),
(104,'百怡花生牛奶250ml',           '6941543400251',   9,  4.00,  65, 'SHELF-E', '/uploads/goods/104.jpg'),
(105,'惠宜原味豆奶250ml',           '6907777822948',   9,  3.50,  75, 'SHELF-E', '/uploads/goods/105.jpg'),
(106,'伊利优酸乳250ml',             '6907992500010',   9,  3.50,  120, 'SHELF-E', '/uploads/goods/106.jpg'),
(107,'伊利早餐奶250ml',             '6907992504476',   9,  4.50,  80, 'SHELF-E', '/uploads/goods/107.jpg');

-- 罐头 (id: 108-121)
INSERT INTO `sys_goods` (`id`, `name`, `barcode`, `category_id`, `price`, `stock`, `shelf_id`, `image_url`)
VALUES
(108,'达利园桂圆莲子360g',          '6911988011985',   10, 12.00,  45, 'SHELF-F', '/uploads/goods/108.jpg'),
(109,'银鹭冰糖百合银耳280g',        '6926892562096',   10, 10.00,  50, 'SHELF-F', '/uploads/goods/109.jpg'),
(110,'喜多多什锦椰果567g',          '6923523998019',   10, 10.00,  40, 'SHELF-F', '/uploads/goods/110.jpg'),
(111,'都乐菠萝块567g',              '4800009004827',   10, 13.00,  30, 'SHELF-F', '/uploads/goods/111.jpg'),
(112,'都乐菠萝块234g',              '38900004095',     10,  7.00,  45, 'SHELF-F', '/uploads/goods/112.jpg'),
(113,'银鹭薏仁红豆粥280g',          '6926892567084',   10,  8.50,  55, 'SHELF-F', '/uploads/goods/113.jpg'),
(114,'银鹭莲子玉米粥280g',          '6926892565080',   10,  8.50,  45, 'SHELF-F', '/uploads/goods/114.jpg'),
(115,'银鹭紫薯紫米粥280g',          '6926892501033',   10,  8.50,  50, 'SHELF-F', '/uploads/goods/115.jpg'),
(116,'银鹭椰奶燕麦粥280g',          '6926892568081',   10,  8.50,  50, 'SHELF-F', '/uploads/goods/116.jpg'),
(117,'银鹭黑糖桂圆280g',            '6926892562003',   10,  9.00,  45, 'SHELF-F', '/uploads/goods/117.jpg'),
(118,'梅林午餐肉340g',              '6902131110167',   10, 15.00,  25, 'SHELF-F', '/uploads/goods/118.jpg'),
(119,'珠江桥牌豆豉鱼150g',          '6916880292012',   10, 14.00,  30, 'SHELF-F', '/uploads/goods/119.jpg'),
(120,'古龙原味黄花鱼120g',          '6901073808347',   10, 12.00,  30, 'SHELF-F', '/uploads/goods/120.jpg'),
(121,'雄鸡标椰浆140ml',             '9556041603720',   10,  8.00,  40, 'SHELF-F', '/uploads/goods/121.jpg');

-- 巧克力 (id: 122-133)
INSERT INTO `sys_goods` (`id`, `name`, `barcode`, `category_id`, `price`, `stock`, `shelf_id`, `image_url`)
VALUES
(122,'德芙芒果酸奶巧克力42g',       '6914973608351',   11, 12.00,  50, 'SHELF-D', '/uploads/goods/122.jpg'),
(123,'德芙摩卡巴旦木巧克力43g',     '6914973604056',   11, 12.00,  50, 'SHELF-D', '/uploads/goods/123.jpg'),
(124,'德芙百香果白巧克力42g',       '6914973608306',   11, 12.00,  45, 'SHELF-D', '/uploads/goods/124.jpg'),
(125,'MM花生牛奶巧克力豆40g',        '6914973105386',   11, 10.00,  60, 'SHELF-D', '/uploads/goods/125.jpg'),
(126,'MM牛奶巧克力豆40g',            '6914973105379',   11, 10.00,  60, 'SHELF-D', '/uploads/goods/126.jpg'),
(127,'好时牛奶巧克力40g',            '6942836705916',   11, 13.00,  40, 'SHELF-D', '/uploads/goods/127.jpg'),
(128,'好时曲奇奶香白巧克力40g',      '6942836705435',   11, 13.00,  40, 'SHELF-D', '/uploads/goods/128.jpg'),
(129,'脆香米海苔白巧克力24g',       '6914973607101',   11,  8.00,  70, 'SHELF-D', '/uploads/goods/129.jpg'),
(130,'脆香米奶香白巧克力24g',       '6914973604469',   11,  8.00,  60, 'SHELF-D', '/uploads/goods/130.jpg'),
(131,'士力架花生夹心巧克力51g',     '6914973603394',   11,  7.00,  80, 'SHELF-D', '/uploads/goods/131.jpg'),
(132,'士力架燕麦花生夹心巧克力40g', '6914973607125',   11,  7.00,  65, 'SHELF-D', '/uploads/goods/132.jpg'),
(133,'士力架辣花生夹心巧克力40g',   '6914973607637',   11,  9.00,  55, 'SHELF-D', '/uploads/goods/133.jpg');

-- 口香糖 (id: 134-141)
INSERT INTO `sys_goods` (`id`, `name`, `barcode`, `category_id`, `price`, `stock`, `shelf_id`, `image_url`)
VALUES
(134,'炫迈果味浪薄荷味37g',         '6924513908216',   12, 10.00,  80, 'SHELF-A', '/uploads/goods/134.jpg'),
(135,'炫迈果味浪柠檬味37g',         '6924513908155',   12, 10.00,  75, 'SHELF-A', '/uploads/goods/135.jpg'),
(136,'炫迈薄荷味21g',               '6954432710218',   12,  6.00,  100, 'SHELF-A', '/uploads/goods/136.jpg'),
(137,'炫迈葡萄味21g',               '6954432710621',   12,  6.00,  95, 'SHELF-A', '/uploads/goods/137.jpg'),
(138,'炫迈西瓜味21g',               '6954432710249',   12,  6.00,  90, 'SHELF-A', '/uploads/goods/138.jpg'),
(139,'炫迈葡萄味50g',               '6954432710645',   12, 12.00,  60, 'SHELF-A', '/uploads/goods/139.jpg'),
(140,'绿箭无糖薄荷糖茉莉花茶味34g', '6923450605981',   12,  8.00,  70, 'SHELF-A', '/uploads/goods/140.jpg'),
(141,'绿箭5片装15g',                '69019388',         12,  3.00,  120, 'SHELF-A', '/uploads/goods/141.jpg');

-- 糖果 (id: 142-151)
INSERT INTO `sys_goods` (`id`, `name`, `barcode`, `category_id`, `price`, `stock`, `shelf_id`, `image_url`)
VALUES
(142,'比巴卜棉花泡泡糖可乐味11g',   '6911316101043',   13,  3.00,  120, 'SHELF-A', '/uploads/goods/142.jpg'),
(143,'比巴卜棉花泡泡堂葡萄味11g',   '6911316101012',   13,  3.00,  120, 'SHELF-A', '/uploads/goods/143.jpg'),
(144,'星爆缤纷原果味25g',           '6923450663981',   13,  5.00,  90, 'SHELF-A', '/uploads/goods/144.jpg'),
(145,'阿尔卑斯焦香牛奶味硬糖45g',   '6911316510005',   13,  6.00,  80, 'SHELF-A', '/uploads/goods/145.jpg'),
(146,'阿尔卑斯牛奶软糖黄桃酸奶味47g','6911316380288',  13,  6.00,  70, 'SHELF-A', '/uploads/goods/146.jpg'),
(147,'阿尔卑斯牛奶软糖蓝莓酸奶味47g','6911316380271',  13,  6.00,  70, 'SHELF-A', '/uploads/goods/147.jpg'),
(148,'王老吉润喉糖28g',             '6901424286213',   13,  8.00,  60, 'SHELF-A', '/uploads/goods/148.jpg'),
(149,'伊利牛奶片蓝莓味32g',         '6907992632483',   13,  5.00,  70, 'SHELF-A', '/uploads/goods/149.jpg'),
(150,'熊博士口嚼糖草莓牛奶味52g',   '6914782114371',   13,  9.00,  55, 'SHELF-A', '/uploads/goods/150.jpg'),
(151,'彩虹糖原果味45g',             '6923450603550',   13,  7.00,  85, 'SHELF-A', '/uploads/goods/151.jpg');

-- 调味料 (id: 152-163)
INSERT INTO `sys_goods` (`id`, `name`, `barcode`, `category_id`, `price`, `stock`, `shelf_id`, `image_url`)
VALUES
(152,'宝鼎天鱼陈酿米醋245ml',       '6932107253215',   14,  5.00,  45, 'SHELF-G', '/uploads/goods/152.jpg'),
(153,'恒顺香醋340ml',               '6902007030087',   14,  6.00,  50, 'SHELF-G', '/uploads/goods/153.jpg'),
(154,'太太乐鸡精200g',              '6922130119954',   14,  8.00,  45, 'SHELF-G', '/uploads/goods/154.jpg'),
(155,'家乐香菇鸡茸汤料41g',         '6913102210618',   14,  5.00,  60, 'SHELF-G', '/uploads/goods/155.jpg'),
(156,'惠宜辣椒粉15g',               '6907777820708',   14,  4.00,  70, 'SHELF-G', '/uploads/goods/156.jpg'),
(157,'惠宜生姜粉15g',               '6907777820722',   14,  4.00,  65, 'SHELF-G', '/uploads/goods/157.jpg'),
(158,'味好美椒盐20g',               '6901844710114',   14,  5.00,  55, 'SHELF-G', '/uploads/goods/158.jpg'),
(159,'海星加碘精制盐400g',          '6920181360936',   14,  2.50,  80, 'SHELF-G', '/uploads/goods/159.jpg'),
(160,'恒顺料酒500ml',               '6930096350922',   14,  7.00,  40, 'SHELF-G', '/uploads/goods/160.jpg'),
(161,'东古味极鲜酱油150ml',         '6911567886393',   14,  6.50,  50, 'SHELF-G', '/uploads/goods/161.jpg'),
(162,'东古一品鲜酱油150ml',         '6911567881060',   14,  7.00,  45, 'SHELF-G', '/uploads/goods/162.jpg'),
(163,'欣和六月鲜酱油160ml',         '6925843403303',   14,  8.00,  40, 'SHELF-G', '/uploads/goods/163.jpg');

-- 个人卫生 (id: 164-173)
INSERT INTO `sys_goods` (`id`, `name`, `barcode`, `category_id`, `price`, `stock`, `shelf_id`, `image_url`)
VALUES
(164,'李施德林零度漱口水80ml',      '6907376820598',   15, 15.00,  35, 'SHELF-G', '/uploads/goods/164.jpg'),
(165,'舒肤佳纯白清香沐浴露100ml',   '6903148232965',   15, 12.00,  35, 'SHELF-G', '/uploads/goods/165.jpg'),
(166,'美涛定型啫喱水60ml',          '6924882349016',   15, 10.00,  40, 'SHELF-G', '/uploads/goods/166.jpg'),
(167,'清扬男士洗发露薄荷型50ml',    '6902088119435',   15, 12.00,  40, 'SHELF-G', '/uploads/goods/167.jpg'),
(168,'蓝月亮风清白兰洗衣液80g',     '6902022138102',   15,  8.00,  50, 'SHELF-G', '/uploads/goods/168.jpg'),
(169,'高露洁亮白小苏打180g',        '6920354818585',   15, 18.00,  30, 'SHELF-G', '/uploads/goods/169.jpg'),
(170,'高露洁冰爽180g',              '6920354808388',   15, 18.00,  30, 'SHELF-G', '/uploads/goods/170.jpg'),
(171,'舒亮皓齿白80g',               '6921469850194',   15, 10.00,  45, 'SHELF-G', '/uploads/goods/171.jpg'),
(172,'云南白药牙膏45g',              '6901070600128',   15, 22.00,  25, 'SHELF-G', '/uploads/goods/172.jpg'),
(173,'舒克宝贝儿童牙刷',            '6940477401396',   15,  8.00,  45, 'SHELF-G', '/uploads/goods/173.jpg');

-- 纸巾 (id: 174-193)
INSERT INTO `sys_goods` (`id`, `name`, `barcode`, `category_id`, `price`, `stock`, `shelf_id`, `image_url`)
VALUES
(174,'清风原木纯品金装100x3',       '6922266452949',   16, 12.00,  60, 'SHELF-G', '/uploads/goods/174.jpg'),
(175,'洁柔face150x3',               '6914068016115',   16, 10.00,  70, 'SHELF-G', '/uploads/goods/175.jpg'),
(176,'斑布100x3',                   '6953631801604',   16, 15.00,  45, 'SHELF-G', '/uploads/goods/176.jpg'),
(177,'维达婴儿150x3',               '6901236378823',   16, 16.00,  40, 'SHELF-G', '/uploads/goods/177.jpg'),
(178,'相印小黄人150x3',             '6922868286249',   16, 11.00,  65, 'SHELF-G', '/uploads/goods/178.jpg'),
(179,'清风原木纯品黑耀150x3',       '6922266457616',   16, 13.00,  55, 'SHELF-G', '/uploads/goods/179.jpg'),
(180,'洁云绒触感130x3',             '6918717002160',   16,  9.00,  75, 'SHELF-G', '/uploads/goods/180.jpg'),
(181,'舒洁萌印花120x2',             '6923589421131',   16,  8.00,  80, 'SHELF-G', '/uploads/goods/181.jpg'),
(182,'相印红悦130x3',               '6903244670166',   16, 14.00,  50, 'SHELF-G', '/uploads/goods/182.jpg'),
(183,'得宝苹果木味90x4',            '6947509910727',   16, 20.00,  30, 'SHELF-G', '/uploads/goods/183.jpg'),
(184,'清风新韧纯品130x3',           '6922266449611',   16, 11.00,  60, 'SHELF-G', '/uploads/goods/184.jpg'),
(185,'金鱼竹浆绿135x3',             '6951481302241',   16, 12.00,  55, 'SHELF-G', '/uploads/goods/185.jpg'),
(186,'清风原木纯品150x2',           '6922266444463',   16,  9.00,  70, 'SHELF-G', '/uploads/goods/186.jpg'),
(187,'洁柔face130x3',               '6914068018171',   16, 10.00,  65, 'SHELF-G', '/uploads/goods/187.jpg'),
(188,'维达立体美110x3',             '6901236344033',   16, 17.00,  35, 'SHELF-G', '/uploads/goods/188.jpg'),
(189,'洁柔CS单包',                  '6914068016535',   16,  4.00,  100, 'SHELF-G', '/uploads/goods/189.jpg'),
(190,'相印小黄人单包',              '6922868282265',   16,  4.00,  100, 'SHELF-G', '/uploads/goods/190.jpg'),
(191,'清风原色单包',                '6922266457425',   16,  3.50,  110, 'SHELF-G', '/uploads/goods/191.jpg'),
(192,'相印茶语单包',                '6922868290932',   16,  3.50,  110, 'SHELF-G', '/uploads/goods/192.jpg'),
(193,'清风质感纯品单包',            '6922266426001',   16,  3.00,  120, 'SHELF-G', '/uploads/goods/193.jpg');

-- 文具 (id: 194-200)
INSERT INTO `sys_goods` (`id`, `name`, `barcode`, `category_id`, `price`, `stock`, `shelf_id`, `image_url`)
VALUES
(194,'米奇1928笔记本',              '6901687353417',   17,  5.00,  60, 'SHELF-G', '/uploads/goods/194.jpg'),
(195,'广博固体胶15g',               '6930114504085',   17,  3.00,  80, 'SHELF-G', '/uploads/goods/195.jpg'),
(196,'票据文件袋',                  '6951384903194',   17,  2.00,  100, 'SHELF-G', '/uploads/goods/196.jpg'),
(197,'晨光蜗牛改正带',              '6933631504811',   17,  5.00,  55, 'SHELF-G', '/uploads/goods/197.jpg'),
(198,'鸿泰液体胶50g',               '6933093050208',   17,  4.00,  70, 'SHELF-G', '/uploads/goods/198.jpg'),
(199,'马培德自粘性标签',            '6939789303252',   17,  3.50,  90, 'SHELF-G', '/uploads/goods/199.jpg'),
(200,'东亚记号笔',                  '6925792550042',   17,  2.50,  100, 'SHELF-G', '/uploads/goods/200.jpg');

SET FOREIGN_KEY_CHECKS = 1;
