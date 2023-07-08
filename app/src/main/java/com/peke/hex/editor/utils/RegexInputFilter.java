package com.peke.hex.editor.utils;

import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexInputFilter implements InputFilter {

    private final Pattern mRegex;

    /**
     * @param regex 正则表达式
     */
    public RegexInputFilter(@NonNull String regex) {
        mRegex = Pattern.compile(regex);
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dStart, int dEnd) {
        if (TextUtils.isEmpty(source))
            return null;

        if (source.length() == 1){
            Matcher matcher = mRegex.matcher(source);
            return matcher.find() ? null : "";
        }
        else {
            StringBuilder builder = new StringBuilder();
            Matcher matcher = mRegex.matcher(source);
            while (matcher.find()){
                builder.append(matcher.group());
            }
            return builder.toString();
        }

    }

}
