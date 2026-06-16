package com.retail.server.recommendation.semantic;

import com.retail.server.recommendation.model.GoodsSearchDocument;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GoodsSemanticVectorizer {

    private static final Map<String, List<String>> CONCEPT_ALIASES = new LinkedHashMap<>();

    /**
     * 商品向量缓存：vectorizeGoods 的结果只取决于商品的名称/类目（与查询无关），
     * 而搜索时会对全量商品逐个调用，故缓存确定性结果，避免每次搜索重算别名/n-gram/CJK 向量。
     * key 由 名称|类目名|类目ID 组成；商品资料变更时 key 变化自然失效。缓存的向量只读，可并发共享。
     */
    private final Map<String, TextVector> goodsVectorCache = new ConcurrentHashMap<>();
    private static final List<QueryIntent> QUERY_INTENTS = List.of(
            new QueryIntent(
                    List.of("水", "喝水", "饮用水", "瓶装水", "矿泉水", "纯净水"),
                    List.of("beverage")),
            new QueryIntent(
                    List.of("饮料", "饮品", "喝的", "口渴", "汽水", "茶饮", "果汁"),
                    List.of("beverage", "instant_drink", "dairy")),
            new QueryIntent(
                    List.of("奶茶"),
                    List.of("instant_drink", "dairy", "beverage")),
            new QueryIntent(
                    List.of("零食", "休闲食品", "小吃", "小食"),
                    List.of("snack", "puffed_snack", "nuts_fruit", "biscuit_pastry",
                            "chocolate", "gum", "candy", "instant_noodle"))
    );

    static {
        CONCEPT_ALIASES.put("beverage", List.of(
                "饮料", "饮品", "汽水", "碳酸", "可乐", "雪碧", "芬达", "百事", "可口",
                "矿泉水", "纯净水", "饮用水", "瓶装水", "苏打水", "气泡水", "电解质水",
                "农夫山泉", "怡宝", "凉茶", "王老吉", "加多宝", "茶饮", "绿茶", "红茶",
                "冰红茶", "茶派", "果汁"));
        CONCEPT_ALIASES.put("dairy", List.of(
                "奶制品", "牛奶", "酸奶", "豆奶", "早餐奶", "乳", "旺仔", "伊利", "优酸乳"));
        CONCEPT_ALIASES.put("alcohol", List.of(
                "酒", "啤酒", "果酒", "rio", "百威", "喜力", "青岛", "雪花", "哈尔滨", "二锅头"));
        CONCEPT_ALIASES.put("snack", List.of(
                "零食", "休闲食品", "小吃", "小食"));
        CONCEPT_ALIASES.put("puffed_snack", List.of(
                "膨化", "薯片", "虾条", "虾片", "妙脆角", "奇多", "上好佳", "洋葱圈", "粟米条"));
        CONCEPT_ALIASES.put("nuts_fruit", List.of(
                "果仁", "果脯", "坚果", "花生", "瓜子", "腰果", "开心果", "芒果干", "枣", "地瓜干"));
        CONCEPT_ALIASES.put("instant_noodle", List.of(
                "方便面", "泡面", "杯面", "牛肉面", "合味道", "康师傅", "五谷道场", "今野"));
        CONCEPT_ALIASES.put("biscuit_pastry", List.of(
                "饼干", "点心", "威化", "奥利奥", "泡芙", "蛋糕", "面包", "夹心饼", "派"));
        CONCEPT_ALIASES.put("chocolate", List.of(
                "巧克力", "德芙", "好时", "mm", "m&m"));
        CONCEPT_ALIASES.put("gum", List.of(
                "口香糖", "薄荷糖", "绿箭", "益达"));
        CONCEPT_ALIASES.put("candy", List.of(
                "糖果", "棒棒糖", "软糖", "硬糖", "奶糖"));
        CONCEPT_ALIASES.put("canned", List.of(
                "罐头", "粥", "午餐肉", "椰浆", "鱼罐头", "银鹭"));
        CONCEPT_ALIASES.put("condiment", List.of(
                "调味料", "酱油", "醋", "盐", "味精", "鸡精", "料酒", "辣椒", "火锅底料"));
        CONCEPT_ALIASES.put("hygiene", List.of(
                "个人卫生", "牙膏", "牙刷", "洗发", "沐浴", "香皂", "肥皂"));
        CONCEPT_ALIASES.put("tissue", List.of(
                "纸巾", "抽纸", "卷纸", "湿巾", "手帕纸"));
        CONCEPT_ALIASES.put("stationery", List.of(
                "文具", "笔", "本", "胶带", "橡皮", "铅笔", "圆珠笔"));
        CONCEPT_ALIASES.put("dry_goods", List.of(
                "干货", "香菇", "木耳", "黄花菜", "桂圆干", "茶树菇"));
        CONCEPT_ALIASES.put("instant_drink", List.of(
                "冲调", "奶茶", "麦片", "豆浆粉", "米稀", "早餐", "茶包"));
    }

    public TextVector vectorizeQuery(String query) {
        TextVector vector = new TextVector();
        String normalized = normalize(query);
        if (!StringUtils.hasText(normalized)) {
            return vector;
        }

        addQueryIntents(vector, normalized, 4.2);
        addAliases(vector, normalized, 4.0);
        addNgrams(vector, normalized, 0.75);
        addCjkChars(vector, normalized, termLength(normalized) <= 1 ? 0.85 : 0.22);
        vector.add("literal:" + normalized, 1.2);
        return vector;
    }

    public TextVector vectorizeGoods(GoodsSearchDocument document) {
        if (document == null) {
            return new TextVector();
        }
        return goodsVectorCache.computeIfAbsent(goodsCacheKey(document), key -> buildGoodsVector(document));
    }

    private String goodsCacheKey(GoodsSearchDocument document) {
        return (document.getName() == null ? "" : document.getName())
                + "\u0001" + (document.getCategoryName() == null ? "" : document.getCategoryName())
                + "\u0001" + (document.getCategoryId() == null ? "" : document.getCategoryId());
    }

    private TextVector buildGoodsVector(GoodsSearchDocument document) {
        TextVector vector = new TextVector();

        String name = normalize(document.getName());
        String category = normalize(document.getCategoryName());

        if (StringUtils.hasText(category)) {
            vector.add("category:" + category, 2.5);
            addAliases(vector, category, 5.0);
            addNgrams(vector, category, 0.5);
            addCjkChars(vector, category, 0.12);
        }
        if (StringUtils.hasText(name)) {
            addAliases(vector, name, 2.4);
            addNgrams(vector, name, 0.35);
            addCjkChars(vector, name, 0.08);
            vector.add("literal:" + name, 0.4);
        }
        if (document.getCategoryId() != null) {
            vector.add("category-id:" + document.getCategoryId(), 1.0);
        }
        return vector;
    }

    public String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .replace("（", "(")
                .replace("）", ")");
    }

    public boolean isShortQuery(String query) {
        return termLength(query) <= 1;
    }

    public boolean isBroadIntentQuery(String query) {
        String normalized = normalize(query);
        if (!StringUtils.hasText(normalized)) {
            return false;
        }
        if (isShortQuery(normalized)) {
            return true;
        }
        for (QueryIntent intent : QUERY_INTENTS) {
            if (intent.matches(normalized)) {
                return true;
            }
        }
        return false;
    }

    public int termLength(String text) {
        String normalized = normalize(text);
        return normalized.codePointCount(0, normalized.length());
    }

    private void addQueryIntents(TextVector vector, String query, double conceptWeight) {
        for (QueryIntent intent : QUERY_INTENTS) {
            if (intent.matches(query)) {
                for (String concept : intent.concepts()) {
                    vector.add("concept:" + concept, conceptWeight);
                }
            }
        }
    }

    private void addAliases(TextVector vector, String text, double conceptWeight) {
        for (Map.Entry<String, List<String>> entry : CONCEPT_ALIASES.entrySet()) {
            for (String alias : entry.getValue()) {
                String normalizedAlias = normalize(alias);
                if (StringUtils.hasText(normalizedAlias) && text.contains(normalizedAlias)) {
                    vector.add("concept:" + entry.getKey(), conceptWeight);
                    vector.add("alias:" + normalizedAlias, conceptWeight * 0.35);
                }
            }
        }
    }

    private void addNgrams(TextVector vector, String text, double weight) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        int length = text.length();
        for (int size = 2; size <= 3; size++) {
            if (length < size) {
                continue;
            }
            for (int i = 0; i <= length - size; i++) {
                String gram = text.substring(i, i + size);
                if (gram.chars().allMatch(Character::isLetterOrDigit) && gram.length() < 3) {
                    continue;
                }
                vector.add("ng:" + gram, weight);
            }
        }
    }

    private void addCjkChars(TextVector vector, String text, double weight) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        text.codePoints()
                .filter(this::isCjk)
                .forEach(codePoint -> vector.add("ch:" + new String(Character.toChars(codePoint)), weight));
    }

    private boolean isCjk(int codePoint) {
        Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
        return script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA
                || script == Character.UnicodeScript.HANGUL;
    }

    private static boolean queryMatches(String query, String trigger) {
        if (!StringUtils.hasText(query) || !StringUtils.hasText(trigger)) {
            return false;
        }
        int queryLength = query.codePointCount(0, query.length());
        int triggerLength = trigger.codePointCount(0, trigger.length());
        if (queryLength <= 1 || triggerLength <= 1) {
            return query.equals(trigger);
        }
        return query.contains(trigger) || trigger.contains(query);
    }

    private record QueryIntent(List<String> triggers, List<String> concepts) {
        boolean matches(String query) {
            for (String trigger : triggers) {
                if (queryMatches(query, trigger)) {
                    return true;
                }
            }
            return false;
        }
    }
}
