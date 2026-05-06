package com.desensitizer.core.detector;

import com.desensitizer.core.api.SensitiveDetector;
import com.desensitizer.core.api.SensitiveMatch;
import com.desensitizer.core.api.SensitiveType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexDetector implements SensitiveDetector {

    private final String name;
    private final SensitiveType type;
    private final Pattern pattern;

    public RegexDetector(String name, SensitiveType type, String regex) {
        this.name = name;
        this.type = type;
        this.pattern = Pattern.compile(regex);
    }

    @Override
    public List<SensitiveMatch> detect(String text) {
        List<SensitiveMatch> matches = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return matches;
        }

        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String matchedText;
            int start;
            int end;
            
            if (matcher.groupCount() > 0 && matcher.group(1) != null) {
                matchedText = matcher.group(1);
                start = matcher.start(1);
                end = matcher.end(1);
            } else {
                matchedText = matcher.group(0);
                start = matcher.start();
                end = matcher.end();
            }
            
            SensitiveMatch match = new SensitiveMatch(
                    start,
                    end,
                    matchedText,
                    type.name(),
                    1.0f
            );
            matches.add(match);
        }
        return matches;
    }

    @Override
    public String name() {
        return name;
    }
}
