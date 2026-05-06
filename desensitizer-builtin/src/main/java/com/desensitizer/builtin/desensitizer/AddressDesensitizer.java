package com.desensitizer.builtin.desensitizer;

import com.desensitizer.core.api.Desensitizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddressDesensitizer implements Desensitizer {

    @Override
    public String desensitize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // 检查是否包含日语特定字符（假名）
        boolean hasJapanese = containsJapanese(value);
        
        // 检查是否包含中文字符
        boolean hasChinese = containsChinese(value);

        // 日语地址处理（优先检查，因为日语也包含汉字）
        if (hasJapanese) {
            return desensitizeJapaneseAddress(value);
        }

        // 中文地址处理
        if (hasChinese) {
            return desensitizeChineseAddress(value);
        }

        // 外国地址处理
        return desensitizeForeignAddress(value);
    }

    /**
     * 中文地址脱敏：保留省/市/区信息，脱敏详细地址
     */
    private String desensitizeChineseAddress(String value) {
        // 优先匹配行政区域关键词
        if (value.contains("区") || value.contains("县")) {
            int index = Math.max(value.lastIndexOf("区"), value.lastIndexOf("县"));
            if (index > 0) {
                String provinceCity = value.substring(0, index + 1);
                return provinceCity + "***";
            }
        }

        if (value.contains("市")) {
            int index = value.indexOf("市");
            if (index > 0) {
                String provinceCity = value.substring(0, index + 1);
                return provinceCity + "***";
            }
        }

        if (value.contains("省")) {
            int index = value.indexOf("省");
            if (index > 0) {
                String province = value.substring(0, index + 1);
                return province + "***";
            }
        }

        // 默认处理：保留前半部分，脱敏后半部分
        int halfLength = value.length() / 2;
        if (halfLength >= 2) {
            return value.substring(0, halfLength) + "***";
        } else if (value.length() > 4) {
            return value.substring(0, 4) + "***";
        }

        return value;
    }

    /**
     * 外国地址脱敏：根据不同国家/地区的地址格式进行脱敏
     */
    private String desensitizeForeignAddress(String value) {
        // 美国地址格式：123 Street Name, City, State ZIP
        Pattern usPattern = Pattern.compile("^(.+),\\s*([A-Za-z\\s]+),\\s*([A-Z]{2})\\s*(\\d{5})(-.+)?$");
        Matcher usMatcher = usPattern.matcher(value);
        if (usMatcher.matches()) {
            String city = usMatcher.group(2).trim();
            String state = usMatcher.group(3);
            String zip = usMatcher.group(4);
            return city + ", " + state + " " + zip;
        }

        // 英国地址格式：... City, Postcode
        Pattern ukPattern = Pattern.compile("^(.+),\\s*([A-Za-z\\s]+),\\s*([A-Z]{1,2}\\d[A-Z\\d]?\\s*\\d[A-Z]{2})$");
        Matcher ukMatcher = ukPattern.matcher(value);
        if (ukMatcher.matches()) {
            String city = ukMatcher.group(2).trim();
            String postcode = ukMatcher.group(3);
            return city + ", " + postcode;
        }

        // 法国地址格式：..., PostalCode City
        Pattern frPattern = Pattern.compile("^(.+),\\s*(\\d{5})\\s+([A-Za-z\\s-]+)$");
        Matcher frMatcher = frPattern.matcher(value);
        if (frMatcher.matches()) {
            String postcode = frMatcher.group(2);
            String city = frMatcher.group(3).trim();
            return postcode + " " + city;
        }

        // 韩国地址格式1：광역시/特别自治市 구 ...
        Pattern krPattern = Pattern.compile("^([가-힣]+광역시|[가-힣]+특별[자치]?시)\\s+([가-힣]+구)\\s*.+$");
        Matcher krMatcher = krPattern.matcher(value);
        if (krMatcher.matches()) {
            String city = krMatcher.group(1);
            String gu = krMatcher.group(2);
            return city + " " + gu + "***";
        }

        // 韩国地址格式2：특별자치도 시 구 ...
        Pattern krPattern2 = Pattern.compile("^([가-힣]+특별자치도)\\s+([가-힣]+시)\\s+([가-힣]+구)\\s*.+$");
        Matcher krMatcher2 = krPattern2.matcher(value);
        if (krMatcher2.matches()) {
            String doRegion = krMatcher2.group(1);
            String si = krMatcher2.group(2);
            String gu = krMatcher2.group(3);
            return doRegion + " " + si + " " + gu + "***";
        }

        // 韩国地址格式3：도 시/군 ...
        Pattern krPattern3 = Pattern.compile("^([가-힣]+도)\\s+([가-힣]+[시군])\\s*.+$");
        Matcher krMatcher3 = krPattern3.matcher(value);
        if (krMatcher3.matches()) {
            String doRegion = krMatcher3.group(1);
            String sigun = krMatcher3.group(2);
            return doRegion + " " + sigun + "***";
        }

        // 德国地址格式：Street, PostalCode City
        Pattern dePattern = Pattern.compile("^(.+),\\s*(\\d{5})\\s+([A-Za-z\\s-üöäßÜÖÄß]+)$");
        Matcher deMatcher = dePattern.matcher(value);
        if (deMatcher.matches()) {
            String postcode = deMatcher.group(2);
            String city = deMatcher.group(3).trim();
            return postcode + " " + city;
        }

        // 通用外国地址处理：保留最后两个逗号分隔的部分（通常是城市和邮编/州）
        if (value.contains(",")) {
            String[] parts = value.split(",");
            if (parts.length >= 2) {
                String lastPart = parts[parts.length - 1].trim();
                String secondLastPart = parts[parts.length - 2].trim();
                // 如果最后部分看起来像邮编，保留城市+邮编
                if (lastPart.matches("\\d{4,6}") || lastPart.matches("[A-Za-z0-9\\s]{5,10}")) {
                    return secondLastPart + ", " + lastPart;
                }
                // 否则保留最后两部分
                return secondLastPart + ", " + lastPart;
            }
        }

        // 默认处理：保留前半部分，脱敏后半部分
        int halfLength = value.length() / 2;
        if (halfLength >= 4) {
            return value.substring(0, halfLength).trim() + "***";
        }

        return value;
    }

    /**
     * 日语地址脱敏：保留都道府県+市区町村，脱敏详细地址
     */
    private String desensitizeJapaneseAddress(String value) {
        // 日本地址格式1：県市町... 匹配到町级别
        Pattern jpPattern = Pattern.compile("^([\\u3040-\\u30FF\\u4E00-\\u9FA5]+?県)([\\u3040-\\u30FF\\u4E00-\\u9FA5]+?市)([\\u3040-\\u30FF\\u4E00-\\u9FA5]+?町).*$");
        Matcher jpMatcher = jpPattern.matcher(value);
        if (jpMatcher.matches()) {
            String pref = jpMatcher.group(1);
            String city = jpMatcher.group(2);
            String town = jpMatcher.group(3);
            return pref + city + town + "***";
        }
        
        // 日本地址格式2：県区町... 匹配到町级别（东京等特别区）
        Pattern jpPattern2 = Pattern.compile("^([\\u3040-\\u30FF\\u4E00-\\u9FA5]+?県)([\\u3040-\\u30FF\\u4E00-\\u9FA5]+?区)([\\u3040-\\u30FF\\u4E00-\\u9FA5]+?町).*$");
        Matcher jpMatcher2 = jpPattern2.matcher(value);
        if (jpMatcher2.matches()) {
            String pref = jpMatcher2.group(1);
            String ku = jpMatcher2.group(2);
            String town = jpMatcher2.group(3);
            return pref + ku + town + "***";
        }
        
        // 日本地址格式3：県市... 匹配到市级别
        Pattern jpPattern3 = Pattern.compile("^([\\u3040-\\u30FF\\u4E00-\\u9FA5]+?県)([\\u3040-\\u30FF\\u4E00-\\u9FA5]+?市).*$");
        Matcher jpMatcher3 = jpPattern3.matcher(value);
        if (jpMatcher3.matches()) {
            String pref = jpMatcher3.group(1);
            String city = jpMatcher3.group(2);
            return pref + city + "***";
        }
        
        // 日本地址格式4：府都道市... 匹配到市级别
        Pattern jpPattern4 = Pattern.compile("^([\\u3040-\\u30FF\\u4E00-\\u9FA5]+?[府都道])([\\u3040-\\u30FF\\u4E00-\\u9FA5]+[市区町村]).*$");
        Matcher jpMatcher4 = jpPattern4.matcher(value);
        if (jpMatcher4.matches()) {
            String pref = jpMatcher4.group(1);
            String city = jpMatcher4.group(2);
            return pref + city + "***";
        }
        
        // 只匹配都道府県的情况
        Pattern jpPattern5 = Pattern.compile("^([\\u3040-\\u30FF\\u4E00-\\u9FA5]+[県府都道]).*$");
        Matcher jpMatcher5 = jpPattern5.matcher(value);
        if (jpMatcher5.matches()) {
            String pref = jpMatcher5.group(1);
            return pref + "***";
        }

        return value;
    }

    private boolean containsChinese(String value) {
        for (char c : value.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fa5') {
                return true;
            }
        }
        return false;
    }

    private boolean containsJapanese(String value) {
        // 检查是否包含日语假名（平假名或片假名）
        for (char c : value.toCharArray()) {
            // 平假名: \u3040-\u309F
            // 片假名: \u30A0-\u30FF
            if ((c >= '\u3040' && c <= '\u309F') || (c >= '\u30A0' && c <= '\u30FF')) {
                return true;
            }
        }
        return false;
    }
}