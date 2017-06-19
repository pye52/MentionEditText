package com.kanade.mentionedittext;

import android.text.Editable;

public interface MentionTextChangedListener {
    void beforeTextChanged(CharSequence s, int start, int count, int after);

    void onTextChanged(CharSequence s, int start, int before, int count);

    void afterTextChanged(Editable s);

    void onTextPaste(Editable edit, CharSequence c, int start, int end);
}
