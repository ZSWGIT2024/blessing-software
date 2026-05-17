// EmojiServiceImpl.java
package com.itheima.service.impl;

import com.itheima.dto.*;
import com.itheima.pojo.*;
import com.itheima.mapper.EmojiMapper;
import com.itheima.service.EmojiService;
import com.itheima.utils.AliOssUtil;
import com.itheima.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmojiServiceImpl implements EmojiService {

    private final EmojiMapper emojiMapper;
    private final AliOssUtil aliOssUtil;
    private final EmojiCacheService emojiCacheService;

    @Value("${upload.max-size.chat:10485760}") // 聊天文件最大10MB
    private long maxImageSize;

    // 系统Emoji分类数据（硬编码在后端）
    private static final Map<String, SystemEmojiCategory> SYSTEM_EMOJI_CATEGORIES = new LinkedHashMap<>();
    private static final Map<String, List<SystemEmoji>> SYSTEM_EMOJI_MAP = new HashMap<>();

    // 静态初始化块，加载系统Emoji数据
    static {
        // 初始化分类
        SYSTEM_EMOJI_CATEGORIES.put("smileys", new SystemEmojiCategory("smileys", "😀", "笑脸"));
        SYSTEM_EMOJI_CATEGORIES.put("people", new SystemEmojiCategory("people", "👋", "人物"));
        SYSTEM_EMOJI_CATEGORIES.put("animals", new SystemEmojiCategory("animals", "🐶", "动物"));
        SYSTEM_EMOJI_CATEGORIES.put("food", new SystemEmojiCategory("food", "🍎", "食物"));
        SYSTEM_EMOJI_CATEGORIES.put("activities", new SystemEmojiCategory("activities", "⚽", "活动"));
        SYSTEM_EMOJI_CATEGORIES.put("travel", new SystemEmojiCategory("travel", "✈️", "旅行"));
        SYSTEM_EMOJI_CATEGORIES.put("objects", new SystemEmojiCategory("objects", "💡", "物品"));
        SYSTEM_EMOJI_CATEGORIES.put("symbols", new SystemEmojiCategory("symbols", "❤️", "符号"));
        SYSTEM_EMOJI_CATEGORIES.put("flags", new SystemEmojiCategory("flags", "🏁", "旗帜"));

        // 初始化每个分类下的Emoji
        SYSTEM_EMOJI_MAP.put("smileys", Arrays.asList(
                new SystemEmoji("😀", " grinning face", "smileys"),
                new SystemEmoji("😁", "beaming face with smiling eyes", "smileys"),
                new SystemEmoji("😂", "face with tears of joy", "smileys"),
                new SystemEmoji("😃", "grinning face with big eyes", "smileys"),
                new SystemEmoji("😄", "grinning face with smiling eyes", "smileys"),
                new SystemEmoji("😅", "grinning face with sweat", "smileys"),
                new SystemEmoji("😆", "grinning squinting face", "smileys"),
                new SystemEmoji("😉", "winking face", "smileys"),
                new SystemEmoji("😊", "smiling face with smiling eyes", "smileys"),
                new SystemEmoji("😋", "face savoring food", "smileys"),
                new SystemEmoji("😎", "smiling face with sunglasses", "smileys"),
                new SystemEmoji("😍", "smiling face with heart-eyes", "smileys"),
                new SystemEmoji("😘", "face blowing a kiss", "smileys"),
                new SystemEmoji("😗", "kissing face", "smileys"),
                new SystemEmoji("😙", "kissing face with smiling eyes", "smileys"),
                new SystemEmoji("😚", "kissing face with closed eyes", "smileys"),
                new SystemEmoji("🙂", "slightly smiling face", "smileys"),
                new SystemEmoji("🤗", "hugging face", "smileys"),
                new SystemEmoji("🤩", "star-struck", "smileys"),
                new SystemEmoji("🤔", "thinking face", "smileys"),
                new SystemEmoji("😐", "neutral face", "smileys"),
                new SystemEmoji("😑", "expressionless face", "smileys"),
                new SystemEmoji("😶", "face without mouth", "smileys"),
                new SystemEmoji("🙄", "face with rolling eyes", "smileys"),
                new SystemEmoji("😏", "smirking face", "smileys"),
                new SystemEmoji("😣", "persevering face", "smileys"),
                new SystemEmoji("😥", "disappointed but relieved face", "smileys"),
                new SystemEmoji("😮", "face with open mouth", "smileys"),
                new SystemEmoji("🤐", "zipper-mouth face", "smileys"),
                new SystemEmoji("😯", "hushed face", "smileys"),
                new SystemEmoji("😪", "sleepy face", "smileys"),
                new SystemEmoji("😫", "tired face", "smileys"),
                new SystemEmoji("🥱", "yawning face", "smileys"),
                new SystemEmoji("😨", "fearful face", "smileys"),
                new SystemEmoji("😰", "face with open mouth and cold sweat", "smileys"),
                new SystemEmoji("😱", "face screaming in fear", "smileys"),
                new SystemEmoji("😲", "astonished face", "smileys"),
                new SystemEmoji("😳", "flushed face", "smileys"),
                new SystemEmoji("😵", "dizzy face", "smileys"),
                new SystemEmoji("😡", "pouting face", "smileys"),
                new SystemEmoji("😠", "angry face", "smileys"),
                new SystemEmoji("🤬", "face with symbols on mouth", "smileys"),
                new SystemEmoji("😷", "face with medical mask", "smileys"),
                new SystemEmoji("🤒", "face with thermometer", "smileys"),
                new SystemEmoji("🤕", "face with head-bandage", "smileys"),
                new SystemEmoji("🤢", "nauseated face", "smileys"),
                new SystemEmoji("🤮", "face vomiting", "smileys"),
                new SystemEmoji("🤧", "sneezing face", "smileys"),
                new SystemEmoji("🥵", "hot face", "smileys"),
                new SystemEmoji("🥶", "cold face", "smileys"),
                new SystemEmoji("🥴", "woozy face", "smileys"),
                new SystemEmoji("😈", "smiling face with horns", "smileys"),
                new SystemEmoji("👿", "angry face with horns", "smileys"),
                new SystemEmoji("👹", "ogre", "smileys"),
                new SystemEmoji("👺", "goblin", "smileys"),
                new SystemEmoji("💀", "skull", "smileys"),
                new SystemEmoji("☠️", "skull and crossbones", "smileys"),
                new SystemEmoji("👻", "ghost", "smileys"),
                new SystemEmoji("👽", "alien", "smileys"),
                new SystemEmoji("👾", "alien monster", "smileys"),
                new SystemEmoji("🤖", "robot", "smileys"),
                new SystemEmoji("🎃", "jack-o-lantern", "smileys"),
                new SystemEmoji("😺", "smiling cat face with open mouth", "smileys"),
                new SystemEmoji("😸", "smiling cat face with open mouth and smiling eyes", "smileys"),
                new SystemEmoji("😹", "cat face with tears of joy", "smileys"),
                new SystemEmoji("😻", "smiling cat face with heart-eyes", "smileys"),
                new SystemEmoji("😼", "cat face with wry smile", "smileys"),
                new SystemEmoji("😽", "kissing cat face with closed eyes", "smileys"),
                new SystemEmoji("🙀", "weary cat face", "smileys"),
                new SystemEmoji("😿", "crying cat face", "smileys"),
                new SystemEmoji("😾", "pouting cat face", "smileys"),
                new SystemEmoji("🙈", "see-no-evil monkey", "smileys"),
                new SystemEmoji("🙉", "hear-no-evil monkey", "smileys"),
                new SystemEmoji("🙊", "speak-no-evil monkey", "smileys"),
                new SystemEmoji("💋", "kiss mark", "smileys"),
                new SystemEmoji("💌", "love letter", "smileys"),
                new SystemEmoji("💘", "heart with arrow", "smileys"),
                new SystemEmoji("💝", "heart with ribbon", "smileys"),
                new SystemEmoji("💖", "sparkling heart", "smileys"),
                new SystemEmoji("💗", "growing heart", "smileys"),
                new SystemEmoji("💓", "beating heart", "smileys"),
                new SystemEmoji("💞", "revolving hearts", "smileys"),
                new SystemEmoji("💕", "two hearts", "smileys"),
                new SystemEmoji("💟", "heart decoration", "smileys"),
                new SystemEmoji("❣️", "heart exclamation", "smileys"),
                new SystemEmoji("💔", "broken heart", "smileys"),
                new SystemEmoji("❤️", "red heart", "smileys"),
                new SystemEmoji("🧡", "orange heart", "smileys"),
                new SystemEmoji("💛", "yellow heart", "smileys"),
                new SystemEmoji("💚", "green heart", "smileys"),
                new SystemEmoji("💙", "blue heart", "smileys"),
                new SystemEmoji("💜", "purple heart", "smileys"),
                new SystemEmoji("🤎", "brown heart", "smileys"),
                new SystemEmoji("🖤", "black heart", "smileys"),
                new SystemEmoji("🤍", "white heart", "smileys"),
                new SystemEmoji("💯", "hundred points", "smileys"),
                new SystemEmoji("💢", "anger symbol", "smileys"),
                new SystemEmoji("💥", "collision", "smileys"),
                new SystemEmoji("💫", "dizzy", "smileys"),
                new SystemEmoji("💦", "sweat droplets", "smileys"),
                new SystemEmoji("💨", "dashing away", "smileys"),
                new SystemEmoji("🕳️", "hole", "smileys"),
                new SystemEmoji("💣", "bomb", "smileys"),
                new SystemEmoji("💬", "speech balloon", "smileys"),
                new SystemEmoji("👁️‍🗨️", "eye in speech bubble", "smileys"),
                new SystemEmoji("🗨️", "left speech bubble", "smileys"),
                new SystemEmoji("🗯️", "right anger bubble", "smileys"),
                new SystemEmoji("💭", "thought balloon", "smileys"),
                new SystemEmoji("💤", "zzz", "smileys")
        ));

        SYSTEM_EMOJI_MAP.put("people", Arrays.asList(
                new SystemEmoji("👋", "waving hand", "people"),
                new SystemEmoji("🤚", "raised back of hand", "people"),
                new SystemEmoji("🖐️", "hand with fingers splayed", "people"),
                new SystemEmoji("✋", "raised hand", "people"),
                new SystemEmoji("🖖", "vulcan salute", "people"),
                new SystemEmoji("👌", "OK hand", "people"),
                new SystemEmoji("🤌", "pinched fingers", "people"),
                new SystemEmoji("🤏", "pinching hand", "people"),
                new SystemEmoji("✌️", "victory hand", "people"),
                new SystemEmoji("🤞", "crossed fingers", "people"),
                new SystemEmoji("🤟", "love-you gesture", "people"),
                new SystemEmoji("🤘", "sign of the horns", "people"),
                new SystemEmoji("🤙", "call me hand", "people"),
                new SystemEmoji("👈", "backhand index pointing left", "people"),
                new SystemEmoji("👉", "backhand index pointing right", "people"),
                new SystemEmoji("👆", "backhand index pointing up", "people"),
                new SystemEmoji("🖕", "middle finger", "people"),
                new SystemEmoji("👇", "backhand index pointing down", "people"),
                new SystemEmoji("☝️", "index pointing up", "people"),
                new SystemEmoji("👍", "thumbs up", "people"),
                new SystemEmoji("👎", "thumbs down", "people"),
                new SystemEmoji("✊", "raised fist", "people"),
                new SystemEmoji("👊", "oncoming fist", "people"),
                new SystemEmoji("🤛", "left-facing fist", "people"),
                new SystemEmoji("🤜", "right-facing fist", "people"),
                new SystemEmoji("👏", "clapping hands", "people"),
                new SystemEmoji("🙌", "raising hands", "people"),
                new SystemEmoji("🤝", "handshake", "people"),
                new SystemEmoji("🙏", "folded hands", "people"),
                new SystemEmoji("🤲", "palms up together", "people"),
                new SystemEmoji("🤞‍♂️", "man with raised fist", "people"),
                new SystemEmoji("🤞‍♀️", "woman with raised fist", "people"),
                new SystemEmoji("🤟‍♂️", "man with love-you gesture", "people"),
                new SystemEmoji("🤟‍♀️", "woman with love-you gesture", "people"),
                new SystemEmoji("💅", "nail polish", "people"),
                new SystemEmoji("🤳", "selfie", "people"),
                new SystemEmoji("💪", "flexed biceps", "people"),
                new SystemEmoji("🦵", "leg", "people"),
                new SystemEmoji("🦶", "foot", "people"),
                new SystemEmoji("👂", "ear", "people"),
                new SystemEmoji("👃", "nose", "people"),
                new SystemEmoji("🧠", "brain", "people"),
                new SystemEmoji("🫀", "anatomical heart", "people"),
                new SystemEmoji("🫁", "lungs", "people"),
                new SystemEmoji("🦷", "tooth", "people"),
                new SystemEmoji("🦴", "bone", "people"),
                new SystemEmoji("👀", "eyes", "people"),
                new SystemEmoji("👁️", "eye", "people"),
                new SystemEmoji("👅", "tongue", "people"),
                new SystemEmoji("👄", "mouth", "people"),
                new SystemEmoji("👶", "baby", "people"),
                new SystemEmoji("🧒", "child", "people"),
                new SystemEmoji("👦", "boy", "people"),
                new SystemEmoji("👧", "girl", "people"),
                new SystemEmoji("🧑", "person", "people"),
                new SystemEmoji("👱", "person: blond hair", "people"),
                new SystemEmoji("👨", "man", "people"),
                new SystemEmoji("👩", "woman", "people"),
                new SystemEmoji("🧓", "older person", "people"),
                new SystemEmoji("👴", "old man", "people"),
                new SystemEmoji("👵", "old woman", "people"),
                new SystemEmoji("🙍", "person frowning", "people"),
                new SystemEmoji("🙎", "person pouting", "people"),
                new SystemEmoji("🙅", "person gesturing NO", "people"),
                new SystemEmoji("🙆", "person gesturing OK", "people"),
                new SystemEmoji("💁", "person tipping hand", "people"),
                new SystemEmoji("🙋", "person raising hand", "people"),
                new SystemEmoji("🧏", "deaf person", "people"),
                new SystemEmoji("🙇", "person bowing", "people"),
                new SystemEmoji("🤦", "person facepalming", "people"),
                new SystemEmoji("🤷", "person shrugging", "people"),
                new SystemEmoji("👮", "police officer", "people"),
                new SystemEmoji("🕵️", "detective", "people"),
                new SystemEmoji("💂", "guard", "people"),
                new SystemEmoji("🥷", "ninja", "people"),
                new SystemEmoji("👷", "construction worker", "people"),
                new SystemEmoji("🤴", "prince", "people"),
                new SystemEmoji("👸", "princess", "people"),
                new SystemEmoji("👳", "person wearing turban", "people"),
                new SystemEmoji("👲", "man with gua pi mao", "people"),
                new SystemEmoji("🧕", "woman with headscarf", "people"),
                new SystemEmoji("🤵", "man in tuxedo", "people"),
                new SystemEmoji("👰", "bride with veil", "people"),
                new SystemEmoji("🤰", "pregnant woman", "people"),
                new SystemEmoji("🤱", "breast-feeding", "people"),
                new SystemEmoji("👼", "baby angel", "people"),
                new SystemEmoji("🎅", "father christmas", "people"),
                new SystemEmoji("🤶", "mother christmas", "people"),
                new SystemEmoji("🦸", "superhero", "people"),
                new SystemEmoji("🦹", "supervillain", "people"),
                new SystemEmoji("🧙", "mage", "people"),
                new SystemEmoji("🧚", "fairy", "people"),
                new SystemEmoji("🧛", "vampire", "people"),
                new SystemEmoji("🧜", "merperson", "people"),
                new SystemEmoji("🧝", "elf", "people"),
                new SystemEmoji("🧞", "genie", "people"),
                new SystemEmoji("🧟", "zombie", "people"),
                new SystemEmoji("💆", "person getting massage", "people"),
                new SystemEmoji("💇", "person getting haircut", "people"),
                new SystemEmoji("🚶", "person walking", "people"),
                new SystemEmoji("🧍", "person standing", "people"),
                new SystemEmoji("🧎", "person kneeling", "people"),
                new SystemEmoji("🏃", "person running", "people"),
                new SystemEmoji("💃", "woman dancing", "people"),
                new SystemEmoji("🕺", "man dancing", "people"),
                new SystemEmoji("👯", "people with bunny ears", "people"),
                new SystemEmoji("🕴️", "man in business suit levitating", "people"),
                new SystemEmoji("👰‍♀️", "bride with veil", "people"),
                new SystemEmoji("🤵‍♂️", "man in tuxedo", "people"),
                new SystemEmoji("🤰‍♀️", "pregnant woman", "people"),
                new SystemEmoji("🤱‍♀️", "breast-feeding", "people"),
                new SystemEmoji("👩‍🦰", "woman: red hair", "people"),
                new SystemEmoji("👩‍🦱", "woman: curly hair", "people"),
                new SystemEmoji("👩‍🦳", "woman: white hair", "people"),
                new SystemEmoji("👩‍🦲", "woman: bald", "people"),
                new SystemEmoji("👩‍🦵", "woman: leg", "people"),
                new SystemEmoji("👩‍🦶", "woman: foot", "people"),
                new SystemEmoji("👩‍👦", "family: woman, boy", "people"),
                new SystemEmoji("👩‍👧", "family: woman, girl", "people"),
                new SystemEmoji("👩‍👧‍👦", "family: woman, girl, boy", "people"),
                new SystemEmoji("👩‍👦‍👦", "family: woman, boy, boy", "people"),
                new SystemEmoji("👩‍👧‍👧", "family: woman, girl, girl", "people"),
                new SystemEmoji("👩‍👩‍👦", "family: woman, woman, boy", "people"),
                new SystemEmoji("👩‍👩‍👧", "family: woman, woman, girl", "people"),
                new SystemEmoji("👩‍👩‍👧‍👦", "family: woman, woman, girl, boy", "people"),
                new SystemEmoji("👩‍👩‍👦‍👦", "family: woman, woman, boy, boy", "people"),
                new SystemEmoji("👩‍👩‍👧‍👧", "family: woman, woman, girl, girl", "people")
        ));

        SYSTEM_EMOJI_MAP.put("animals", Arrays.asList(
                new SystemEmoji("🐶", "dog face", "animals"),
                new SystemEmoji("🐱", "cat face", "animals"),
                new SystemEmoji("🐭", "mouse face", "animals"),
                new SystemEmoji("🐹", "hamster", "animals"),
                new SystemEmoji("🐰", "rabbit face", "animals"),
                new SystemEmoji("🦊", "fox", "animals"),
                new SystemEmoji("🐻", "bear", "animals"),
                new SystemEmoji("🐼", "panda", "animals"),
                new SystemEmoji("🐨", "koala", "animals"),
                new SystemEmoji("🐯", "tiger face", "animals"),
                new SystemEmoji("🦁", "lion", "animals"),
                new SystemEmoji("🐮", "cow face", "animals"),
                new SystemEmoji("🐷", "pig face", "animals"),
                new SystemEmoji("🐸", "frog", "animals"),
                new SystemEmoji("🐙", "octopus", "animals"),
                new SystemEmoji("🦑", "squid", "animals"),
                new SystemEmoji("🦐", "shrimp", "animals"),
                new SystemEmoji("🦞", "lobster", "animals"),
                new SystemEmoji("🐟", "fish", "animals"),
                new SystemEmoji("🐠", "tropical fish", "animals"),
                new SystemEmoji("🐡", "blowfish", "animals"),
                new SystemEmoji("🐬", "dolphin", "animals"),
                new SystemEmoji("🐳", "spouting whale", "animals"),
                new SystemEmoji("🐋", "whale", "animals")
        ));

        SYSTEM_EMOJI_MAP.put("food", Arrays.asList(
                new SystemEmoji("🍎", "red apple", "food"),
                new SystemEmoji("🍐", "pear", "food"),
                new SystemEmoji("🍊", "tangerine", "food"),
                new SystemEmoji("🍋", "lemon", "food"),
                new SystemEmoji("🍌", "banana", "food"),
                new SystemEmoji("🍉", "watermelon", "food"),
                new SystemEmoji("🍇", "grapes", "food"),
                new SystemEmoji("🍓", "strawberry", "food"),
                new SystemEmoji("🫐", "blueberries", "food"),
                new SystemEmoji("🍈", "melon", "food"),
                new SystemEmoji("🍒", "cherries", "food"),
                new SystemEmoji("🍑", "peach", "food"),
                new SystemEmoji("🥭", "mango", "food"),
                new SystemEmoji("🍍", "pineapple", "food"),
                new SystemEmoji("🥥", "coconut", "food"),
                new SystemEmoji("🥝", "kiwi fruit", "food"),
                new SystemEmoji("🍅", "tomato", "food"),
                new SystemEmoji("🍆", "eggplant", "food"),
                new SystemEmoji("🥑", "avocado", "food"),
                new SystemEmoji("🥦", "broccoli", "food"),
                new SystemEmoji("🥬", "leafy green", "food"),
                new SystemEmoji("🥒", "cucumber", "food"),
                new SystemEmoji("🌶️", "hot pepper", "food"),
                new SystemEmoji("🫑", "bell pepper", "food")
        ));

        SYSTEM_EMOJI_MAP.put("activities", Arrays.asList(
                new SystemEmoji("⚽", "soccer ball", "activities"),
                new SystemEmoji("🏀", "basketball", "activities"),
                new SystemEmoji("🏈", "american football", "activities"),
                new SystemEmoji("⚾", "baseball", "activities"),
                new SystemEmoji("🥎", "softball", "activities"),
                new SystemEmoji("🏐", "volleyball", "activities"),
                new SystemEmoji("🏉", "rugby football", "activities"),
                new SystemEmoji("🎾", "tennis", "activities"),
                new SystemEmoji("🥏", "flying disc", "activities"),
                new SystemEmoji("🎱", "bowling", "activities"),
                new SystemEmoji("🏓", "ping pong", "activities"),
                new SystemEmoji("🏸", "badminton", "activities"),
                new SystemEmoji("🥊", "boxing glove", "activities"),
                new SystemEmoji("🥋", "martial arts uniform", "activities"),
                new SystemEmoji("🥅", "goal net", "activities"),
                new SystemEmoji("⛳", "flag in hole", "activities"),
                new SystemEmoji("⛸", "ice skate", "activities"),
                new SystemEmoji("🎿", "ski and ski boot", "activities"),
                new SystemEmoji("🛷", "sled", "activities"),
                new SystemEmoji("🥌", "curling stone", "activities"),
                new SystemEmoji("🎣", "fishing pole", "activities"),
                new SystemEmoji("🎽", "running shirt", "activities")
        ));

        SYSTEM_EMOJI_MAP.put("travel", Arrays.asList(
                new SystemEmoji("✈️", "airplane", "travel"),
                new SystemEmoji("🛫", "airplane departure", "travel"),
                new SystemEmoji("🛬", "airplane arrival", "travel"),
                new SystemEmoji("💺", "seat", "travel"),
                new SystemEmoji("🚁", "helicopter", "travel"),
                new SystemEmoji("🚂", "locomotive", "travel"),
                new SystemEmoji("🚊", "tram", "travel"),
                new SystemEmoji("🚉", "station", "travel"),
                new SystemEmoji("🚆", "train", "travel"),
                new SystemEmoji("🚄", "high-speed train", "travel"),
                new SystemEmoji("🚅", "bullet train", "travel"),
                new SystemEmoji("🚈", "light rail", "travel"),
                new SystemEmoji("🚇", "metro", "travel"),
                new SystemEmoji("🚝", "monorail", "travel"),
                new SystemEmoji("🚋", "tram car", "travel"),
                new SystemEmoji("🚃", "railway car", "travel"),
                new SystemEmoji("🚎", "bus", "travel"),
                new SystemEmoji("🚌", "bus stop", "travel"),
                new SystemEmoji("🚍", "oncoming bus", "travel"),
                new SystemEmoji("🚙", "recreational vehicle", "travel"),
                new SystemEmoji("🛻", "delivery truck", "travel"),
                new SystemEmoji("🚲", "bicycle", "travel"),
                new SystemEmoji("🛴", "scooter", "travel"),
                new SystemEmoji("🛵", "motor scooter", "travel"),
                new SystemEmoji("🏍", "motorcycle", "travel"),
                new SystemEmoji("🛺", "auto rickshaw", "travel"),
                new SystemEmoji("🚗", "automobile", "travel"),
                new SystemEmoji("🚕", "taxi", "travel"),
                new SystemEmoji("🚖", "oncoming taxi", "travel"),
                new SystemEmoji("🚨", "police car", "travel"),
                new SystemEmoji("🚓", "ambulance", "travel"),
                new SystemEmoji("🚔", "fire engine", "travel"),
                new SystemEmoji("🚑", "ambulance", "travel"),
                new SystemEmoji("🚒", "fire engine", "travel"),
                new SystemEmoji("🚐", "minibus", "travel"),
                new SystemEmoji("🚛", "articulated lorry", "travel"),
                new SystemEmoji("🚚", "truck", "travel"),
                new SystemEmoji("🚜", "tractor", "travel"),
                new SystemEmoji("🛺", "auto rickshaw", "travel")
        ));

        SYSTEM_EMOJI_MAP.put("objects", Arrays.asList(
                new SystemEmoji("💡", "electric light bulb", "objects"),
                new SystemEmoji("🔦", "flashlight", "objects"),
                new SystemEmoji("🕯️", "candle", "objects"),
                new SystemEmoji("🗿", "moyai", "objects"),
                new SystemEmoji("🏮", "red paper lantern", "objects"),
                new SystemEmoji("📷", "camera", "objects"),
                new SystemEmoji("📸", "camera with flash", "objects"),
                new SystemEmoji("📹", "video camera", "objects"),
                new SystemEmoji("📼", "videocassette", "objects"),
                new SystemEmoji("🔍", "magnifying glass tilted left", "objects"),
                new SystemEmoji("🔎", "magnifying glass tilted right", "objects"),
                new SystemEmoji("🕵️", "detective", "objects"),
                new SystemEmoji("🔫", "pistol", "objects"),
                new SystemEmoji("🏹", "bow and arrow", "objects"),
                new SystemEmoji("🛡️", "shield", "objects"),
                new SystemEmoji("🚬", "cigarette", "objects"),
                new SystemEmoji("⚰️", "coffin", "objects"),
                new SystemEmoji("🪦", "funeral urn", "objects"),
                new SystemEmoji("🏺", "amphora", "objects"),
                new SystemEmoji("💈", "barber pole", "objects"),
                new SystemEmoji("🛑", "stop sign", "objects"),
                new SystemEmoji("🚧", "construction", "objects"),
                new SystemEmoji("⚓", "anchor", "objects"),
                new SystemEmoji("⛵", "sailboat", "objects"),
                new SystemEmoji("🛶", "canoe", "objects"),
                new SystemEmoji("🚤", "speedboat", "objects"),
                new SystemEmoji("🛥", "motor boat", "objects"),
                new SystemEmoji("⛵", "sailboat", "objects"),
                new SystemEmoji("🛳", "passenger ship", "objects"),
                new SystemEmoji("⛴", "ferry", "objects")
        ));

        SYSTEM_EMOJI_MAP.put("symbols", Arrays.asList(
                new SystemEmoji("☀️", "sun", "symbols"),
                new SystemEmoji("🌞", "brightness", "symbols"),
                new SystemEmoji("🌟", "glowing star", "symbols"),
                new SystemEmoji("🌠", "shooting star", "symbols"),
                new SystemEmoji("🌨️", "cloud with snow", "symbols"),
                new SystemEmoji("🌩️", "cloud with lightning", "symbols"),
                new SystemEmoji("🌧️", "cloud with rain", "symbols"),
                new SystemEmoji("🌪️", "cloud with tornado", "symbols"),
                new SystemEmoji("🌫️", "fog", "symbols"),
                new SystemEmoji("🌬️", "wind face", "symbols"),
                new SystemEmoji("🌀", "cyclone", "symbols"),
                new SystemEmoji("🌈", "rainbow", "symbols"),
                new SystemEmoji("🌂", "closed umbrella", "symbols"),
                new SystemEmoji("☂️", "umbrella", "symbols"),
                new SystemEmoji("☔", "umbrella with rain drops", "symbols"),
                new SystemEmoji("⛱️", "umbrella on ground", "symbols"),
                new SystemEmoji("⚡", "high voltage", "symbols"),
                new SystemEmoji("❄️", "snowflake", "symbols"),
                new SystemEmoji("☃️", "snowman", "symbols"),
                new SystemEmoji("⛄", "snowman without snow", "symbols"),
                new SystemEmoji("☄️", "comet", "symbols"),
                new SystemEmoji("🔥", "fire", "symbols"),
                new SystemEmoji("💧", "droplet", "symbols"),
                new SystemEmoji("🌊", "water wave", "symbols"),
                new SystemEmoji("🎃", "jack-o-lantern", "symbols"),
                new SystemEmoji("🎄", "christmas tree", "symbols"),
                new SystemEmoji("🎆", "fireworks", "symbols"),
                new SystemEmoji("🎇", "sparkler", "symbols"),
                new SystemEmoji("🧨", "firecracker", "symbols"),
                new SystemEmoji("✨", "sparkles", "symbols"),
                new SystemEmoji("🎈", "balloon", "symbols"),
                new SystemEmoji("🎉", "party popper", "symbols"),
                new SystemEmoji("🎊", "confetti ball", "symbols"),
                new SystemEmoji("🎋", "tanabata tree", "symbols"),
                new SystemEmoji("🎍", "pine decoration", "symbols"),
                new SystemEmoji("🎎", "japanese dolls", "symbols"),
                new SystemEmoji("🎏", "carp streamer", "symbols"),
                new SystemEmoji("🎐", "wind chime", "symbols"),
                new SystemEmoji("🎑", "moon viewing ceremony", "symbols"),
                new SystemEmoji("🧧", "red envelope", "symbols"),
                new SystemEmoji("🎀", "ribbon", "symbols"),
                new SystemEmoji("🎁", "wrapped gift", "symbols"),
                new SystemEmoji("🎗", "reminder ribbon", "symbols"),
                new SystemEmoji("🎟️", "admission tickets", "symbols"),
                new SystemEmoji("🎫", "ticket", "symbols"),
                new SystemEmoji("🎖️", "military medal", "symbols"),
                new SystemEmoji("🏆", "trophy", "symbols"),
                new SystemEmoji("🏅", "sports medal", "symbols"),
                new SystemEmoji("🥇", "1st place medal", "symbols"),
                new SystemEmoji("🥈", "2nd place medal", "symbols"),
                new SystemEmoji("🥉", "3rd place medal", "symbols")
        ));

        SYSTEM_EMOJI_MAP.put("flags", Arrays.asList(
                new SystemEmoji("🏁", "chequered flag", "flags"),
                new SystemEmoji("🚩", "triangular flag", "flags"),
                new SystemEmoji("🏳️", "white flag", "flags"),
                new SystemEmoji("🏳️‍🌈", "rainbow flag", "flags"),
                new SystemEmoji("🏴", "black flag", "flags"),
                new SystemEmoji("🏳️‍🌂", "white flag with small black square", "flags"),
                new SystemEmoji("🏴‍☠️", "pirate flag", "flags")
        ));

        // 可以继续添加更多分类...
    }

    // 内部类，用于组织系统Emoji数据
    private static class SystemEmojiCategory {
        String id;
        String icon;
        String name;

        SystemEmojiCategory(String id, String icon, String name) {
            this.id = id;
            this.icon = icon;
            this.name = name;
        }
    }

    private static class SystemEmoji {
        String code;
        String name;
        String category;

        SystemEmoji(String code, String name, String category) {
            this.code = code;
            this.name = name;
            this.category = category;
        }
    }

    @Override
    public List<Map<String, Object>> getSystemEmojiCategories() {
        List<Map<String, Object>> categories = new ArrayList<>();
        for (SystemEmojiCategory category : SYSTEM_EMOJI_CATEGORIES.values()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", category.id);
            map.put("icon", category.icon);
            map.put("name", category.name);
            categories.add(map);
        }
        return categories;
    }

    @Override
    public List<EmojiDTO> getSystemEmojis(String category) {
        List<SystemEmoji> emojiList = SYSTEM_EMOJI_MAP.getOrDefault(category, new ArrayList<>());
        return emojiList.stream().map(se -> {
            EmojiDTO dto = new EmojiDTO();
            dto.setCode(se.code);
            dto.setName(se.name);
            dto.setCategory(se.category);
            dto.setType("emoji");
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<EmojiPackDTO> getEmojiPacks() {
        // 查缓存
        List<EmojiPackDTO> cached = emojiCacheService.getCachedPacks();
        if (cached != null) return cached;

        List<EmojiPack> packs = emojiMapper.selectAllPacks();
        List<EmojiPackDTO> dtos = new ArrayList<>();

        for (EmojiPack pack : packs) {
            EmojiPackDTO dto = new EmojiPackDTO();
            dto.setId(pack.getId());
            dto.setPackName(pack.getPackName());
            dto.setCoverUrl(pack.getCoverUrl());
            dto.setDescription(pack.getDescription());
            dto.setItemCount(emojiMapper.countPackItems(pack.getId()));
            dtos.add(dto);
        }
        // 写缓存（防穿透）
        if (dtos.isEmpty()) {
            emojiCacheService.cacheEmptyPacks();
        } else {
            emojiCacheService.cachePacks(dtos);
        }
        return dtos;
    }

    @Override
    public List<EmojiPackItemDTO> getEmojiPackItems(Long packId) {
        // 查缓存
        List<EmojiPackItemDTO> cached = emojiCacheService.getCachedPackItems(packId);
        if (cached != null) return cached;

        Integer userId = getCurrentUserId();
        List<EmojiPackItem> items = emojiMapper.selectPackItems(packId);
        List<EmojiPackItemDTO> dtos = new ArrayList<>();

        for (EmojiPackItem item : items) {
            EmojiPackItemDTO dto = new EmojiPackItemDTO();
            dto.setId(item.getId());
            dto.setPackId(item.getPackId());
            dto.setImageUrl(item.getImageUrl());
            dto.setDescription(item.getDescription());
            // 检查是否已收藏
            int count = emojiMapper.checkPackItemFavorite(userId, item.getId());
            dto.setIsFavorite(count > 0);
            dtos.add(dto);
        }
        // 写缓存（防穿透）
        if (dtos.isEmpty()) {
            emojiCacheService.cacheEmptyPackItems(packId);
        } else {
            emojiCacheService.cachePackItems(packId, dtos);
        }
        return dtos;
    }

    @Override
    @Transactional
    public EmojiPackDTO createEmojiPack(EmojiPackDTO emojiPackDTO) {
        // 参数校验
        if (emojiPackDTO.getPackName() == null || emojiPackDTO.getPackName().trim().isEmpty()) {
            throw new RuntimeException("表情包名称不能为空");
        }

        EmojiPack emojiPack = new EmojiPack();
        emojiPack.setPackName(emojiPackDTO.getPackName());
        emojiPack.setCoverUrl(emojiPackDTO.getCoverUrl());
        emojiPack.setDescription(emojiPackDTO.getDescription());
        emojiPack.setStatus(1); // 默认启用

        emojiMapper.insertEmojiPack(emojiPack);
        emojiCacheService.evictPacks(); // 表情包列表变更

        emojiPackDTO.setId(emojiPack.getId());
        emojiPackDTO.setItemCount(0);

        return emojiPackDTO;
    }

    @Override
    @Transactional
    public void updateEmojiPack(EmojiPackDTO emojiPackDTO) {
        // 检查表情包是否存在
        EmojiPack existingPack = emojiMapper.selectPackById(emojiPackDTO.getId());
        if (existingPack == null) {
            throw new RuntimeException("表情包不存在");
        }

        EmojiPack emojiPack = new EmojiPack();
        emojiPack.setId(emojiPackDTO.getId());
        emojiPack.setPackName(emojiPackDTO.getPackName());
        emojiPack.setCoverUrl(emojiPackDTO.getCoverUrl());
        emojiPack.setDescription(emojiPackDTO.getDescription());

        emojiMapper.updateEmojiPack(emojiPack);
        emojiCacheService.evictPacks(); // 表情包信息变更
    }

    @Override
    @Transactional
    public void deleteEmojiPack(Long packId) {
        // 检查表情包是否存在
        EmojiPack pack = emojiMapper.selectPackById(packId);
        if (pack == null) {
            throw new RuntimeException("表情包不存在");
        }

        // 软删除或硬删除，这里使用硬删除
        emojiMapper.deleteEmojiPack(packId);
        emojiCacheService.evictAllEmojiPackCache(packId); // 清除表情包及内容缓存
    }

    @Override
    public String uploadPackCover(MultipartFile file) {
        try {
            // 1. 文件校验
            if (file.isEmpty()) {
                throw new RuntimeException("文件不能为空");
            }

            if (file.getSize() > maxImageSize) {
                throw new RuntimeException("图片大小不能超过 " + (maxImageSize / 1024 / 1024) + "MB");
            }

            // 2. 校验文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RuntimeException("请上传图片文件");
            }

            // 3. 生成OSS路径
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String uuid = UUID.randomUUID().toString();

            // OSS对象名称：emoji/pack/cover/{date}/{uuid}.{ext}
            String objectName = String.format("emoji/pack/cover/%s/%s.%s",
                    datePath, uuid, extension);

            // 4. 上传到OSS
            String fileUrl = aliOssUtil.uploadFile(objectName, file.getInputStream());
            log.info("表情包封面上传成功: {}", fileUrl);

            return fileUrl;

        } catch (IOException e) {
            log.error("表情包封面上传失败", e);
            throw new RuntimeException("封面上传失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public List<EmojiPackItemDTO> uploadPackItems(Long packId, List<MultipartFile> files, List<String> descriptions) {
        // 1. 检查表情包是否存在
        EmojiPack pack = emojiMapper.selectPackById(packId);
        if (pack == null) {
            throw new RuntimeException("表情包不存在");
        }

        if (files == null || files.isEmpty()) {
            throw new RuntimeException("请选择要上传的图片");
        }

        List<EmojiPackItemDTO> result = new ArrayList<>();

        try {
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);

                // 文件校验
                if (file.isEmpty()) continue;

                if (file.getSize() > maxImageSize) {
                    throw new RuntimeException("图片大小不能超过 " + (maxImageSize / 1024 / 1024) + "MB");
                }

                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new RuntimeException("请上传图片文件");
                }

                // 生成OSS路径
                String originalFilename = file.getOriginalFilename();
                String extension = getFileExtension(originalFilename);
                String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                String uuid = UUID.randomUUID().toString();

                // OSS对象名称：emoji/pack/item/{packId}/{date}/{uuid}.{ext}
                String objectName = String.format("emoji/pack/item/%d/%s/%s.%s",
                        packId, datePath, uuid, extension);

                // 上传到OSS
                String fileUrl = aliOssUtil.uploadFile(objectName, file.getInputStream());

                // 获取描述
                String description = (descriptions != null && descriptions.size() > i)
                        ? descriptions.get(i) : null;

                // 保存到数据库
                EmojiPackItem item = new EmojiPackItem();
                item.setPackId(packId);
                item.setImageUrl(fileUrl);
                item.setDescription(description);
                item.setStatus(1);

                emojiMapper.insertPackItem(item);

                // 构建返回DTO
                EmojiPackItemDTO itemDTO = new EmojiPackItemDTO();
                itemDTO.setId(item.getId());
                itemDTO.setPackId(packId);
                itemDTO.setImageUrl(fileUrl);
                itemDTO.setDescription(description);
                itemDTO.setIsFavorite(false);

                result.add(itemDTO);
            }

            log.info("批量上传表情包图片成功，packId: {}, 数量: {}", packId, result.size());
            emojiCacheService.evictPackItems(packId); // 表情包内容变更
            return result;

        } catch (IOException e) {
            log.error("表情包图片上传失败", e);
            throw new RuntimeException("图片上传失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updatePackItem(EmojiPackItemDTO itemDTO) {
        EmojiPackItem item = emojiMapper.selectPackItemById(itemDTO.getId());
        if (item == null) {
            throw new RuntimeException("表情包图片不存在");
        }

        EmojiPackItem updateItem = new EmojiPackItem();
        updateItem.setId(itemDTO.getId());
        updateItem.setDescription(itemDTO.getDescription());

        emojiMapper.updatePackItem(updateItem);
        emojiCacheService.evictPackItems(item.getPackId());
    }

    @Override
    @Transactional
    public void deletePackItem(Long itemId) {
        EmojiPackItem item = emojiMapper.selectPackItemById(itemId);
        if (item == null) {
            throw new RuntimeException("表情包图片不存在");
        }

        emojiMapper.deletePackItem(itemId);
        emojiCacheService.evictPackItems(item.getPackId()); // 表情包内容变更
    }

    @Override
    @Transactional
    public void batchDeletePackItems(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return;
        }
        String idsStr = StringUtils.join(itemIds, ",");
        emojiMapper.batchDeletePackItems(idsStr);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex == -1 ? "" : fileName.substring(dotIndex + 1);
    }

    @Override
    public Map<String, Object> getFavorites(Integer type, Integer page, Integer pageSize) {
        Integer userId = getCurrentUserId();
        int offset = (page - 1) * pageSize;
        Map<String, Object> result = new HashMap<>();

        if (type == 1) { // Emoji收藏
            List<UserEmojiFavorite> favorites = emojiMapper.selectFavoriteEmojis(userId, offset, pageSize);
            int total = emojiMapper.countFavoriteEmojis(userId);

            List<FavoriteDTO> items = favorites.stream().map(f -> {
                FavoriteDTO dto = new FavoriteDTO();
                dto.setId(f.getId());
                dto.setType(1);
                dto.setEmojiCode(f.getEmojiCode());
                dto.setFavoriteId(f.getId());
                return dto;
            }).collect(Collectors.toList());

            result.put("items", items);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("totalPages", (total + pageSize - 1) / pageSize);

        } else { // 表情包收藏
            List<Map<String, Object>> favorites = emojiMapper.selectFavoritePackItems(userId, offset, pageSize);
            int total = emojiMapper.countFavoritePackItems(userId);

            List<FavoriteDTO> items = favorites.stream().map(f -> {
                FavoriteDTO dto = new FavoriteDTO();
                dto.setId(((Number) f.get("id")).longValue());
                dto.setType(2);
                dto.setPackItemId(((Number) f.get("pack_item_id")).longValue());
                dto.setImageUrl((String) f.get("image_url"));
                dto.setDescription((String) f.get("description"));
                dto.setFavoriteId(((Number) f.get("id")).longValue());
                return dto;
            }).collect(Collectors.toList());

            result.put("items", items);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("totalPages", (total + pageSize - 1) / pageSize);
        }

        return result;
    }

    @Override
    @Transactional
    public void addFavorite(Integer userId, Integer type, String emojiCode, String emojiName, Long packItemId) {
        UserEmojiFavorite favorite = new UserEmojiFavorite();
        favorite.setUserId(userId);
        favorite.setFavoriteType(type);

        if (type == 1) {
            if (emojiCode == null || emojiCode.isEmpty()) {
                throw new RuntimeException("Emoji代码不能为空");
            }
            favorite.setEmojiCode(emojiCode);
            favorite.setEmojiName(emojiName);
            // 检查是否已收藏
            int count = emojiMapper.checkEmojiFavorite(userId, emojiName);
            if (count > 0) {
                throw new RuntimeException("已经收藏过了");
            }
        } else {
            if (packItemId == null) {
                throw new RuntimeException("表情包项ID不能为空");
            }
            favorite.setPackItemId(packItemId);
            int count = emojiMapper.checkPackItemFavorite(userId, packItemId);
            if (count > 0) {
                throw new RuntimeException("已经收藏过了");
            }
        }

        emojiMapper.insertFavorite(favorite);
    }

    @Override
    @Transactional
    public void removeFavorite(Integer userId, Long favoriteId) {
        emojiMapper.deleteFavorite(userId, favoriteId);
    }

    @Override
    public void removeFavoriteByEmoji(Integer userId, String emojiName) {
        emojiMapper.deleteFavoriteByEmoji(userId, emojiName);
    }

    @Override
    public Map<String, Object> getRecentEmojis(Integer limit) {
        Integer userId = getCurrentUserId();
        Map<String, Object> result = new HashMap<>();

        // 获取最近使用的Emoji
        List<UserRecentEmoji> recentEmojis = emojiMapper.selectRecentEmojis(userId, limit);
        List<RecentEmojiDTO> emojiItems = recentEmojis.stream().map(r -> {
            RecentEmojiDTO dto = new RecentEmojiDTO();
            dto.setId(r.getId());
            dto.setType(1);
            dto.setEmojiCode(r.getEmojiCode());
            dto.setEmojiName(r.getEmojiName());
            dto.setUseCount(r.getUseCount());
            dto.setLastUseTime(r.getLastUseTime() != null ? r.getLastUseTime().toString() : null);
            return dto;
        }).collect(Collectors.toList());

        // 获取最近使用的表情包
        List<Map<String, Object>> recentPackItems = emojiMapper.selectRecentPackItems(userId, limit);
        List<RecentEmojiDTO> packItems = recentPackItems.stream().map(r -> {
            RecentEmojiDTO dto = new RecentEmojiDTO();
            dto.setId(((Number) r.get("id")).longValue());
            dto.setType(2);
            dto.setPackItemId(((Number) r.get("pack_item_id")).longValue());
            dto.setImageUrl((String) r.get("image_url"));
            dto.setDescription((String) r.get("description"));
            dto.setUseCount((Integer) r.get("use_count"));
            dto.setLastUseTime(r.get("last_use_time") != null ? r.get("last_use_time").toString() : null);
            return dto;
        }).collect(Collectors.toList());

        result.put("emojis", emojiItems);
        result.put("packs", packItems);
        return result;
    }

    @Override
    @Transactional
    public void recordUsage(Integer userId, Integer type, String emojiCode, String emojiName, Long packItemId) {
        if (type == 1) { // 单个Emoji
            // 先尝试更新
            int updated = emojiMapper.updateCountByEmojiName(userId, emojiName);

            // 如果没有更新到记录，说明是第一次使用，插入新记录
            if (updated == 0) {
                UserRecentEmoji recent = new UserRecentEmoji();
                recent.setUserId(userId);
                recent.setEmojiCode(emojiCode);
                recent.setEmojiName(emojiName);
                recent.setUseType(type);
                recent.setUseCount(1);
                // packItemId 设置为 null
                emojiMapper.insert(recent);
            }
        } else { // 表情包项
            // 先尝试更新
            int updated = emojiMapper.updateCountByPackItemId(userId, packItemId);

            // 如果没有更新到记录，插入新记录
            if (updated == 0) {
                UserRecentEmoji recent = new UserRecentEmoji();
                recent.setUserId(userId);
                recent.setPackItemId(packItemId);
                recent.setUseType(type);
                recent.setUseCount(1);
                // emojiCode 设置为 null
                emojiMapper.insert(recent);
            }
        }
    }

    @Override
    @Transactional
    public void clearRecent(Integer userId, Integer type) {
        emojiMapper.clearRecent(userId, type);
    }

    // 获取当前用户ID
    private Integer getCurrentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return (Integer) claims.get("id");
    }
}