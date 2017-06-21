package com.kanade.mentionedittext;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
//import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * 类似微信、QQ等的@人功能
 * 部分代码参考自https://github.com/luckyandyzhang/MentionEditText，部分注释已保留
 * @author 团子吃蛋挞
 * Created by kanade on 2016/9/1.
 */
public class MentionEditText extends AppCompatEditText {
    protected Runnable mAction;

    protected int mMentionTextColor;

    protected boolean mIsSelected;
    protected Range mLastSelectedRange;
    protected ArrayList<Range> mRangeArrayList;
    protected MentionTextChangedListener listener;

    public MentionEditText(Context context) {
        super(context);
    }

    public MentionEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public MentionEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.MentionEditText);
        mRangeArrayList = new ArrayList<>();
        mMentionTextColor = typedArray.getColor(R.styleable.MentionEditText_mentionColor, Color.RED);
        typedArray.recycle();
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                mentionTextChanged(start, count, after);
                if (listener != null) {
                    listener.beforeTextChanged(s, start, count, after);
                }
//                Log.d("MentionEdit", "beforeTextChanged");
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (listener != null) {
                    listener.onTextChanged(s, start, before, count);
                }
//                Log.d("MentionEdit", "onTextChanged");
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (listener != null) {
                    listener.afterTextChanged(s);
                }
//                Log.d("MentionEdit", "afterTextChanged");
            }
        });
    }

    public void setListener(MentionTextChangedListener listener) {
        this.listener = listener;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new HackInputConnection(super.onCreateInputConnection(outAttrs), true, this);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        if (mAction == null) {
            mAction = new Runnable() {
                @Override
                public void run() {
                    setSelection(getText().length());
                }
            };
        }
        post(mAction);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        // avoid infinite recursion after calling setSelection()
        if (mLastSelectedRange != null && mLastSelectedRange.isEqual(selStart, selEnd)) {
            return;
        }

        // if user cancel a selection of mention string, reset the state of 'mIsSelected'
        Range closestRange = getRangeOfClosestMentionString(selStart, selEnd);
        if (closestRange != null && closestRange.to == selEnd) {
            mIsSelected = false;
        }

        Range nearbyRange = getRangeOfNearbyMentionString(selStart, selEnd);
        // if there is no mention string nearby the cursor, just skip
        if (nearbyRange == null) {
            return;
        }

        // forbid cursor located in the mention string.
        if (selStart == selEnd) {
            setSelection(nearbyRange.getAnchorPosition(selStart));
        } else {
            if (selEnd < nearbyRange.to) {
                setSelection(selStart, nearbyRange.to);
            }
            if (selStart > nearbyRange.from) {
                setSelection(nearbyRange.from, selEnd);
            }
        }
    }

    public void addMentionText(int uid, String name) {
        addMentionText(uid, name, true);
    }

    /**
     * 插入mention string，并且高亮
     * @param uid id
     * @param name 需要显示的名字
     * @param charBefore 是否需要和前面一个字符(通常为'@')结合
     */
    public void addMentionText(int uid, String name, boolean charBefore) {
        Editable editable = getText();
        int start = getSelectionStart();
        int end = start + name.length();
        editable.insert(start, name + " ");
        if (charBefore) {
            start--;
        }
        editable.setSpan(new ForegroundColorSpan(mMentionTextColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mRangeArrayList.add(new Range(uid, name, start, end));
        setSelection(end + 1);
    }

    public String printMentionString(String format) {
        return printMentionString(format, true);
    }

    /**
     * 将所有mention string以指定格式输出
     * @param format 格式化字符串，注意要包含id和name的位置，示例："[mention: %s, %s]"
     * @param isClear 输出完毕是否需要清理edittext
     * @return 以指定格式输出的字符串
     */
    public String printMentionString(String format, boolean isClear) {
        String text = getText().toString();
        if (mRangeArrayList.isEmpty()) {
            return text;
        }

        StringBuilder builder = new StringBuilder("");
        int lastRangeTo = 0;
        // 根据rang的起始位置正序排列
        Collections.sort(mRangeArrayList);

        for (Range range : mRangeArrayList) {
            String newChar = String.format(format, range.id, range.name);
            builder.append(text.substring(lastRangeTo, range.from));
            builder.append(newChar);
            lastRangeTo = range.to;
        }
        builder.append(text.substring(lastRangeTo, text.length()));

        if (isClear) {
            clear();
        }
        return builder.toString();
    }

    /**
     * 输入框内容发生变化时，需要重新构建range列表
     * @param start 变化开始位置
     * @param count 产生变化的字符个数
     * @param after 变化后的字符个数
     */
    protected void mentionTextChanged(int start, int count, int after) {
        Editable editable = getText();
        // 在末尾增加就不需要处理了
        if (start >= editable.length()) {
            return;
        }

        int end = start + count;
        int offset = after - count;

        // 清理start 到 start + count之间的span
        // 注意如果range.from = 0，也会被getSpans(0,0,ForegroundColorSpan.class)获取到
        if (start != end && !mRangeArrayList.isEmpty()) {
            ForegroundColorSpan[] spans = editable.getSpans(start, end, ForegroundColorSpan.class);
            for (ForegroundColorSpan span : spans) {
                editable.removeSpan(span);
            }
        }

        // 清理arraylist中上面已经清理掉的range
        // 将end之后的span往后挪offset个位置
        Iterator<Range> iterator = mRangeArrayList.iterator();
        while (iterator.hasNext()) {
            Range range = iterator.next();
            if (range.isWrapped(start, end)) {
                iterator.remove();
                continue;
            }

            if (range.from >= end) {
                range.setOffset(offset);
            }
        }
    }

    public void clear() {
        mRangeArrayList.clear();
        setText("");
    }

    private Range getRangeOfClosestMentionString(int selStart, int selEnd) {
        if (mRangeArrayList == null) {
            return null;
        }
        for (Range range : mRangeArrayList) {
            if (range.contains(selStart, selEnd)) {
                return range;
            }
        }
        return null;
    }

    private Range getRangeOfNearbyMentionString(int selStart, int selEnd) {
        if (mRangeArrayList == null) {
            return null;
        }
        for (Range range : mRangeArrayList) {
            if (range.isWrappedBy(selStart, selEnd)) {
                return range;
            }
        }
        return null;
    }

    // handle the deletion action for mention string, such as '@test'
    private class HackInputConnection extends InputConnectionWrapper {
        private EditText editText;

        private HackInputConnection(InputConnection target, boolean mutable, MentionEditText editText) {
            super(target, mutable);
            this.editText = editText;
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                int selectionStart = editText.getSelectionStart();
                int selectionEnd = editText.getSelectionEnd();
                Range closestRange = getRangeOfClosestMentionString(selectionStart, selectionEnd);
                if (closestRange == null) {
                    mIsSelected = false;
                    return super.sendKeyEvent(event);
                }
                // if mention string has been selected or the cursor is at the beginning of mention string, just use default action(delete)
                if (mIsSelected || selectionStart == closestRange.from) {
                    mIsSelected = false;
                    return super.sendKeyEvent(event);
                } else {
                    // select the mention string
                    mIsSelected = true;
                    mLastSelectedRange = closestRange;
                    setSelection(closestRange.to, closestRange.from);
                }
                return true;
            }
            return super.sendKeyEvent(event);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            if (beforeLength == 1 && afterLength == 0) {
                return sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                        && sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
            }
            return super.deleteSurroundingText(beforeLength, afterLength);
        }
    }
}