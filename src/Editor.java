import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文员
 */
public class Editor extends Worker {

    private static final String DEPT = "Editor";

    public Editor() {

    }

    /**
     * @param name   姓名
     * @param age    年龄
     * @param salary 工资
     */
    public Editor(String name, int age, int salary) {
        super(name, age, salary, DEPT);
    }

    /**
     * 文本对齐
     * <p>
     * 根据统计经验，用户在手机上阅读英文新闻的时候，
     * 一行最多显示32个字节（1个中文占2个字节）时最适合用户阅读。
     * 给定一段字符串，重新排版，使得每行恰好有32个字节，并输出至控制台
     * 首行缩进4个字节，其余行数左对齐，每个短句不超过32个字节，
     * 每行最后一个有效字符必须为标点符号
     * <p>
     * 示例：
     * <p>
     * String：给定一段字符串，重新排版，使得每行恰好有32个字符，并输出至控制台首行缩进，其余行数左对齐，每个短句不超过32个字符。
     * <p>
     * 控制台输出:
     * 给定一段字符串，重新排版，
     * 使得每行恰好有32个字符，
     * 并输出至控制台首行缩进，
     * 其余行数左对齐，
     * 每个短句不超过32个字符。
     */
    public void textExtraction(String data) {
        List<String> sentences = this.splitText(data, true);
        StringBuilder builder = new StringBuilder();
        final int maxSize = 32;
        StringBuilder curLine = new StringBuilder("    ");
        int i = 0;
        while (i < sentences.size()) {
            while (i < sentences.size() && byteLength(sentences.get(i)) <= maxSize - byteLength(curLine.toString())) {
                curLine.append(sentences.get(i));
                i++;
            }
            i--;
            builder.append(curLine);
            if (i < sentences.size() - 1) {
                builder.append(System.getProperty("line.separator"));
            }
            curLine = new StringBuilder();
            i++;
        }
        System.out.println(builder.toString());
    }

    /**
     * @param str 要获取字符长度的字符串
     * @return 字符长度
     */
    private int byteLength(String str) {
        try {
            return str.getBytes("GBK").length;
        } catch (UnsupportedEncodingException e) {
            return 0;
        }
    }

    /**
     * @param data 中文句子
     * @return 包含文末标点符号的句子列表
     */
    private List<String> splitText(String data, boolean hasPuncuation) {
        Pattern punctuationPattern = Pattern.compile("[，。？！＠＃￥％……＆（）]");
        Matcher m = punctuationPattern.matcher(data);
        List<String> sentences = new ArrayList<>();
        int curIndex = 0;
        while (m.find()) {
            sentences.add(hasPuncuation ? data.substring(curIndex, m.start() + 1) : data.substring(curIndex, m.start()));
            curIndex = m.start() + 1;
        }
        return sentences;
    }


    /**
     * 标题排序
     * <p>
     * 将给定的新闻标题按照拼音首字母进行排序，
     * 首字母相同则按第二个字母，如果首字母相同，则首字拼音没有后续的首字排在前面，如  鹅(e)与二(er)，
     * 以鹅为开头的新闻排在以二为开头的新闻前。
     * 如果新闻标题第一个字的拼音完全相同，则按照后续单词进行排序。如 新闻1为 第一次...  新闻2为 第二次...，
     * 则新闻2应该排在新闻1之前。
     * 示例：
     * <p>
     * newsList：我是谁；谁是我；我是我
     * <p>
     * return：谁是我；我是谁；我是我；
     *
     * @param newsList
     */
    public ArrayList<String> newsSort(ArrayList<String> newsList) {

        newsList.sort((o1, o2) -> {
            // 设置汉语拼音格式
            HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
            format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
            format.setVCharType(HanyuPinyinVCharType.WITH_V);
            format.setCaseType(HanyuPinyinCaseType.UPPERCASE);
            int len = Math.min(o1.length(), o2.length());
            for (int i = 0; i < len; i++) {
                try {
                    // 多音字取最后一个
                    String[] pyList1 = PinyinHelper.toHanyuPinyinStringArray(o1.charAt(i), format);
                    String[] pyList2 = PinyinHelper.toHanyuPinyinStringArray(o2.charAt(i), format);
                    String py1 = pyList1[pyList1.length - 1];
                    String py2 = pyList2[pyList2.length - 1];
                    int res = py1.compareTo(py2);
                    if (res != 0) {
                        return res;
                    }
                } catch (BadHanyuPinyinOutputFormatCombination badHanyuPinyinOutputFormatCombination) {
                    return o1.length() - o2.length();
                }
            }

            return o1.length() - o2.length();
        });

        return newsList;

    }

    /**
     * 热词搜索
     * <p>
     * 根据给定的新闻内容，找到文中出现频次最高的一个词语，词语长度最少为2（即4个字节），最多为10（即20个字节），且词语中不包含标点符号，可以出现英文，同频词语选择在文中更早出现的词语。
     * <p>
     * 示例：
     * <p>
     * String: 今天的中国，呈现给世界的不仅有波澜壮阔的改革发展图景，更有一以贯之的平安祥和稳定。这平安祥和稳定的背后，凝聚着中国治国理政的卓越智慧，也凝结着中国公安民警的辛勤奉献。
     * <p>
     * return：中国
     *
     * @param newsContent
     */
    public String findHotWords(String newsContent) {
        // 获取所有可能的热词及其频次
        List<String> sentences = splitText(newsContent.trim(), false);
        List<String> allHotwords = getAllHotwords(sentences);
        Map<String, Integer> frequencyOfWordsMap = getFrequencyOfWords(allHotwords);

        // 出现频次最高的词语
        Set<String> wordsHasTopTimes = getWordsHasTopTimes(frequencyOfWordsMap);
        if (wordsHasTopTimes.size() == 1) {
            return wordsHasTopTimes.iterator().next();
        } else {
            // 同频词语选择在文中更早出现的词语
            Set<String> topTimesEarliestWords = getEarliestWord(newsContent, wordsHasTopTimes);
            return pickLongestEarliestTopWord(topTimesEarliestWords);
        }
    }

    /**
     * @param sentences 句子列表
     * @return 所有可能的热词
     */
    private List<String> getAllHotwords(List<String> sentences) {
        List<String> result = new ArrayList<>();
        for (String sentence : sentences) {
            result.addAll(getHotwordOfCurSentence(sentence));
        }
        return result;
    }

    /**
     * @param sentence 一个句子
     * @return 该句子中所有可能的热词
     */
    private List<String> getHotwordOfCurSentence(String sentence) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < sentence.length(); i++) {
            for (int j = i + 1; j < sentence.length(); j++) {
                String curWord = sentence.substring(i, j + 1);
                if (checkWordLen(curWord)) {
                    result.add(curWord);
                }
            }
        }
        return result;
    }

    /**
     * @param toCheck 要检查的文本
     * @return 文本是否满足：长度最少为2（即4个字节），最多为10（即20个字节）
     */
    private boolean checkWordLen(String toCheck) {
        int len = byteLength(toCheck);
        return len >= 4 && len <= 20;
    }

    /**
     * @param hotwords 所有可能的热词
     * @return 热词及其出现的频率对应 map
     */
    private Map<String, Integer> getFrequencyOfWords(List<String> hotwords) {
        Map<String, Integer> tokenTimesMap = new HashMap<>();
        for (String curWord : hotwords) {
            if (checkWordLen(curWord)) {
                if (tokenTimesMap.containsKey(curWord)) {
                    int curTimeOfToken = tokenTimesMap.get(curWord);
                    curTimeOfToken++;
                    tokenTimesMap.put(curWord, curTimeOfToken);
                } else {
                    tokenTimesMap.put(curWord, 1);
                }
            }
        }
        return tokenTimesMap;
    }

    /**
     * @param frequencyOfWordsMap 包含词语及其频次的列表
     * @return 频次最高的词语（可能大于一）
     */
    private Set<String> getWordsHasTopTimes(Map<String, Integer> frequencyOfWordsMap) {
        // 排序
        List<Map.Entry<String, Integer>> frequencyOfWordsMapEntryList = new ArrayList<>(frequencyOfWordsMap.entrySet());
        frequencyOfWordsMapEntryList.sort(new ValueComparator());

        int curTopTimes = frequencyOfWordsMapEntryList.get(0).getValue();
        return getCompoentsOfExpectedValue(frequencyOfWordsMapEntryList, curTopTimes);
    }

    /**
     * @param newsContent      文本内容
     * @param wordsHasTopTimes 频率出现最高的词语
     * @return 频率出现最高且在文本中出现最早的词语
     */
    private Set<String> getEarliestWord(String newsContent, Set<String> wordsHasTopTimes) {
        // 统计热词在文中第一次出现的位置
        Map<String, Integer> wordsWithIndexMap = new HashMap<>();
        for (String curWord : wordsHasTopTimes) {
            int firstIndex = newsContent.indexOf(curWord);
            wordsWithIndexMap.put(curWord, firstIndex);
        }

        // 按照出现时间的降序排序
        List<Map.Entry<String, Integer>> wordsWithIndexMapEntryList = new ArrayList<>(wordsWithIndexMap.entrySet());
        wordsWithIndexMapEntryList.sort(new ValueComparator());

        // 排序后的最后一个即是最早出现的
        int curEarliestIndex = wordsWithIndexMapEntryList.get(wordsWithIndexMapEntryList.size() - 1).getValue();
        return getCompoentsOfExpectedValue(wordsWithIndexMapEntryList, curEarliestIndex);
    }


    /**
     * @param list          包含 map.entry 的列表
     * @param expectedValue 期望的 entry 的值
     * @return entry 值为 expectedValue 的 entry key值的 set
     */
    private Set<String> getCompoentsOfExpectedValue(List<Map.Entry<String, Integer>> list, int expectedValue) {
        Set<String> result = new HashSet<>();

        for (Map.Entry<String, Integer> curToken : list) {
            if (curToken.getValue() == expectedValue) {
                result.add(curToken.getKey());
            }
        }
        return result;
    }

    /**
     * @param words 所有的备选热词
     * @return 在频次最高、出现最早的基础上，最长的热词
     */
    private String pickLongestEarliestTopWord(Set<String> words) {
        String result = "";
        for (String word : words) {
            if (word.length() > result.length()) {
                result = word;
            }
        }
        return result;
    }

    /**
     * 相似度对比
     * <p>
     * 为了检测新闻标题之间的相似度，公司需要一个评估字符串相似度的算法。
     * 即一个新闻标题A修改到新闻标题B需要几步操作，我们将最少需要的次数定义为 最少操作数
     * 操作包括三种： 替换：一个字符替换成为另一个字符，
     * 插入：在某位置插入一个字符，
     * 删除：删除某位置的字符
     * 示例：
     * 中国队是冠军  -> 我们是冠军
     * 最少需要三步来完成。第一步删除第一个字符  "中"
     * 第二步替换第二个字符  "国"->"我"
     * 第三步替换第三个字符  "队"->"们"
     * 因此 最少的操作步数就是 3
     * <p>
     * 定义相似度= 1 - 最少操作次数/较长字符串的长度
     * 如在上例中：相似度为  (1 - 3/6) * 100= 50.00（结果保留2位小数，四舍五入，范围在0.00-100.00之间）
     *
     * @param title1
     * @param title2
     */
    public double minDistance(String title1, String title2) {
        int editDistance = getEditDistance(title1, title2);
        double similarity = (1 - (double) editDistance / Math.max(title1.length(), title2.length())) * 100;
        DecimalFormat df = new DecimalFormat("#.##");
        return Double.valueOf(df.format(similarity));
    }

    // 计算两字符串的编辑距离
    private int getEditDistance(String str1, String str2) {
        int[] dp = new int[str2.length() + 1];
        for (int i = 0; i <= str1.length(); i++) {
            int last = i;
            for (int j = 0; j <= str2.length(); j++) {
                if (i == 0) {
                    dp[j] = j;
                } else if (j > 0) {
                    int distance = dp[j - 1];
                    if (str1.charAt(i - 1) != str2.charAt(j - 1))
                        distance = Math.min(Math.min(distance, last),
                                dp[j]) + 1;
                    dp[j - 1] = last;
                    last = distance;
                }
            }
            if (i > 0)
                dp[str2.length()] = last;
        }
        return dp[str2.length()];
    }

    /**
     * 按照降序排列
     */
    private class ValueComparator implements Comparator<Map.Entry<String, Integer>> {
        @Override
        public int compare(Map.Entry<String, Integer> mp1, Map.Entry<String, Integer> mp2) {
            return mp2.getValue() - mp1.getValue();
        }
    }
}
