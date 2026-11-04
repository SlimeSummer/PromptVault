package com.promptvault.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 批量导入自动分类的规则引擎。
 *
 * <p>当前版本为纯关键词匹配，分类词典硬编码在本类中；
 * 未来可替换成 AI 调用，保持 classify 方法签名不变即可。</p>
 */
@Component
public class TagClassifier {

    /** value → 触发关键词（小写）集合。命中任一关键词即归入对应分类。 */
    private static final Map<String, Set<String>> RULES;

    static {
        Map<String, Set<String>> rules = new LinkedHashMap<>();
        // 质量词
        rules.put("quality", Set.of(
                "masterpiece", "best quality", "high quality", "ultra detailed", "ultra-detailed",
                "intricate details", "fine details", "beautiful", "gorgeous", "amazing",
                "award winning", "award-winning", "official art", "official",
                "from above", "depth of field", "sharp focus", "cinematic", "hdr", "8k", "4k"
        ));
        // 触发词（LoRA/角色专用 tag，带括号或常见角色词）
        rules.put("trigger", Set.of(
                "1girl", "1boy", "solo", "solo focus", "master", "masterpiece", "best",
                "looking at viewer", "from below", "from side", "from behind"
        ));
        // 风格
        rules.put("style", Set.of(
                "anime", "manga", "realistic", "realism", "photo", "photorealistic",
                "oil painting", "watercolor", "sketch", "lineart", "3d", "cgi", "render",
                "chibi", "cyberpunk", "steampunk", "fantasy", "sci-fi", "dark", "horror",
                "monochrome", "grayscale", "sepia"
        ));
        // 穿搭
        rules.put("outfit", Set.of(
                "dress", "skirt", "jacket", "coat", "hoodie", "shirt", "t-shirt", "pants",
                "shorts", "boots", "shoes", "socks", "gloves", "hat", "helmet", "suit",
                "uniform", "kimono", "school uniform", "serafuku", "stockings", "thighhighs",
                "swimsuit", "bikini", "lingerie", "clothes", "outfit", "necklace", "ribbon",
                "bow", "scarf", "glasses", "eyeglasses", "sunglasses", "tie"
        ));
        // 场景
        rules.put("scene", Set.of(
                "background", "sky", "cloud", "sun", "moon", "starry sky", "night sky",
                "indoor", "outdoor", "city", "street", "school", "classroom", "bedroom",
                "forest", "mountain", "beach", "ocean", "sea", "river", "lake",
                "flowers", "garden", "window", "door", "building", "ruins", "nature",
                "white background", "simple background", "black background"
        ));
        // 动作
        rules.put("action", Set.of(
                "standing", "sitting", "squatting", "lying", "sleeping", "walking", "running",
                "jumping", "kneeling", "posing", "dancing", "holding", "carrying", "waving",
                "smiling", "laughing", "crying", "thinking", "looking away", "looking back",
                "arms up", "hands up", "open mouth", "closed eyes", "blush"
        ));
        // 表情
        rules.put("expression", Set.of(
                "smile", "smiling", "grin", "laugh", "happy", "sad", "angry", "cry",
                "crying", "tears", "blush", "blushing", "surprised", "shy", "embarrassed",
                "worried", "thinking", "pouting", "closed eyes", "half-closed eyes"
        ));
        // 细节（头发、眼睛、身体部位、光影等描述性细节）
        rules.put("detail", Set.of(
                "hair", "short hair", "long hair", "ponytail", "twin tails", "twintails",
                "braid", "blonde hair", "brown hair", "black hair", "blue hair", "pink hair",
                "red hair", "white hair", "silver hair",
                "eyes", "blue eyes", "red eyes", "green eyes", "yellow eyes", "heterochromia",
                "skin", "dark skin", "tan skin", "fair skin",
                "small breasts", "medium breasts", "large breasts",
                "light", "lighting", "soft light", "hard light", "rim light", "backlight",
                "bokeh", "particle", "sparkle", "sparkles", "glow", "glowing", "aura"
        ));
        // 负面
        rules.put("negative", Set.of(
                "low quality", "worst quality", "bad quality", "ugly", "deformed", "disfigured",
                "bad anatomy", "bad hands", "missing fingers", "extra fingers", "extra limbs",
                "malformed", "mutated", "duplicate", "over saturated", "over-saturated",
                "watermark", "signature", "text", "logo", "username", "jpeg artifacts",
                "blurry", "cropped", "out of frame", "out-of-frame", "lowres", "3d"
        ));
        RULES = rules;
    }

    /**
     * 对单个提示词进行分类。
     *
     * @param prompt 去掉权重后的标签本体（小写）
     * @return 分类 value，未命中任何规则返回 "other"
     */
    public String classify(String prompt) {
        String lower = prompt == null ? "" : prompt.toLowerCase();
        if (lower.isEmpty()) {
            return "other";
        }
        // 负面优先：含有 negative 关键词的一定归负面，避免被其他规则抢走
        Set<String> neg = RULES.get("negative");
        if (neg != null && containsAny(lower, neg)) {
            return "negative";
        }
        for (Map.Entry<String, Set<String>> entry : RULES.entrySet()) {
            if ("negative".equals(entry.getKey())) {
                continue;
            }
            if (containsAny(lower, entry.getValue())) {
                return entry.getKey();
            }
        }
        return "other";
    }

    private boolean containsAny(String haystack, Set<String> needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
